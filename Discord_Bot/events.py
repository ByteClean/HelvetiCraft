"""Event handlers and background tasks for the Discord bot."""

import discord
import json
from discord.ext import tasks
import aiohttp

try:
    from Discord_Bot.config import (
        GUILD_ID,
        RULES_CHANNEL_ID,
        RULES_MESSAGE_ID,
        GUEST_ROLE,
        PLAYER_ROLE,
        VERIFY_EMOJI,
        WELCOME_CHANNEL_ID,
        COMMANDS_CHANNEL_NAME,
        INITIATIVES_CHANNEL_NAME,
        TOKEN,
    )
    from Discord_Bot.stats import setup_stats_channels, update_stats_channels
except ModuleNotFoundError:
    from config import (
        GUILD_ID,
        RULES_CHANNEL_ID,
        RULES_MESSAGE_ID,
        GUEST_ROLE,
        PLAYER_ROLE,
        VERIFY_EMOJI,
        WELCOME_CHANNEL_ID,
        COMMANDS_CHANNEL_NAME,
        INITIATIVES_CHANNEL_NAME,
        TOKEN,
    )
    from stats import setup_stats_channels, update_stats_channels


async def on_ready(bot: discord.Client):
    """Called when the bot successfully connects and is ready."""
    try:
        print(f"✅ Bot is online as {bot.user} (id={getattr(bot.user, 'id', None)})")
        print("DIAG: bot.application_id =", getattr(bot, "application_id", None))
        print("DIAG: configured GUILD_ID =", GUILD_ID)
        guild = bot.get_guild(GUILD_ID)
        print("DIAG: bot.get_guild(GUILD_ID) ->", guild)
    except Exception as e:
        print(f"DIAG: error printing basic diagnostics: {e}")

    # === Setup statistics ===
    await setup_stats_channels(guild)

    # === Add reaction to rules message if missing ===
    try:
        channel = guild.get_channel(RULES_CHANNEL_ID)
        message = await channel.fetch_message(RULES_MESSAGE_ID)
        if not any(reaction.emoji == VERIFY_EMOJI for reaction in message.reactions):
            await message.add_reaction(VERIFY_EMOJI)
            print("✅ Reaction added to rules message")
    except Exception as e:
        print(f"⚠️ Could not add reaction: {e}")

    # === Ensure text channels exist ===
    try:
        if guild:
            cmd_chan = discord.utils.get(guild.text_channels, name=COMMANDS_CHANNEL_NAME)
            if not cmd_chan:
                cmd_chan = await guild.create_text_channel(COMMANDS_CHANNEL_NAME)
                print(f"✅ Created commands channel: {cmd_chan}")

            init_chan = discord.utils.get(guild.text_channels, name=INITIATIVES_CHANNEL_NAME)
            if not init_chan:
                init_chan = await guild.create_text_channel(INITIATIVES_CHANNEL_NAME)
                print(f"✅ Created initiatives channel: {init_chan}")
    except Exception as e:
        print(f"⚠️ Could not create/find text channels: {e}")

    # === Start background tasks ===
    update_stats_loop.start(bot)

    # === Sync slash commands (official way) ===
    try:
        synced = await bot.tree.sync()
        print(f"✅ Synced {len(synced)} slash commands with Discord.")
    except Exception as e:
        print(f"⚠️ Failed to sync slash commands: {e}")


async def on_member_join(member: discord.Member):
    """Called when a new member joins the server."""
    role = discord.utils.get(member.guild.roles, name=GUEST_ROLE)
    if role:
        await member.add_roles(role)
    print(f"👤 Gast role assigned to {member.name}")

    await update_stats_channels(member.guild)

    channel = member.guild.get_channel(WELCOME_CHANNEL_ID)
    if channel:
        await channel.send(
            f"Willkommen {member.mention}! "
            f"Bitte lies dir zuerst die Regeln im <#{RULES_CHANNEL_ID}> durch "
            f"und bestätige sie mit {VERIFY_EMOJI}, um freigeschaltet zu werden."
        )


async def on_member_remove(member: discord.Member):
    """Called when a member leaves the server."""
    await update_stats_channels(member.guild)


async def on_raw_reaction_add(payload: discord.RawReactionActionEvent, bot: discord.Client):
    """Handles verification reaction on the rules message."""
    if payload.message_id != RULES_MESSAGE_ID:
        return
    if str(payload.emoji) != VERIFY_EMOJI:
        return

    guild = bot.get_guild(payload.guild_id)
    member = await guild.fetch_member(payload.user_id)
    if member.bot:
        return

    guest_role = discord.utils.get(guild.roles, name=GUEST_ROLE)
    player_role = discord.utils.get(guild.roles, name=PLAYER_ROLE)

    if guest_role in member.roles:
        await member.remove_roles(guest_role)
        await member.add_roles(player_role)
    print(f"✅ {member.name} verified and role updated.")


@tasks.loop(minutes=1)
async def update_stats_loop(bot: discord.Client):
    """Background task to periodically update server statistics."""
    guild = bot.get_guild(GUILD_ID)
    if guild:
        await update_stats_channels(guild)
