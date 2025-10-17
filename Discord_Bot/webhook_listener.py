import aiohttp
from aiohttp import web
import discord
from discord.ext import commands

# Import your channel config
try:
    from Discord_Bot.config import INITIATIVES_CHANNEL_ID, INITIATIVES_CHANNEL_NAME
except ModuleNotFoundError:
    from config import INITIATIVES_CHANNEL_ID, INITIATIVES_CHANNEL_NAME


class WebhookListener(commands.Cog):
    """Cog to handle incoming webhooks from the backend."""

    def __init__(self, bot: commands.Bot):
        self.bot = bot
        self.app = web.Application()
        self.app.add_routes([web.post('/initiative-webhook', self.handle_initiative)])
        self.runner: web.AppRunner | None = None
        self.site: web.TCPSite | None = None

    async def cog_load(self):
        """Start aiohttp server when the cog loads."""
        self.runner = web.AppRunner(self.app)
        await self.runner.setup()
        self.site = web.TCPSite(self.runner, "127.0.0.1", 8081)
        await self.site.start()
        print("✅ Webhook listener running on http://127.0.0.1:8081")

    async def cog_unload(self):
        """Stop aiohttp server when the cog unloads."""
        if self.runner:
            await self.runner.cleanup()
            print("🛑 Webhook listener stopped")

    async def handle_initiative(self, request: web.Request):
        """Receive POST webhook from backend and post to Discord."""
        try:
            data = await request.json()
            print("⚙️ handle_initiative triggered")
            print(f"Received webhook: {data}")
    
            guilds = self.bot.guilds
            if not guilds:
                return web.json_response({"error": "Bot not connected to any guilds"}, status=500)
    
            guild = guilds[0]
    
            channel = guild.get_channel(INITIATIVES_CHANNEL_ID) or \
                      discord.utils.get(guild.text_channels, name=INITIATIVES_CHANNEL_NAME)
    
            if not channel:
                return web.json_response({"error": "Could not find initiative channel"}, status=404)
    
            # Compose the embed according to your requested format
            owner_name = f"{data.get('discord_name', 'Unknown')} | {data.get('minecraft_name', 'Unknown')}"
            initiative_id = data.get("id", "?")
            title = data.get("title", "Untitled")
            description = data.get("description", "")
    
            embed = discord.Embed(
                title=f"{title}",
                description=f"**{description}**",
                color=discord.Color.orange()
            )
            embed.set_author(name=owner_name)
            embed.add_field(name="Initiative ID", value=str(initiative_id), inline=False)
    
            await channel.send(embed=embed)
            return web.json_response({"status": "ok"})
    
        except Exception as e:
            print(f"Webhook error: {e}")
            return web.json_response({"error": str(e)}, status=500)



async def setup(bot: commands.Bot):
    """Discord.py 2.0+ automatic cog setup."""
    await bot.add_cog(WebhookListener(bot))
