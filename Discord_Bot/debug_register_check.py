"""Debug helper: PUT a set of guild commands using the bot token, then GET them back.

This mirrors the requests the running bot makes but uses plain requests so we can compare results.

Run from repository root:
    python Discord_Bot/debug_register_check.py
"""
from dotenv import load_dotenv
import os
import requests
import json
import sys

# ===============================
# Load environment and constants
# ===============================
load_dotenv()
TOKEN = os.getenv("DISCORD_TOKEN")
GUILD_ID = os.getenv("GUILD_ID")
API_BASE = "https://discord.com/api/v10"

if not TOKEN or not GUILD_ID:
    sys.exit("❌ ERROR: DISCORD_TOKEN and GUILD_ID must be set in .env")

headers = {
    "Authorization": f"Bot {TOKEN}",
    "Content-Type": "application/json",
}

# ===============================
# Get bot application info
# ===============================
r = requests.get(f"{API_BASE}/users/@me", headers=headers)
print("GET /users/@me ->", r.status_code)
if r.status_code != 200:
    sys.exit(f"❌ Could not fetch bot user info: {r.text}")

app_id = r.json().get("id")
print(f"✅ Determined application_id = {app_id}")

# ================================================================
# Guild command payload — matches bot’s app_commands.GroupCog
# ================================================================
payload = [
    {
        "name": "initiative",
        "type": 1,
        "description": "Manage your initiatives",
        "options": [
            {
                "type": 1,
                "name": "new",
                "description": "Create a new initiative"
            },
            {
                "type": 1,
                "name": "own",
                "description": "View your own initiatives"
            }
        ]
    },
    {
        "name": "networth",
        "description": "Display all player networth",
        "type": 1,
    },
    {
        "name": "finance",
        "description": "Show your finance stats",
        "type": 1,
    },
    {
        "name": "verify",
        "description": "Verify your Minecraft account with a code",
        "type": 1,
        "options": [
            {
                "type": 3,  # STRING
                "name": "code",
                "description": "The verification code you received in Minecraft",
                "required": True
            }
        ]
    },
    {
        "name": "diag",
        "description": "Diagnostic command: logs and replies",
        "type": 1,
    },
]

# ===============================
# Sync commands to Discord
# ===============================
put_url = f"{API_BASE}/applications/{app_id}/guilds/{GUILD_ID}/commands"
print(f"PUT -> {put_url}")
pr = requests.put(put_url, headers=headers, json=payload)
print("PUT status:", pr.status_code)

try:
    print("PUT response json:", json.dumps(pr.json(), indent=2))
except Exception:
    print("PUT response raw:", pr.text)

# ===============================
# Verify results
# ===============================
print("\nFetching back registered guild commands...")
gr = requests.get(put_url, headers=headers)
print("GET status:", gr.status_code)

try:
    data = gr.json()
    print("GET response json:", json.dumps(data, indent=2))
except Exception:
    print("GET response raw:", gr.text)

print("\n✅ Done.")
