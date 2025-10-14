"""Manage statistics category and voice channels in a guild."""
import discord
try:
    from Discord_Bot.config import STATS_CATEGORY_NAME, MEMBER_CHANNEL_NAME, MC_CHANNEL_NAME, IP_CHANNEL_NAME, MC_SERVER_URL
    from Discord_Bot.mc_status import get_mc_status
except ModuleNotFoundError:
    from config import STATS_CATEGORY_NAME, MEMBER_CHANNEL_NAME, MC_CHANNEL_NAME, IP_CHANNEL_NAME, MC_SERVER_URL
    from mc_status import get_mc_status


async def setup_stats_channels(guild: discord.Guild):
    """Ensure the category and channels exist once."""
    category = discord.utils.get(guild.categories, name=STATS_CATEGORY_NAME)
    if not category:
        category = await guild.create_category(STATS_CATEGORY_NAME)
        overwrite = {guild.default_role: discord.PermissionOverwrite(connect=False, speak=False)}
        await category.edit(overwrites=overwrite)

    # --- Members channel ---
    member_count = sum(1 for m in guild.members if not m.bot)
    member_channel = discord.utils.get(category.voice_channels, name=MEMBER_CHANNEL_NAME.format(count=member_count))
    if not member_channel:
        old = discord.utils.find(lambda c: c.name.startswith("👥 Mitglieder:"), category.voice_channels)
        if old:
            await old.delete()
        await category.create_voice_channel(MEMBER_CHANNEL_NAME.format(count=member_count))

    # --- Minecraft status channel ---
    status = await get_mc_status()
    mc_channel = discord.utils.get(category.voice_channels, name=MC_CHANNEL_NAME.format(status=status))
    if not mc_channel:
        old = discord.utils.find(lambda c: c.name.startswith("🎮 Minecraft:"), category.voice_channels)
        if old:
            await old.delete()
        await category.create_voice_channel(MC_CHANNEL_NAME.format(status=status))

    # --- IP address channel ---
    ip_channel = discord.utils.get(category.voice_channels, name=IP_CHANNEL_NAME.format(MC_SERVER_URL=MC_SERVER_URL))
    if not ip_channel:
        old = discord.utils.find(lambda c: c.name.startswith("🔌 IP:"), category.voice_channels)
        if old:
            await old.delete()
        await category.create_voice_channel(IP_CHANNEL_NAME.format(MC_SERVER_URL=MC_SERVER_URL))

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
        elif vc.name.startswith("🔌 IP:"):
            await vc.edit(name=IP_CHANNEL_NAME.format(MC_SERVER_URL=MC_SERVER_URL))
