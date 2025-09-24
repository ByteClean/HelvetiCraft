"""Configuration loader for the Discord bot.

Loads environment variables and exposes constants used across modules.
"""
from dotenv import load_dotenv
import os

load_dotenv()


def _int_env(key, default=None):
	v = os.getenv(key)
	if v is None or v == "":
		return default
	try:
		return int(v)
	except ValueError:
		return default


# Discord / environment
TOKEN = os.getenv("DISCORD_TOKEN")
GUILD_ID = _int_env("GUILD_ID")
RULES_CHANNEL_ID = _int_env("RULES_CHANNEL_ID")
RULES_MESSAGE_ID = _int_env("RULES_MESSAGE_ID")
WELCOME_CHANNEL_ID = _int_env("WELCOME_CHANNEL_ID")
GUEST_ROLE = os.getenv("GUEST_ROLE")
PLAYER_ROLE = os.getenv("PLAYER_ROLE")
VERIFY_EMOJI = os.getenv("VERIFY_EMOJI")
MC_SERVER_URL = os.getenv("MC_SERVER_URL")

# Channel / category names
STATS_CATEGORY_NAME = "📊 Server-Stats"
MEMBER_CHANNEL_NAME = "👥 Mitglieder: {count}"
MC_CHANNEL_NAME = "🎮 Minecraft: {status}"

# Text channels used by the bot
COMMANDS_CHANNEL_NAME = "bot-commands"
INITIATIVES_CHANNEL_NAME = "volksinitiativen"

# Optional: specify channel IDs to enforce commands by channel id instead of name
COMMANDS_CHANNEL_ID = _int_env("COMMANDS_CHANNEL_ID")
INITIATIVES_CHANNEL_ID = _int_env("INITIATIVES_CHANNEL_ID")
