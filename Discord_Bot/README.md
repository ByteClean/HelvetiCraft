# HelvetiCraft Discord Bot

Ein Discord-Bot für den HelvetiCraft Server.  
Er verwaltet Rollen, sendet Willkommensnachrichten und zeigt Live-Statistiken wie die Mitgliederzahl und den Minecraft-Serverstatus an.

---

## Setup

### 1. Repository klonen
```bash
git clone https://github.com/ByteClean/HelvetiCraft.git
cd HelvetiCraft/Discord_Bot
```

### 2. Virtuelle Umgebung erstellen (empfohlen)
```
python -m venv venv
source venv/bin/activate   # Linux/macOS
venv\Scripts\activate      # Windows
```

### 3. Abhängigkeiten installieren

```
pip install -r requirements.txt
```

### 4. ``.env`` Datei erstellen

Lege im ``Discord_Bot/``-Ordner eine .env Datei mit folgendem Inhalt an:
```yml
DISCORD_TOKEN=dein_discord_token
GUILD_ID=123456789012345678
RULES_CHANNEL_ID=123456789012345678
RULES_MESSAGE_ID=123456789012345678
WELCOME_CHANNEL_ID=123456789012345678
GUEST_ROLE=Gast
PLAYER_ROLE=Spieler
VERIFY_EMOJI=✅
MC_SERVER_URL=mc.example.com:25565
```

### 5. Bot starten
```
python bot.py
```

### Features

- ✅ Rollenmanagement (Gast → Spieler nach Verifizierung)

- 👋 Willkommensnachricht für neue Mitglieder

- 📊 Server-Statistiken als Voice-Channels (Mitgliederzahl, Minecraft-Status)

- 🔄 Automatische Aktualisierung der Statistiken

### Dependencies

- [discord.py](https://pypi.org/project/discord.py/)
- [python-dotenv](https://pypi.org/project/python-dotenv/)
- [mcstatus](https://pypi.org/project/mcstatus/)

### Hinweise
- Stelle sicher, dass der Bot die richtigen Berechtigungen hat (Rollen verwalten, Nachrichten lesen/schreiben, Reaktionen hinzufügen).
- Halte dein ``.env`` lokal und privat – niemals in die Repo pushen!