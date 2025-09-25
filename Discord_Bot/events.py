"""Event handlers and background tasks for the Discord bot."""
import discord
import aiohttp
import json
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
from Discord_Bot.config import COMMANDS_CHANNEL_NAME, INITIATIVES_CHANNEL_NAME, TOKEN

# Guard to ensure we only register/sync commands once per process
_commands_registered = False


async def on_ready(bot: discord.Client):
    # Diagnostics added to help debug missing slash commands
    try:
        print(f"Bot is online as {bot.user} (id={getattr(bot.user, 'id', None)})")
        print("DIAG: bot.application_id =", getattr(bot, "application_id", None))
        print("DIAG: configured GUILD_ID =", GUILD_ID)
        guild = bot.get_guild(GUILD_ID)
        print("DIAG: bot.get_guild(GUILD_ID) ->", guild)
        # Extra diagnostics: session id and gateway info to help trace where interactions go
        try:
            conn = getattr(bot, '_connection', None)
            # discord.py stores a session id in the connection object once identified.
            session_id = None
            try:
                # prefer public attribute if present
                session_id = getattr(conn, 'session_id', None)
            except Exception:
                session_id = None
            print('DIAG: gateway session_id =', session_id)

            # Print a short snapshot of the connection internals so we can see shard and ws details
            try:
                conn_snapshot = {
                    'state': getattr(conn, 'state', None),
                    'identified': getattr(conn, 'identified', None),
                    'reconnecting': getattr(conn, 'reconnecting', None),
                    'shards': getattr(conn, 'shards', None),
                    'latency': getattr(conn, 'latency', None),
                }
                # Try to include the websocket URL if the internal method exists (best-effort)
                if conn and getattr(conn, '_get_websocket_url', None):
                    try:
                        conn_snapshot['ws_url'] = conn._get_websocket_url()
                    except Exception as e:
                        conn_snapshot['ws_url'] = f'unavailable: {e}'
                print('DIAG: connection snapshot =', conn_snapshot)
            except Exception as e:
                print('DIAG: error getting connection snapshot:', e)
            # Heuristic: dump any attribute names that might contain session/shard/ws info
            try:
                if conn:
                    interesting = []
                    for name in dir(conn):
                        lname = name.lower()
                        if any(k in lname for k in ('session', 'sess', 'shard', 'ws', 'gateway', 'identify', 'id', 'connection')):
                            interesting.append(name)
                    print('DIAG: connection attributes (filtered) =', interesting)
                    # Try a few common private names as best-effort
                    for candidate in ('session_id', '_session_id', '_session', 'session', '_identified'):
                        try:
                            val = getattr(conn, candidate)
                            print(f"DIAG: conn.{candidate} =", val)
                        except Exception:
                            pass
                # Also try to inspect bot-level ws/shard attributes
                try:
                    print('DIAG: bot.shard_count =', getattr(bot, 'shard_count', None))
                    print('DIAG: bot.shards =', getattr(bot, 'shards', None))
                    print('DIAG: bot.ws =', type(getattr(bot, 'ws', None)).__name__ if getattr(bot, 'ws', None) else None)
                except Exception:
                    pass
            except Exception as e:
                print('DIAG: error enumerating connection attributes:', e)
        except Exception as e:
            print('DIAG: error reading connection info:', e)
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

    # Application commands are registered during startup via commands.setup(bot)

    # Try to bulk-register commands via REST (so the running process is the author),
    # then sync the local tree. Doing this at on_ready ensures the bot's token is
    # used to create the commands and prevents externally-created commands from
    # being overwritten unexpectedly. This is guarded so it only runs once per
    # bot process (avoids repeated overwrites if on_ready fires multiple times).
    try:
        if guild:
            global _commands_registered
            if not _commands_registered:
                # Determine application id
                app_id = getattr(bot, 'application_id', None) or (getattr(bot.user, 'id', None) if getattr(bot, 'user', None) else None)
                if not app_id:
                    print('Could not determine application id for REST registration')
                else:
                    try:
                        # Build payload from local top-level chat input commands
                        payload = []
                        for cmd in bot.tree.walk_commands():
                            if getattr(cmd, 'parent', None):
                                continue
                            desc = getattr(cmd, 'description', '') or ''
                            payload.append({'name': cmd.name, 'description': desc, 'type': 1})

                        if payload:
                            # Use aiohttp and the configured TOKEN to perform the same
                            # PUT+GET flow as debug_register_check.py. This avoids
                            # depending on discord.py's internal http client and makes
                            # the behaviour identical to the diagnostic script.
                            try:
                                if not TOKEN:
                                    print('DISCORD token not configured; skipping REST registration')
                                else:
                                    API_BASE = 'https://discord.com/api/v10'
                                    headers = {
                                        'Authorization': f'Bot {TOKEN}',
                                        'Content-Type': 'application/json',
                                    }
                                    async with aiohttp.ClientSession(headers=headers) as sess:
                                        # Determine application id via /users/@me
                                        try:
                                            async with sess.get(f"{API_BASE}/users/@me") as r:
                                                me = await r.json()
                                                app_id = me.get('id')
                                                print('DIAG: determined app_id via /users/@me =>', app_id)
                                        except Exception as e:
                                            print('Could not determine application id via /users/@me:', e)

                                        if not app_id:
                                            print('No application id; aborting REST registration')
                                        else:
                                            put_url = f"{API_BASE}/applications/{app_id}/guilds/{GUILD_ID}/commands"
                                            print('DIAG: Performing PUT to bulk-register commands ->', put_url)
                                            try:
                                                async with sess.put(put_url, json=payload) as pr:
                                                    try:
                                                        data = await pr.json()
                                                        print('PUT status:', pr.status)
                                                        print('PUT response json count=', len(data) if isinstance(data, list) else 'n/a')
                                                    except Exception:
                                                        txt = await pr.text()
                                                        print('PUT response raw:', txt)
                                            except Exception as e:
                                                print('Error during PUT to register commands:', e)

                                            # GET to verify
                                            try:
                                                async with sess.get(put_url) as gr:
                                                    print('GET status:', gr.status)
                                                    try:
                                                        remote = await gr.json()
                                                        print('GET response json count=', len(remote) if isinstance(remote, list) else 'n/a')
                                                        for rc in (remote or []):
                                                            print(f" - {rc.get('name')} (id={rc.get('id')}) app={rc.get('application_id')} guild={rc.get('guild_id')}")
                                                    except Exception:
                                                        txt = await gr.text()
                                                        print('GET response raw:', txt)
                                            except Exception as e:
                                                print('Error during GET after PUT:', e)
                                    # mark as registered for this process
                                    _commands_registered = True
                            except Exception as e:
                                print('Error during aiohttp PUT+GET registration flow:', e)
                    except Exception as e:
                        print('Error during REST bulk-register:', e)

                    # Now optionally sync the bot tree to the guild (ensures local cache matches Discord)
                    # If we already performed the PUT+GET flow above, skip the discord.py sync
                    # to avoid discord.py potentially overwriting commands differently.
                    if not _commands_registered:
                        try:
                            await bot.tree.sync(guild=discord.Object(id=GUILD_ID))
                            print("Commands synced to guild")
                            print("Registered app commands:")
                            for cmd in bot.tree.walk_commands():
                                parent = getattr(cmd, "parent", None)
                                print(f" - {cmd.name} (group={parent.name if parent else 'none'}) -> {cmd.description}")
                            _commands_registered = True
                        except Exception as e:
                            print(f"Could not sync commands to guild: {e}")
            else:
                print('Commands already registered for this process; skipping REST/sync')
    except Exception as e:
        print(f"Unexpected error during command registration: {e}")


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
