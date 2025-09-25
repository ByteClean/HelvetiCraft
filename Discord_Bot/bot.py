"""Entrypoint for the Discord bot.  Composes smaller modules.

This file intentionally stays small: it configures intents, creates the
`commands.Bot` instance, registers event handlers from `events.py`, and
starts the bot using the token from `config.py`.

This file can be run either as a package (`python -m Discord_Bot.bot`) or
directly as a script (`python Discord_Bot\bot.py`). When executed directly
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

try:
    import Discord_Bot.config as config
    import Discord_Bot.events as events
    pkg_mode = True
except ModuleNotFoundError:
    # Running as a script inside the package folder (for example in Docker when
    # WORKDIR is set to the package root) may not have the package available.
    # Fall back to local imports so bot.py can run directly.
    import config
    import events
    config = config
    events = events
    pkg_mode = False


TOKEN = config.TOKEN

# === BOT SETUP ===
intents = discord.Intents.default()
intents.members = True
intents.guilds = True
intents.messages = True
intents.reactions = True
# Allow the bot to read message content for the interactive flows (wait_for message)
# NOTE: You must also enable "Message Content Intent" in the Discord Developer Portal for
# your bot application.
intents.message_content = True

bot = commands.Bot(command_prefix="!", intents=intents)

# Enable verbose logging to help diagnose gateway/interaction delivery problems.
# This sets the root logger to DEBUG so discord.py emits gateway events.
logging.basicConfig(level=logging.DEBUG)
logging.getLogger('discord').setLevel(logging.DEBUG)

# Load text-based (!) commands cog so prefix commands like !initiative work
try:
    if pkg_mode:
        import Discord_Bot.text_commands as text_commands
    else:
        import text_commands
    # If the module exposes async setup(bot), call it; otherwise add the Cog class
    if hasattr(text_commands, 'setup'):
        import asyncio
        asyncio.get_event_loop().run_until_complete(text_commands.setup(bot))
    elif hasattr(text_commands, 'TextCommands'):
        bot.add_cog(text_commands.TextCommands(bot))
    print("Text commands cog loaded")
except Exception as e:
    print(f"Could not load text commands cog: {e}")

# Load application (slash) commands via commands.setup(bot)
try:
    if pkg_mode:
        import Discord_Bot.commands as app_commands_module
    else:
        import commands as app_commands_module
    # If module exposes async setup(bot), call it so commands register on bot.tree
    if hasattr(app_commands_module, 'setup'):
        import asyncio
        asyncio.get_event_loop().run_until_complete(app_commands_module.setup(bot))
        print("Application commands registered via commands.setup")
except Exception as e:
    print(f"Could not register application commands: {e}")




@bot.event
async def on_ready():
    # Delegate to events module
    await events.on_ready(bot)


@bot.event
async def on_member_join(member):
    await events.on_member_join(member)


@bot.event
async def on_member_remove(member):
    await events.on_member_remove(member)


@bot.event
async def on_raw_reaction_add(payload):
    # events.on_raw_reaction_add needs the bot instance too
    await events.on_raw_reaction_add(payload, bot)


# Diagnostic listener: log incoming interaction events so we can confirm whether
# Discord is delivering interactions to this process.
@bot.event
async def on_interaction(interaction: discord.Interaction):
    try:
        name = None
        if hasattr(interaction, 'data') and isinstance(interaction.data, dict):
            name = interaction.data.get('name')
    except Exception:
        name = None
    print(
        f"ON_INTERACTION: id={getattr(interaction,'id',None)} type={getattr(interaction,'type',None)} name={name} user_id={getattr(interaction.user,'id',None)} channel_id={getattr(interaction.channel,'id',None)}"
    )


@bot.event
async def on_socket_response(msg):
    """Log raw socket messages for diagnostics. We're especially interested in
    INTERACTION_CREATE events which indicate Discord is sending interactions
    to this connection.
    """
    try:
        t = msg.get('t')
        if t:
            print(f"SOCKET_EVENT: t={t} op={msg.get('op')} d_keys={list(msg.get('d',{}).keys()) if isinstance(msg.get('d',None), dict) else type(msg.get('d'))}")
        else:
            # For non-dispatch opcodes, just print opcode
            print(f"SOCKET_EVENT: op={msg.get('op')}")
    except Exception as e:
        print(f"on_socket_response error: {e}")


if __name__ == "__main__":
    if not TOKEN:
        print("ERROR: DISCORD_TOKEN is not set. Please add it to your environment or .env file before running the bot.")
    else:
        bot.run(TOKEN)
