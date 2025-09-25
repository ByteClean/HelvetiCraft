"""Debug helper: PUT a set of guild commands using the bot token, then GET them back.

This mirrors the requests the running bot makes but uses plain requests so we can compare results.

Run from repository root:
    python Discord_Bot\debug_register_check.py
"""
from dotenv import load_dotenv
import os
import requests
import json

load_dotenv()
TOKEN = os.getenv("DISCORD_TOKEN")
GUILD_ID = os.getenv("GUILD_ID")
API_BASE = "https://discord.com/api/v10"

if not TOKEN or not GUILD_ID:
    print("ERROR: DISCORD_TOKEN and GUILD_ID must be set in .env")
    raise SystemExit(1)

headers = {
    "Authorization": f"Bot {TOKEN}",
    "Content-Type": "application/json",
}

# Get the bot/user id from the API
r = requests.get(f"{API_BASE}/users/@me", headers=headers)
print("GET /users/@me ->", r.status_code)
print(r.text)
if r.status_code != 200:
    raise SystemExit("Could not fetch bot user info")

app_id = r.json().get("id")
print("Determined application_id=", app_id)

# Build payload of top-level chat input commands (mirror what's in commands.py)
payload = [
    {"name": "initiative", "description": "Create a new initiative", "type": 1},
    {"name": "networth", "description": "Display all player networth", "type": 1},
    {"name": "finance", "description": "Show your finance stats", "type": 1},
    {"name": "diag", "description": "Diagnostic command: logs and replies", "type": 1},
]

put_url = f"{API_BASE}/applications/{app_id}/guilds/{GUILD_ID}/commands"
print("PUT ->", put_url)
pr = requests.put(put_url, headers=headers, json=payload)
print("PUT status:", pr.status_code)
try:
    print("PUT response json:", json.dumps(pr.json(), indent=2))
except Exception:
    print("PUT response raw:", pr.text)

# Now GET the remote guild commands
gr = requests.get(put_url, headers=headers)
print("GET status:", gr.status_code)
try:
    data = gr.json()
    print("GET response json:", json.dumps(data, indent=2))
except Exception:
    print("GET response raw:", gr.text)

print("Done")
