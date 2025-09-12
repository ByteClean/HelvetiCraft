import discord
from discord.ext import commands, tasks
from dotenv import load_dotenv
import os
from mcstatus import JavaServer

# === LOAD ENV ===
load_dotenv()
TOKEN = os.getenv("DISCORD_TOKEN")
GUILD_ID = int(os.getenv("GUILD_ID"))
RULES_CHANNEL_ID = int(os.getenv("RULES_CHANNEL_ID"))
RULES_MESSAGE_ID = int(os.getenv("RULES_MESSAGE_ID"))
GUEST_ROLE = os.getenv("GUEST_ROLE")
PLAYER_ROLE = os.getenv("PLAYER_ROLE")
VERIFY_EMOJI = os.getenv("VERIFY_EMOJI")
MC_SERVER_URL = os.getenv("MC_SERVER_URL")

STATS_CATEGORY_NAME = "📊 Server-Stats"
MEMBER_CHANNEL_NAME = "👥 Mitglieder: {count}"
MC_CHANNEL_NAME = "🎮 Minecraft: {status}"

# === BOT SETUP ===
intents = discord.Intents.default()
intents.members = True
intents.guilds = True
intents.messages = True
intents.reactions = True

bot = commands.Bot(command_prefix="!", intents=intents)

# === FUNCTIONS ===
async def get_mc_status():
    """Ping the MC server and return Online/Offline + player count."""
    try:
        server = JavaServer.lookup(MC_SERVER_URL)
        status = server.status()
        return f"Online ({status.players.online}/{status.players.max})"
    except Exception:
        return "Offline"

async def setup_stats_channels(guild: discord.Guild):
    """Ensure the category and channels exist once."""
    category = discord.utils.get(guild.categories, name=STATS_CATEGORY_NAME)
    if not category:
        category = await guild.create_category(STATS_CATEGORY_NAME)
        overwrite = {guild.default_role: discord.PermissionOverwrite(connect=False, speak=False)}
        await category.edit(overwrites=overwrite)

    # Members channel
    member_count = sum(1 for m in guild.members if not m.bot)
    member_channel = discord.utils.get(category.voice_channels, name=MEMBER_CHANNEL_NAME.format(count=member_count))
    if not member_channel:
        old = discord.utils.find(lambda c: c.name.startswith("👥 Mitglieder:"), category.voice_channels)
        if old:
            await old.delete()
        await category.create_voice_channel(MEMBER_CHANNEL_NAME.format(count=member_count))

    # Minecraft status channel
    status = await get_mc_status()
    mc_channel = discord.utils.get(category.voice_channels, name=MC_CHANNEL_NAME.format(status=status))
    if not mc_channel:
        old = discord.utils.find(lambda c: c.name.startswith("🎮 Minecraft:"), category.voice_channels)
        if old:
            await old.delete()
        await category.create_voice_channel(MC_CHANNEL_NAME.format(status=status))

    return category

async def update_stats_channels(guild: discord.Guild):
    """Update the channel names without recreating them."""
    category = discord.utils.get(guild.categories, name=STATS_CATEGORY_NAME)
    if not category:
        category = await setup_stats_channels(guild)

    member_count = sum(1 for m in guild.members if not m.bot)
    status = await get_mc_status()

    for vc in category.voice_channels:
        if vc.name.startswith("👥 Mitglieder:"):
            await vc.edit(name=MEMBER_CHANNEL_NAME.format(count=member_count))
        elif vc.name.startswith("🎮 Minecraft:"):
            await vc.edit(name=MC_CHANNEL_NAME.format(status=status))

# === EVENTS ===
@bot.event
async def on_ready():
    print(f"✅ Bot is online as {bot.user}")
    guild = bot.get_guild(GUILD_ID)

    await setup_stats_channels(guild)

    # Add reaction to rules message if missing
    channel = guild.get_channel(RULES_CHANNEL_ID)
    try:
        message = await channel.fetch_message(RULES_MESSAGE_ID)
        if not any(reaction.emoji == VERIFY_EMOJI for reaction in message.reactions):
            await message.add_reaction(VERIFY_EMOJI)
            print("✅ Reaction added to rules message")
    except Exception as e:
        print(f"⚠️ Could not add reaction: {e}")

    update_stats_loop.start()  # Start periodic stats updater

@bot.event
async def on_member_join(member):
    # Assign Gast role
    role = discord.utils.get(member.guild.roles, name=GUEST_ROLE)
    if role:
        await member.add_roles(role)
        print(f"👋 Gast role assigned to {member.name}")

    await update_stats_channels(member.guild)

    # Send welcome message
    channel_id = int(os.getenv("WELCOME_CHANNEL_ID"))
    channel = member.guild.get_channel(channel_id)
    if channel:
        await channel.send(
            f"👋 Willkommen {member.mention}! "
            f"Bitte lies dir zuerst die Regeln im <#{RULES_CHANNEL_ID}> durch "
            f"und bestätige sie mit {VERIFY_EMOJI}, um freigeschaltet zu werden."
        )

@bot.event
async def on_member_remove(member):
    await update_stats_channels(member.guild)

@bot.event
async def on_raw_reaction_add(payload):
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
        print(f"🎉 {member.name} verified and role updated.")

# === TASKS ===
@tasks.loop(minutes=1)
async def update_stats_loop():
    guild = bot.get_guild(GUILD_ID)
    if guild:
        await update_stats_channels(guild)

# === RUN BOT ===
bot.run(TOKEN)
