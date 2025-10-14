"""Entrypoint for the Discord bot.  Composes smaller modules.

This file intentionally stays small: it configures intents, creates the
`commands.Bot` instance, registers event handlers from `events.py`, and
starts the bot using the token from `config.py`.

This file can be run either as a package (`python -m Discord_Bot.bot`) or
directly as a script (`python Discord_Bot/bot.py`). When executed directly
we adjust sys.path and set __package__ so the package-relative imports work.
"""

import sys
import pathlib
import discord
import logging
from discord.ext import commands

# Ensure repository root is on sys.path so absolute package imports work
pkg_root = pathlib.Path(__file__).resolve().parent
repo_root = str(pkg_root.parent)
if repo_root not in sys.path:
    sys.path.insert(0, repo_root)

# === Dual Import Support ===
try:
    import Discord_Bot.config as config
    import Discord_Bot.events as events
    pkg_mode = True
except ModuleNotFoundError:
    import config
    import events
    pkg_mode = False

TOKEN = config.TOKEN

# === BOT SETUP ===
intents = discord.Intents.default()
intents.members = True
intents.guilds = True
intents.messages = True
intents.reactions = True
intents.message_content = True  # Requires enabling in Discord Dev Portal

bot = commands.Bot(command_prefix="!", intents=intents)

# Enable verbose logging
logging.basicConfig(level=logging.DEBUG)
logging.getLogger('discord').setLevel(logging.DEBUG)

# === Load Text Commands (prefix-based) ===
try:
    if pkg_mode:
        import Discord_Bot.text_commands as text_commands
    else:
        import text_commands

    if hasattr(text_commands, 'setup'):
        import asyncio
        asyncio.get_event_loop().run_until_complete(text_commands.setup(bot))
    elif hasattr(text_commands, 'TextCommands'):
        bot.add_cog(text_commands.TextCommands(bot))

    print("Text commands cog loaded.")
except Exception as e:
    print(f"Could not load text commands cog: {e}")

# === Load Slash Commands ===
try:
    if pkg_mode:
        import Discord_Bot.commands as app_commands_module
    else:
        import commands as app_commands_module

    import asyncio
    asyncio.get_event_loop().run_until_complete(app_commands_module.setup(bot))
    print("Application (slash) commands registered via commands.setup.")
except Exception as e:
    print(f"Could not register application commands: {e}")

# === EVENTS ===
@bot.event
async def on_ready():
    await events.on_ready(bot)

@bot.event
async def on_member_join(member):
    await events.on_member_join(member)

@bot.event
async def on_member_remove(member):
    await events.on_member_remove(member)

@bot.event
async def on_raw_reaction_add(payload):
    await events.on_raw_reaction_add(payload, bot)

# === Diagnostics ===
@bot.event
async def on_interaction(interaction: discord.Interaction):
    try:
        name = None
        if hasattr(interaction, 'data') and isinstance(interaction.data, dict):
            name = interaction.data.get('name')
    except Exception:
        name = None
    print(
        f"ON_INTERACTION: id={getattr(interaction,'id',None)} "
        f"type={getattr(interaction,'type',None)} name={name} "
        f"user_id={getattr(interaction.user,'id',None)} "
        f"channel_id={getattr(interaction.channel,'id',None)}"
    )

@bot.event
async def on_socket_response(msg):
    """Log raw socket messages for diagnostics."""
    try:
        t = msg.get('t')
        if t:
            print(
                f"SOCKET_EVENT: t={t} op={msg.get('op')} "
                f"d_keys={list(msg.get('d',{}).keys()) if isinstance(msg.get('d',None), dict) else type(msg.get('d'))}"
            )
        else:
            print(f"SOCKET_EVENT: op={msg.get('op')}")
    except Exception as e:
        print(f"on_socket_response error: {e}")

# === RUN ===
if __name__ == "__main__":
    if not TOKEN:
        print("ERROR: DISCORD_TOKEN is not set. Please add it to your environment or .env file before running the bot.")
    else:
        bot.run(TOKEN)
