"""Simple test script to create a guild command via the Discord REST API.

Usage:
  python Discord_Bot/test_create_command.py

It loads `Discord_Bot/.env` (via python-dotenv) and posts a small command. It prints the response.

Note: install dependencies if needed:
  pip install requests python-dotenv
"""
import os
import json
import sys
from dotenv import load_dotenv

load_dotenv('Discord_Bot/.env')

token = os.getenv('DISCORD_TOKEN')
if not token:
    print('DISCORD_TOKEN not found in Discord_Bot/.env', file=sys.stderr)
    sys.exit(1)

app_id = '1415779098114002964'  # use your application id from the DIAG output
guild_id = os.getenv('GUILD_ID')
if not guild_id:
    print('GUILD_ID not found in Discord_Bot/.env', file=sys.stderr)
    sys.exit(1)

import requests

url = f'https://discord.com/api/v10/applications/{app_id}/guilds/{guild_id}/commands'
headers = {'Authorization': f'Bot {token.strip()}', 'Content-Type': 'application/json'}
body = { 'name': 'probetest', 'description': 'Temporary diagnostic command' }

print('POST', url)
resp = requests.post(url, headers=headers, json=body)
print('Status:', resp.status_code)
try:
    print(json.dumps(resp.json(), indent=2))
except Exception:
    print(resp.text)

if resp.status_code in (200, 201):
    print('\nCreated command id:', resp.json().get('id'))
    print('To delete it, run:')
    print(f"curl -X DELETE -H \"Authorization: Bot <token>\" \"https://discord.com/api/v10/applications/{app_id}/guilds/{guild_id}/commands/{resp.json().get('id')}\"")
