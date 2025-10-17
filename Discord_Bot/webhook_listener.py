# Discord_Bot/webhook_listener.py
import aiohttp
from aiohttp import web
import discord
from discord.ext import commands
import asyncio

# Import your channel config and backend endpoint settings
try:
    from Discord_Bot.config import (
        INITIATIVES_CHANNEL_ID,
        INITIATIVES_CHANNEL_NAME,
        ACCEPTED_CHANNEL_ID,
        ACCEPTED_CHANNEL_NAME,
        REJECTED_CHANNEL_ID,
        REJECTED_CHANNEL_NAME,
        BACKEND_VOTE_URL,
        NEWS_CHANNEL_ID,
        NEWS_CHANNEL_NAME,
    )
except ModuleNotFoundError:
    from config import (
        INITIATIVES_CHANNEL_ID,
        INITIATIVES_CHANNEL_NAME,
        ACCEPTED_CHANNEL_ID,
        ACCEPTED_CHANNEL_NAME,
        REJECTED_CHANNEL_ID,
        REJECTED_CHANNEL_NAME,
        BACKEND_VOTE_URL,
        NEWS_CHANNEL_ID,
        NEWS_CHANNEL_NAME,
    )

if not BACKEND_VOTE_URL:
    BACKEND_VOTE_URL = "http://127.0.0.1:9000/register_vote"


def _channel_by_fallback(guild, id_val, name_val):
    chan = None
    if id_val:
        chan = guild.get_channel(id_val)
    if not chan and name_val:
        chan = discord.utils.get(guild.text_channels, name=name_val)
    return chan


class InitiativeViewStep1(discord.ui.View):
    def __init__(self, initiative_id: int):
        super().__init__(timeout=None)
        self.initiative_id = str(initiative_id)
        self.signatures = 0

    async def _post_vote_to_backend(self, user: discord.User):
        payload = {
            "initiative_id": self.initiative_id,
            "action": "sign",
            "user_discord": f"{user.name}#{user.discriminator}",
            "user_id": user.id,
        }
        try:
            async with aiohttp.ClientSession() as s:
                async with s.post(BACKEND_VOTE_URL, json=payload, timeout=5) as resp:
                    print(f"[backend POST] sign -> status {resp.status}")
        except Exception as e:
            print(f"[backend POST error] sign: {e}")

    @discord.ui.button(label="Unterschreiben", style=discord.ButtonStyle.primary)
    async def sign(self, interaction: discord.Interaction, button: discord.ui.Button):
        await self._post_vote_to_backend(interaction.user)
        self.signatures += 1
        if interaction.message.embeds:
            embed = interaction.message.embeds[0]
            embed.set_footer(text=f"Signatures: {self.signatures} — Initiative ID: {self.initiative_id}")
            await interaction.message.edit(embed=embed, view=self)
        await interaction.response.send_message("✅ Du hast unterschrieben! Danke.", ephemeral=True)


class InitiativeViewStep2(discord.ui.View):
    def __init__(self, initiative_id: int):
        super().__init__(timeout=None)
        self.initiative_id = str(initiative_id)
        self.votes_for = 0
        self.votes_against = 0

    async def _post_vote_to_backend(self, user: discord.User, vote: str):
        payload = {
            "initiative_id": self.initiative_id,
            "action": "vote",
            "vote": vote,
            "user_discord": f"{user.name}#{user.discriminator}",
            "user_id": user.id,
        }
        try:
            async with aiohttp.ClientSession() as s:
                async with s.post(BACKEND_VOTE_URL, json=payload, timeout=5) as resp:
                    print(f"[backend POST] vote {vote} -> status {resp.status}")
        except Exception as e:
            print(f"[backend POST error] vote {vote}: {e}")

    @discord.ui.button(label="Für", style=discord.ButtonStyle.success)
    async def vote_for(self, interaction: discord.Interaction, button: discord.ui.Button):
        await self._post_vote_to_backend(interaction.user, "for")
        self.votes_for += 1
        if interaction.message.embeds:
            embed = interaction.message.embeds[0]
            embed.set_footer(text=f"Votes: {self.votes_for} ✅ / {self.votes_against} ❌ — Initiative ID: {self.initiative_id}")
            await interaction.message.edit(embed=embed, view=self)
        await interaction.response.send_message("✅ Deine Stimme wurde für gewertet.", ephemeral=True)

    @discord.ui.button(label="Gegen", style=discord.ButtonStyle.danger)
    async def vote_against(self, interaction: discord.Interaction, button: discord.ui.Button):
        await self._post_vote_to_backend(interaction.user, "against")
        self.votes_against += 1
        if interaction.message.embeds:
            embed = interaction.message.embeds[0]
            embed.set_footer(text=f"Votes: {self.votes_for} ✅ / {self.votes_against} ❌ — Initiative ID: {self.initiative_id}")
            await interaction.message.edit(embed=embed, view=self)
        await interaction.response.send_message("❌ Deine Stimme wurde dagegen gewertet.", ephemeral=True)


