import discord
from discord.ext import commands
from .initiatives_store import create_initiative
from .config import (
    COMMANDS_CHANNEL_ID,
    COMMANDS_CHANNEL_NAME,
    INITIATIVES_CHANNEL_ID,
    INITIATIVES_CHANNEL_NAME,
)


class TextCommands(commands.Cog):
    """Legacy prefix commands (e.g. `!initiative`) that mirror slash command UX.

    For `!initiative` we recommend using the slash command which shows a modal.
    The finance/networth commands return the same embeds as the slash commands.
    """

    def __init__(self, bot: commands.Bot):
        self.bot = bot

    @commands.command(name="initiative")
    async def initiative(self, ctx: commands.Context):
        invoked_channel_id = ctx.channel.id
        print(
            f"DEBUG: !initiative invoked in channel id={invoked_channel_id}, configured COMMANDS_CHANNEL_ID={COMMANDS_CHANNEL_ID}"
        )
        if COMMANDS_CHANNEL_ID and invoked_channel_id != COMMANDS_CHANNEL_ID:
            await ctx.send("Please use the configured commands channel.")
            return
        if not COMMANDS_CHANNEL_ID and ctx.channel.name != COMMANDS_CHANNEL_NAME:
            await ctx.send(f"Please use #{COMMANDS_CHANNEL_NAME} to run commands.")
            return

        # Recommend the slash command which opens a Modal for a nicer UX
        await ctx.send("Please use the slash command `/initiative` to open the initiative form.")
        return

    @commands.command(name="networth")
    async def networth(self, ctx: commands.Context):
        invoked_channel_id = ctx.channel.id
        print(
            f"DEBUG: !networth invoked in channel id={invoked_channel_id}, configured COMMANDS_CHANNEL_ID={COMMANDS_CHANNEL_ID}"
        )
        if COMMANDS_CHANNEL_ID and invoked_channel_id != COMMANDS_CHANNEL_ID:
            await ctx.send("Please use the configured commands channel.")
            return

        # Placeholder data — mirror the slash command embed style
        players = [("player1", 1000), ("player2", 500), ("player3", 250)]
        embed = discord.Embed(title="Player Networth", color=discord.Color.green())
        for name, amount in players:
            embed.add_field(name=name, value=f"{amount:,} coins", inline=False)
        await ctx.send(embed=embed)

    @commands.command(name="finance")
    async def finance(self, ctx: commands.Context):
        invoked_channel_id = ctx.channel.id
        print(
            f"DEBUG: !finance invoked in channel id={invoked_channel_id}, configured COMMANDS_CHANNEL_ID={COMMANDS_CHANNEL_ID}"
        )
        if COMMANDS_CHANNEL_ID and invoked_channel_id != COMMANDS_CHANNEL_ID:
            await ctx.send("Please use the configured commands channel.")
            return

        # Placeholder finance data
        balance = 12345
        income = 250
        expenses = 75
        embed = discord.Embed(
            title=f"Finance for {ctx.author.display_name}", color=discord.Color.blue()
        )
        embed.add_field(name="Balance", value=f"{balance:,} coins", inline=True)
        embed.add_field(name="Income / day", value=f"{income:,}", inline=True)
        embed.add_field(name="Expenses / day", value=f"{expenses:,}", inline=True)
        await ctx.send(embed=embed)

    @commands.command(name="sync_commands")
    @commands.has_guild_permissions(manage_guild=True)
    async def sync_commands(self, ctx: commands.Context):
        """Force the bot to bulk-register and sync application commands for the guild.

        This uses the bot's token to author the guild commands (same as on_ready logic).
        """
        bot = self.bot
        app_id = getattr(bot, 'application_id', None) or (getattr(bot.user, 'id', None) if getattr(bot, 'user', None) else None)
        if not app_id:
            await ctx.send("Could not determine application id for registration.")
            return
        try:
            payload = []
            for cmd in bot.tree.walk_commands():
                if getattr(cmd, 'parent', None):
                    continue
                desc = getattr(cmd, 'description', '') or ''
                payload.append({'name': cmd.name, 'description': desc, 'type': 1})
            route = discord.http.Route('PUT', '/applications/{application_id}/guilds/{guild_id}/commands', application_id=app_id, guild_id=ctx.guild.id)
            data = await bot.http.request(route, json=payload)
            await bot.tree.sync(guild=discord.Object(id=ctx.guild.id))
            await ctx.send(f"Force-registered {len(payload)} commands. Response entries: {len(data)}")
        except Exception as e:
            await ctx.send(f"Error during force-register: {e}")


async def setup(bot: commands.Bot):
    await bot.add_cog(TextCommands(bot))
