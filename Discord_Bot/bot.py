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
from discord.ext import commands
# Ensure repository root is on sys.path so absolute package imports work
pkg_root = pathlib.Path(__file__).resolve().parent
repo_root = str(pkg_root.parent)
if repo_root not in sys.path:
    sys.path.insert(0, repo_root)

import Discord_Bot.config as config
import Discord_Bot.events as events


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

# Load text-based (!) commands cog so prefix commands like !initiative work
try:
    import Discord_Bot.text_commands as text_commands
    # If the module exposes async setup(bot), call it; otherwise add the Cog class
    if hasattr(text_commands, 'setup'):
        import asyncio
        asyncio.get_event_loop().run_until_complete(text_commands.setup(bot))
    elif hasattr(text_commands, 'TextCommands'):
        bot.add_cog(text_commands.TextCommands(bot))
    print("Text commands cog loaded")
except Exception as e:
    print(f"Could not load text commands cog: {e}")




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


if __name__ == "__main__":
    if not TOKEN:
        print("ERROR: DISCORD_TOKEN is not set. Please add it to your environment or .env file before running the bot.")
    else:
        bot.run(TOKEN)