class WebhookListener(commands.Cog):
    def __init__(self, bot: commands.Bot):
        self.bot = bot
        self.app = web.Application()
        self.app.add_routes([
            web.post('/initiative-webhook', self.handle_initiative),
            web.post('/initiative-votes-update', self.update_votes),
            web.post('/initiative-change', self.handle_change),
            web.post('/news-create', self.handle_news_create),
            web.post('/news-delete', self.handle_news_delete),
            web.get('/news-list', self.handle_news_list),
        ])
        self.runner: web.AppRunner | None = None
        self.site: web.TCPSite | None = None

    async def cog_load(self):
        self.runner = web.AppRunner(self.app)
        await self.runner.setup()
        self.site = web.TCPSite(self.runner, "127.0.0.1", 8081)
        await self.site.start()
        print("✅ Webhook listener running on http://127.0.0.1:8081")

    async def cog_unload(self):
        if self.runner:
            await self.runner.cleanup()
            print("🛑 Webhook listener stopped")

    async def _find_message_for_initiative(self, channel: discord.TextChannel, initiative_id: str, lookback: int = 200):
        async for msg in channel.history(limit=lookback):
            if not msg.embeds:
                continue
            emb = msg.embeds[0]
            found = False
            if emb.footer and emb.footer.text and str(initiative_id) in emb.footer.text:
                found = True
            if not found:
                for f in emb.fields:
                    if str(initiative_id) in str(f.value) or str(initiative_id) in str(f.name):
                        found = True
                        break
            if found:
                return msg
        return None
    
    async def _get_all_news_messages(self, channel: discord.TextChannel, limit: int = 200):
        """Return a list of news messages in the channel with their IDs and titles."""
        news_messages = []
        async for msg in channel.history(limit=limit):
            if msg.embeds:
                embed = msg.embeds[0]
                news_messages.append({
                    "id": msg.id,
                    "title": embed.title if embed.title else "",
                })
        return news_messages

    async def handle_initiative(self, request: web.Request):
        try:
            data = await request.json()
            initiative_id = data.get("id")
            step = int(data.get("step", 1))
            title = data.get("title", "Untitled")
            description = data.get("description", "")
            owner_discord = data.get("owner_discord", "Unknown")
            owner_minecraft = data.get("owner_minecraft", "Unknown")

            guild = self.bot.guilds[0]
            target_channel = _channel_by_fallback(guild, INITIATIVES_CHANNEL_ID, INITIATIVES_CHANNEL_NAME)
            if not target_channel:
                return web.json_response({"error": "No target channel configured"}, status=404)

            old_msg = await self._find_message_for_initiative(target_channel, initiative_id)
            if old_msg:
                try: await old_msg.delete()
                except Exception: pass

            embed = discord.Embed(title=f"{title}", description=f"**{description[:200]}**", color=discord.Color.orange())
            embed.add_field(name="Owner", value=f"{owner_discord} • {owner_minecraft}", inline=False)
            embed.add_field(name="Initiative ID", value=str(initiative_id), inline=False)
            embed.add_field(name="Beschreibung", value=description or "No description provided.", inline=False)

            if step == 1:
                embed.set_footer(text=f"Signatures: 0 — Initiative ID: {initiative_id}")
                view = InitiativeViewStep1(initiative_id)
            else:
                embed.set_footer(text=f"Votes: 0 ✅ / 0 ❌ — Initiative ID: {initiative_id}")
                view = InitiativeViewStep2(initiative_id)

            sent = await target_channel.send(embed=embed, view=view)
            print(f"[posted] initiative {initiative_id} in {target_channel.name} (step {step}) msg_id={sent.id}")
            return web.json_response({"status": "ok", "posted_in": target_channel.name})
        except Exception as e:
            print(f"[handle_initiative error] {e}")
            return web.json_response({"error": str(e)}, status=500)

    async def update_votes(self, request: web.Request):
        try:
            data = await request.json()
            initiative_id = str(data.get("id"))
            step = int(data.get("step", 1))
            votes_for = int(data.get("votes_for", 0))
            votes_against = int(data.get("votes_against", 0))
            signatures = int(data.get("signatures", 0))

            guild = self.bot.guilds[0]
            channel = _channel_by_fallback(guild, INITIATIVES_CHANNEL_ID, INITIATIVES_CHANNEL_NAME)
            if not channel:
                return web.json_response({"error": "No initiatives channel configured"}, status=404)

            msg = await self._find_message_for_initiative(channel, initiative_id)
            if not msg:
                return web.json_response({"error": "initiative message not found"}, status=404)

            embed = msg.embeds[0]
            if step == 1:
                embed.set_footer(text=f"Signatures: {signatures} — Initiative ID: {initiative_id}")
            else:
                embed.set_footer(text=f"Votes: {votes_for} ✅ / {votes_against} ❌ — Initiative ID: {initiative_id}")

            await msg.edit(embed=embed)
            print(f"[update_votes] updated initiative {initiative_id} (step {step})")
            return web.json_response({"status": "ok"})
        except Exception as e:
            print(f"[update_votes error] {e}")
            return web.json_response({"error": str(e)}, status=500)

    async def handle_change(self, request: web.Request):
        try:
            data = await request.json()
            initiative_id = str(data.get("id"))
            action = data.get("action")
            result = data.get("result")
            owner_discord = data.get("owner_discord", "Unknown")
            owner_minecraft = data.get("owner_minecraft", "Unknown")

            guild = self.bot.guilds[0]
            initiatives_channel = _channel_by_fallback(guild, INITIATIVES_CHANNEL_ID, INITIATIVES_CHANNEL_NAME)
            accepted_channel = _channel_by_fallback(guild, ACCEPTED_CHANNEL_ID, ACCEPTED_CHANNEL_NAME)
            rejected_channel = _channel_by_fallback(guild, REJECTED_CHANNEL_ID, REJECTED_CHANNEL_NAME)

            old_msg = await self._find_message_for_initiative(initiatives_channel, initiative_id)

            if action == "promote":
                if old_msg:
                    try: await old_msg.delete()
                    except Exception: pass

                title = data.get("title", old_msg.embeds[0].title if old_msg and old_msg.embeds else "Untitled")
                description = data.get("description", old_msg.embeds[0].description if old_msg and old_msg.embeds else "")
                owner_field = old_msg.embeds[0].fields[0].value if old_msg and old_msg.embeds else f"{owner_discord} • {owner_minecraft}"

                embed = discord.Embed(title=title, description=description, color=discord.Color.orange())
                embed.add_field(name="Owner", value=owner_field, inline=False)
                embed.add_field(name="Initiative ID", value=initiative_id, inline=False)
                embed.add_field(name="Beschreibung", value=description or "No description.", inline=False)
                embed.set_footer(text=f"Votes: 0 ✅ / 0 ❌ — Initiative ID: {initiative_id}")
                view = InitiativeViewStep2(initiative_id)
                sent = await initiatives_channel.send(embed=embed, view=view)
                print(f"[promote] initiative {initiative_id} posted in {initiatives_channel.name} as step 2 msg_id={sent.id}")
                return web.json_response({"status": "promoted"})

            elif action == "finalize":
                if old_msg:
                    try: await old_msg.delete()
                    except Exception: pass

                embed = discord.Embed(title=f"", description="", color=discord.Color.green() if result=="accepted" else discord.Color.red())
                embed.add_field(name="Initiative ID", value=initiative_id, inline=False)
                embed.add_field(name="Beschreibung", value=data.get("description", ""), inline=False)
                embed.add_field(name="Erstellt von", value=f"{owner_discord} • {owner_minecraft}", inline=False)

                if result == "accepted":
                    if not accepted_channel:
                        return web.json_response({"error": "accepted channel not found"}, status=404)
                    embed.title = f"✅ Accepted Initiative #{initiative_id}"
                    await accepted_channel.send(embed=embed)
                    print(f"[finalize] initiative {initiative_id} posted to accepted channel")
                    return web.json_response({"status": "accepted"})

                elif result == "rejected":
                    if not rejected_channel:
                        return web.json_response({"error": "rejected channel not found"}, status=404)
                    embed.title = f"❌ Rejected Initiative #{initiative_id}"
                    await rejected_channel.send(embed=embed)
                    print(f"[finalize] initiative {initiative_id} posted to rejected channel")
                    return web.json_response({"status": "rejected"})

            return web.json_response({"error": "unknown action"}, status=400)
        except Exception as e:
            print(f"[handle_change error] {e}")
            return web.json_response({"error": str(e)}, status=500)
        
    # === NEWS HANDLERS ===
    async def handle_news_create(self, request: web.Request):
        """POST /news-create -> creates a new news post with optional image."""
        try:
            data = await request.json()
            title = data.get("title", "📰 Neue Ankündigung")
            content = data.get("content", "")
            author = data.get("author", "Server Team")
            image_url = data.get("image_url")  # optional
    
            guild = self.bot.guilds[0]
            news_channel = _channel_by_fallback(guild, NEWS_CHANNEL_ID, NEWS_CHANNEL_NAME)
            if not news_channel:
                return web.json_response({"error": "news channel not found"}, status=404)
    
            embed = discord.Embed(
                title=title,
                description=content or "Keine Details angegeben.",
                color=discord.Color.blurple()
            )
            embed.set_footer(text=f"Veröffentlicht von {author}")
    
            if image_url:
                embed.set_image(url=image_url)
    
            sent = await news_channel.send(embed=embed)
            print(f"[news-create] Posted announcement: {title} (msg_id={sent.id})")
            return web.json_response({"status": "ok", "message_id": sent.id})
        except Exception as e:
            print(f"[handle_news_create error] {e}")
            return web.json_response({"error": str(e)}, status=500)

    
    
    async def handle_news_delete(self, request: web.Request):
        """POST /news-delete -> deletes a news post by message_id."""
        try:
            data = await request.json()
            message_id = int(data.get("message_id"))
    
            guild = self.bot.guilds[0]
            news_channel = _channel_by_fallback(guild, NEWS_CHANNEL_ID, NEWS_CHANNEL_NAME)
            if not news_channel:
                return web.json_response({"error": "news channel not found"}, status=404)
    
            msg = await news_channel.fetch_message(message_id)
            await msg.delete()
            print(f"[news-delete] Deleted message {message_id}")
            return web.json_response({"status": "deleted"})
        except Exception as e:
            print(f"[handle_news_delete error] {e}")
            return web.json_response({"error": str(e)}, status=500)
    
    async def handle_news_list(self, request: web.Request):
        """Return all news posts currently in Discord (so backend knows the IDs)."""
        try:
            guild = self.bot.guilds[0]
            news_channel = _channel_by_fallback(guild, NEWS_CHANNEL_ID, NEWS_CHANNEL_NAME)
            if not news_channel:
                return web.json_response({"error": "News channel not found"}, status=404)

            news_messages = await self._get_all_news_messages(news_channel)
            print(f"[news-list] Returning {len(news_messages)} news messages")
            return web.json_response({"news_posts": news_messages})
        except Exception as e:
            print(f"[handle_news_list error] {e}")
            return web.json_response({"error": str(e)}, status=500)


async def setup(bot: commands.Bot):
    await bot.add_cog(WebhookListener(bot))
