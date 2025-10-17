"""Entrypoint for the Discord bot. Composes smaller modules."""

import sys
import pathlib
import discord
import logging
from discord.ext import commands
import asyncio

# Windows fix for event loop
asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

# Ensure repository root is on sys.path
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
intents.message_content = True

bot = commands.Bot(command_prefix="!", intents=intents)

# Enable verbose logging
logging.basicConfig(level=logging.INFO)
logging.getLogger('discord').setLevel(logging.WARNING)


async def main():
    """Async entrypoint for the bot, handles proper cog setup."""

    # === Load Text Commands ===
    try:
        if pkg_mode:
            import Discord_Bot.text_commands as text_commands
        else:
            import text_commands

        if hasattr(text_commands, "setup"):
            await text_commands.setup(bot)
        elif hasattr(text_commands, "TextCommands"):
            await bot.add_cog(text_commands.TextCommands(bot))

        print("✅ Text commands cog loaded.")
    except Exception as e:
        print(f"⚠️ Could not load text commands cog: {e}")

    # === Load Slash Commands ===
    try:
        if pkg_mode:
            import Discord_Bot.commands as app_commands_module
        else:
            import commands as app_commands_module

        await app_commands_module.setup(bot)
        print("✅ Application (slash) commands registered via commands.setup.")
    except Exception as e:
        print(f"⚠️ Could not register application commands: {e}")

    # === Load Webhook Listener Cog ===
    try:
        if pkg_mode:
            import Discord_Bot.webhook_listener as webhook_listener
        else:
            import webhook_listener

        await bot.add_cog(webhook_listener.WebhookListener(bot))
        print("✅ Webhook listener cog added.")
    except Exception as e:
        print(f"⚠️ Could not start webhook listener: {e}")

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

    # Diagnostics
    @bot.event
    async def on_interaction(interaction: discord.Interaction):
        name = None
        try:
            name = interaction.data.get("name") if interaction.data else None
        except Exception:
            pass
        print(
            f"ON_INTERACTION: id={getattr(interaction,'id',None)} "
            f"type={getattr(interaction,'type',None)} name={name} "
            f"user_id={getattr(interaction.user,'id',None)} "
            f"channel_id={getattr(interaction.channel,'id',None)}"
        )

    # === RUN BOT ===
    try:
        await bot.start(TOKEN)
    except KeyboardInterrupt:
        print("Shutting down bot…")
        await bot.close()


if __name__ == "__main__":
    if not TOKEN:
        print("ERROR: DISCORD_TOKEN is not set.")
    else:
        asyncio.run(main())
