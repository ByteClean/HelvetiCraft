"""Event handlers and background tasks for the Discord bot."""
import discord
from discord.ext import tasks
from Discord_Bot.config import (
    GUILD_ID,
    RULES_CHANNEL_ID,
    RULES_MESSAGE_ID,
    GUEST_ROLE,
    PLAYER_ROLE,
    VERIFY_EMOJI,
    WELCOME_CHANNEL_ID,
)
from Discord_Bot.stats import setup_stats_channels, update_stats_channels
from Discord_Bot.config import COMMANDS_CHANNEL_NAME, INITIATIVES_CHANNEL_NAME
from Discord_Bot.commands import COMMANDS as TOP_LEVEL_COMMANDS


async def on_ready(bot: discord.Client):
    # Diagnostics added to help debug missing slash commands
    try:
        print(f"Bot is online as {bot.user} (id={getattr(bot.user, 'id', None)})")
        print("DIAG: bot.application_id =", getattr(bot, "application_id", None))
        print("DIAG: configured GUILD_ID =", GUILD_ID)
        guild = bot.get_guild(GUILD_ID)
        print("DIAG: bot.get_guild(GUILD_ID) ->", guild)
    except Exception as e:
        print(f"DIAG: error printing basic diagnostics: {e}")

    await setup_stats_channels(guild)

    # Add reaction to rules message if missing
    channel = guild.get_channel(RULES_CHANNEL_ID)
    try:
        message = await channel.fetch_message(RULES_MESSAGE_ID)
        if not any(reaction.emoji == VERIFY_EMOJI for reaction in message.reactions):
            await message.add_reaction(VERIFY_EMOJI)
            print("Reaction added to rules message")
    except Exception as e:
        print(f"Could not add reaction: {e}")

    # Ensure command and initiatives text channels exist (prefer IDs if configured)
    try:
        from Discord_Bot.config import COMMANDS_CHANNEL_ID, INITIATIVES_CHANNEL_ID
        if guild:
            # Commands channel
            cmd_chan = None
            if COMMANDS_CHANNEL_ID:
                cmd_chan = guild.get_channel(COMMANDS_CHANNEL_ID)
                print(f"DEBUG: Looking up commands channel by id={COMMANDS_CHANNEL_ID} -> {cmd_chan}")
            if not cmd_chan:
                cmd_chan = discord.utils.get(guild.text_channels, name=COMMANDS_CHANNEL_NAME)
                print(f"DEBUG: Looking up commands channel by name='{COMMANDS_CHANNEL_NAME}' -> {cmd_chan}")
            if not cmd_chan:
                cmd_chan = await guild.create_text_channel(COMMANDS_CHANNEL_NAME)
                print(f"Created commands channel: {cmd_chan}")

            # Initiatives channel
            init_chan = None
            if INITIATIVES_CHANNEL_ID:
                init_chan = guild.get_channel(INITIATIVES_CHANNEL_ID)
                print(f"DEBUG: Looking up initiatives channel by id={INITIATIVES_CHANNEL_ID} -> {init_chan}")
            if not init_chan:
                init_chan = discord.utils.get(guild.text_channels, name=INITIATIVES_CHANNEL_NAME)
                print(f"DEBUG: Looking up initiatives channel by name='{INITIATIVES_CHANNEL_NAME}' -> {init_chan}")
            if not init_chan:
                init_chan = await guild.create_text_channel(INITIATIVES_CHANNEL_NAME)
                print(f"Created initiatives channel: {init_chan}")
    except Exception as e:
        print(f"Could not create/find text channels: {e}")

    update_stats_loop.start(bot)

    # Register top-level commands to the tree at runtime (ensures they're present)
    try:
        for cmd in TOP_LEVEL_COMMANDS:
            try:
                bot.tree.add_command(cmd)
            except Exception:
                pass
    except Exception as e:
        print(f"Could not add commands: {e}")

    # Try to sync commands to the specific guild so they appear immediately
    try:
        if guild:
            await bot.tree.sync(guild=discord.Object(id=GUILD_ID))
            print("Commands synced to guild")
            # Debug print registered commands
            print("Registered app commands:")
            for cmd in bot.tree.walk_commands():
                parent = getattr(cmd, "parent", None)
                print(f" - {cmd.name} (group={parent.name if parent else 'none'}) -> {cmd.description}")
    except Exception as e:
        print(f"Could not sync commands to guild: {e}")


async def on_member_join(member: discord.Member):
    # Assign Gast role
    role = discord.utils.get(member.guild.roles, name=GUEST_ROLE)
    if role:
        await member.add_roles(role)
    print(f"Gast role assigned to {member.name}")

    await update_stats_channels(member.guild)

    # Send welcome message
    channel = member.guild.get_channel(WELCOME_CHANNEL_ID)
    if channel:
        await channel.send(
            f"Willkommen {member.mention}! "
            f"Bitte lies dir zuerst die Regeln im <#{RULES_CHANNEL_ID}> durch "
            f"und bestätige sie mit {VERIFY_EMOJI}, um freigeschaltet zu werden."
        )


async def on_member_remove(member: discord.Member):
    await update_stats_channels(member.guild)


async def on_raw_reaction_add(payload: discord.RawReactionActionEvent, bot: discord.Client):
    """Handle raw reaction adds for the rules message. ``bot`` is passed from the caller.
    This wrapper signature accepts the bot instance because discord's event handler only supplies
    the payload; a small wrapper in `bot.py` will call this function with the bot.
    """
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
    print(f"{member.name} verified and role updated.")

@tasks.loop(minutes=1)
async def update_stats_loop(bot: discord.Client):
    guild = bot.get_guild(GUILD_ID)
    if guild:
        await update_stats_channels(guild)
