"""Top-level slash commands for the Discord bot: /initiative, /networth, /finance, /verify.

Uses dummy API requests in the /requests folder.
"""

import discord
from discord import ui, app_commands, TextStyle
from discord.ext import commands
import os

# === Import from dummy requests ===
try:
    from Discord_Bot.http_requests.initiative_requests import (
        create_initiative_request,
        get_user_initiatives_request,
    )
    from Discord_Bot.http_requests.networth_requests import get_all_networths_request
    from Discord_Bot.http_requests.finance_requests import get_finance_data_request
    from Discord_Bot.http_requests.verify_requests import verify_code_request
    from Discord_Bot.config import (
        COMMANDS_CHANNEL_NAME,
        INITIATIVES_CHANNEL_NAME,
        INITIATIVES_CHANNEL_ID,
        COMMANDS_CHANNEL_ID,
    )
except ModuleNotFoundError:
    from http_requests.initiative_requests import (
        create_initiative_request,
        get_user_initiatives_request,
    )
    from http_requests.networth_requests import get_all_networths_request
    from http_requests.finance_requests import get_finance_data_request
    from http_requests.verify_requests import verify_code_request
    from config import (
        COMMANDS_CHANNEL_NAME,
        INITIATIVES_CHANNEL_NAME,
        INITIATIVES_CHANNEL_ID,
        COMMANDS_CHANNEL_ID,
    )

# === Initiative Modal ===
class InitiativeModal(ui.Modal, title="Create Initiative"):
    title_input = ui.TextInput(label="Title", style=TextStyle.short, max_length=100)
    description_input = ui.TextInput(label="Description", style=TextStyle.long, max_length=2000)

    async def on_submit(self, interaction: discord.Interaction):
        # Only send the creation request to backend, no channel posting here.
        item = create_initiative_request(
            interaction.user.id, self.title_input.value, self.description_input.value
        )

        await interaction.response.send_message(
            f"✅ Initiative submitted successfully! (backend id: {item['id']})\n"
            f"It will be posted automatically once the backend confirms it.",
            ephemeral=True
        )


# === Initiative Command Group ===
class Initiative(commands.GroupCog, name="initiative"):
    """Manage your initiatives"""

    @app_commands.command(name="new", description="Create a new initiative")
    async def new(self, interaction: discord.Interaction):
        if COMMANDS_CHANNEL_ID and interaction.channel.id != COMMANDS_CHANNEL_ID:
            await interaction.response.send_message(
                f"Please use this command in the configured commands channel.", ephemeral=True
            )
            return
        await interaction.response.send_modal(InitiativeModal())

    @app_commands.command(name="own", description="View your own initiatives (only visible to you)")
    async def own(self, interaction: discord.Interaction):
        user_id = interaction.user.id
        initiatives = get_user_initiatives_request(user_id)
        if not initiatives:
            await interaction.response.send_message("You don’t have any initiatives yet.", ephemeral=True)
            return

        embed = discord.Embed(
            title=f"Your Initiatives ({len(initiatives)})",
            color=discord.Color.orange()
        )
        for item in initiatives[:10]:
            embed.add_field(
                name=f"#{item['id']} — {item['title']}",
                value=item.get("description", "No description."),
                inline=False,
            )
        if len(initiatives) > 10:
            embed.set_footer(text="Only showing first 10 initiatives.")
        await interaction.response.send_message(embed=embed, ephemeral=True)


# === /verify command ===
class Verify(commands.Cog):
    """Handle Minecraft/Discord verification"""

    def __init__(self, bot: commands.Bot):
        self.bot = bot

    @app_commands.command(name="verify", description="Verify your Minecraft account with a code")
    @app_commands.describe(code="The verification code you received in Minecraft")
    async def verify(self, interaction: discord.Interaction, code: str):
        # Dummy backend request
        success, message = verify_code_request(interaction.user.id, code)

        if success:
            await interaction.response.send_message(
                f"✅ Verification successful! {message}", ephemeral=True
            )
        else:
            await interaction.response.send_message(
                f"❌ Verification failed: {message}", ephemeral=True
            )


# === Setup Function ===
async def setup(bot: commands.Bot):
    # Add /initiative group
    await bot.add_cog(Initiative(bot))

    # Add /verify command
    await bot.add_cog(Verify(bot))

    # === /networth ===
    @bot.tree.command(name="networth", description="Display all player networth")
    async def networth(interaction: discord.Interaction):
        players = get_all_networths_request()
        embed = discord.Embed(title="Player Networth", color=discord.Color.green())
        for p in players:
            embed.add_field(name=p["name"], value=f"{p['amount']:,} CHF", inline=False)
        embed.set_footer(text="Networth snapshot")
        await interaction.response.send_message(embed=embed, ephemeral=False)

    # === /finance ===
    @bot.tree.command(name="finance", description="Show your finance stats")
    async def finance(interaction: discord.Interaction):
        data = get_finance_data_request(interaction.user.id)
        embed = discord.Embed(
            title=f"Finanzen für {interaction.user.display_name}",
            color=discord.Color.blue(),
        )
        embed.add_field(name="Saldo", value=f"{data['balance']:,} CHF", inline=True)
        embed.add_field(name="Ersparnisse", value=f"{data['savings']:,} CHF", inline=True)
        embed.add_field(name="Total", value=f"{data['total']:,} CHF", inline=True)
        embed.set_footer(text="Data is placeholder — connect a data source to show real stats")
        await interaction.response.send_message(embed=embed, ephemeral=True)

    print("✅ Registered slash commands (initiative group, networth, finance, verify)")
