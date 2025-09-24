"""Top-level slash commands for the Discord bot: /initiative, /networth, /finance.

The `/initiative` command uses a Discord Modal for a nicer UX.
"""
import discord
from discord import app_commands
from discord import ui
from discord import TextStyle
from Discord_Bot.initiatives_store import create_initiative
from Discord_Bot.config import COMMANDS_CHANNEL_NAME, INITIATIVES_CHANNEL_NAME, INITIATIVES_CHANNEL_ID, COMMANDS_CHANNEL_ID


class InitiativeModal(ui.Modal, title="Create Initiative"):
    title_input = ui.TextInput(label="Title", style=TextStyle.short, max_length=100)
    description_input = ui.TextInput(label="Description", style=TextStyle.long, max_length=2000)

    async def on_submit(self, interaction: discord.Interaction):
        # Create and post the initiative
        item = create_initiative(interaction.user.id, self.title_input.value, self.description_input.value)
        guild = interaction.guild
        if guild:
            chan = None
            if INITIATIVES_CHANNEL_ID:
                chan = guild.get_channel(INITIATIVES_CHANNEL_ID)
            if not chan:
                chan = discord.utils.get(guild.text_channels, name=INITIATIVES_CHANNEL_NAME)
            if chan:
                await chan.send(f"New Initiative #{item['id']} by {interaction.user.mention}\n**{item['title']}**\n{item['description']}")

        await interaction.response.send_message(f"Initiative created with id {item['id']}.", ephemeral=True)


@app_commands.command(name="initiative", description="Create a new initiative")
async def initiative(interaction: discord.Interaction):
    # Ensure command is issued in the commands channel (prefer ID if configured)
    invoked_channel_id = interaction.channel.id if interaction.channel else None
    print(f"DEBUG: /initiative invoked in channel id={invoked_channel_id}, configured COMMANDS_CHANNEL_ID={COMMANDS_CHANNEL_ID}")
    if COMMANDS_CHANNEL_ID and invoked_channel_id != COMMANDS_CHANNEL_ID:
        await interaction.response.send_message(f"Please use this command in the configured commands channel.", ephemeral=True)
        return
    if not COMMANDS_CHANNEL_ID and interaction.channel and interaction.channel.name != COMMANDS_CHANNEL_NAME:
        await interaction.response.send_message(f"Please use this command in #{COMMANDS_CHANNEL_NAME}", ephemeral=True)
        return

    # Send a modal for a nicer UX
    await interaction.response.send_modal(InitiativeModal())


@app_commands.command(name="networth", description="Display all player networth")
async def networth(interaction: discord.Interaction):
    invoked_channel_id = interaction.channel.id if interaction.channel else None
    print(f"DEBUG: /networth invoked in channel id={invoked_channel_id}, configured COMMANDS_CHANNEL_ID={COMMANDS_CHANNEL_ID}")
    if COMMANDS_CHANNEL_ID and invoked_channel_id != COMMANDS_CHANNEL_ID:
        await interaction.response.send_message("Please use the configured commands channel.", ephemeral=True)
        return

    # Placeholder player data — replace with real data source
    players = [("player1", 1000), ("player2", 500), ("player3", 250)]
    embed = discord.Embed(title="Player Networth", color=discord.Color.green())
    for name, amount in players:
        embed.add_field(name=name, value=f"{amount:,} coins", inline=False)
    embed.set_footer(text="Networth snapshot")
    await interaction.response.send_message(embed=embed, ephemeral=False)


@app_commands.command(name="finance", description="Show your finance stats")
async def finance(interaction: discord.Interaction):
    invoked_channel_id = interaction.channel.id if interaction.channel else None
    print(f"DEBUG: /finance invoked in channel id={invoked_channel_id}, configured COMMANDS_CHANNEL_ID={COMMANDS_CHANNEL_ID}")
    if COMMANDS_CHANNEL_ID and invoked_channel_id != COMMANDS_CHANNEL_ID:
        await interaction.response.send_message("Please use the configured commands channel.", ephemeral=True)
        return

    # Placeholder finance data — replace with real source
    balance = 12345
    income = 250
    expenses = 75
    embed = discord.Embed(title=f"Finance for {interaction.user.display_name}", color=discord.Color.blue())
    embed.add_field(name="Balance", value=f"{balance:,} coins", inline=True)
    embed.add_field(name="Income / day", value=f"{income:,}", inline=True)
    embed.add_field(name="Expenses / day", value=f"{expenses:,}", inline=True)
    embed.set_footer(text="Data is placeholder — connect a data source to show real stats")
    await interaction.response.send_message(embed=embed, ephemeral=True)


COMMANDS = [initiative, networth, finance]
