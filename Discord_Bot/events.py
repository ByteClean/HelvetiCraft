"""Event handlers and background tasks for the Discord bot."""
import discord
import aiohttp
import json
from discord.ext import tasks
try:
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
    from Discord_Bot.config import COMMANDS_CHANNEL_NAME, INITIATIVES_CHANNEL_NAME, TOKEN
except ModuleNotFoundError:
    from config import (
        GUILD_ID,
        RULES_CHANNEL_ID,
        RULES_MESSAGE_ID,
        GUEST_ROLE,
        PLAYER_ROLE,
        VERIFY_EMOJI,
        WELCOME_CHANNEL_ID,
    )
    from stats import setup_stats_channels, update_stats_channels
    from config import COMMANDS_CHANNEL_NAME, INITIATIVES_CHANNEL_NAME, TOKEN

_commands_registered = False


async def on_ready(bot: discord.Client):
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

    # Ensure command and initiatives text channels exist
    try:
        from Discord_Bot.config import COMMANDS_CHANNEL_ID, INITIATIVES_CHANNEL_ID
        if guild:
            cmd_chan = None
            if COMMANDS_CHANNEL_ID:
                cmd_chan = guild.get_channel(COMMANDS_CHANNEL_ID)
                print(f"DEBUG: Looking up commands channel by id={COMMANDS_CHANNEL_ID} -> {cmd_chan}")
            if not cmd_chan:
                cmd_chan = discord.utils.get(guild.text_channels, name=COMMANDS_CHANNEL_NAME)
            if not cmd_chan:
                cmd_chan = await guild.create_text_channel(COMMANDS_CHANNEL_NAME)
                print(f"Created commands channel: {cmd_chan}")

            init_chan = None
            if INITIATIVES_CHANNEL_ID:
                init_chan = guild.get_channel(INITIATIVES_CHANNEL_ID)
            if not init_chan:
                init_chan = discord.utils.get(guild.text_channels, name=INITIATIVES_CHANNEL_NAME)
            if not init_chan:
                init_chan = await guild.create_text_channel(INITIATIVES_CHANNEL_NAME)
                print(f"Created initiatives channel: {init_chan}")
    except Exception as e:
        print(f"Could not create/find text channels: {e}")

    update_stats_loop.start(bot)

    # Register slash commands (handled by commands.setup(bot))
    try:
        if guild:
            global _commands_registered
            if not _commands_registered:
                app_id = getattr(bot, "application_id", None) or getattr(bot.user, "id", None)
                if not app_id:
                    print("Could not determine application id for REST registration")
                else:
                    try:
                        payload = []
                        for cmd in bot.tree.walk_commands():
                            if getattr(cmd, "parent", None):
                                continue
                            desc = getattr(cmd, "description", "") or ""
                            payload.append({"name": cmd.name, "description": desc, "type": 1})

                        if payload and TOKEN:
                            API_BASE = "https://discord.com/api/v10"
                            headers = {"Authorization": f"Bot {TOKEN}", "Content-Type": "application/json"}
                            async with aiohttp.ClientSession(headers=headers) as sess:
                                put_url = f"{API_BASE}/applications/{app_id}/guilds/{GUILD_ID}/commands"
                                async with sess.put(put_url, json=payload) as pr:
                                    print("PUT status:", pr.status)
                                    try:
                                        data = await pr.json()
                                        print("PUT response count =", len(data) if isinstance(data, list) else "n/a")
                                    except Exception:
                                        print("PUT response read error")
                                async with sess.get(put_url) as gr:
                                    print("GET status:", gr.status)
                                    try:
                                        remote = await gr.json()
                                        print("GET response json count =", len(remote) if isinstance(remote, list) else "n/a")
                                    except Exception:
                                        print("GET response read error")

                        _commands_registered = True
                    except Exception as e:
                        print("Error during REST registration:", e)
            else:
                print("Commands already registered for this process; skipping REST sync")
    except Exception as e:
        print(f"Unexpected error during command registration: {e}")


async def on_member_join(member: discord.Member):
    role = discord.utils.get(member.guild.roles, name=GUEST_ROLE)
    if role:
        await member.add_roles(role)
    print(f"Gast role assigned to {member.name}")

    await update_stats_channels(member.guild)

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
