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

## Slash commands (Anwendungsbefehle)

- Die Bot-Instanz registriert die Guild-scoped Slash-/Application-Commands automatisch beim Start. Das heißt: beim Starten von `python bot.py` wird der Bot versuchen, die definierten Befehle in `commands.py` für die konfigurierte Guild (`GUILD_ID`) per REST an Discord zu schreiben.
- Änderungen an den in `commands.py` definierten Befehlen werden beim nächsten Neustart des Bots auf die Guild angewendet.

Troubleshooting:

- Falls neue Befehle nicht sofort im Discord-Client erscheinen, lade den Client neu (Desktop: `Ctrl+R`) oder prüfe die Web-Version (https://discord.com/app). Client-seitiges Caching kann die Anzeige verzögern.
- Stelle sicher, dass dein Benutzer die Berechtigung "Use Application Commands" hat (Server Settings → Rollen → Use Application Commands), sonst werden Befehle für dich nicht sichtbar/ausführbar.
- Achte darauf, dass nur eine Bot-Instanz mit demselben Token gleichzeitig laufen sollte, da konkurrierende Prozesse Commands überschreiben können.

Entwicklerhinweis

- Der Code registriert die Befehle beim `on_ready`-Event (siehe `Discord_Bot/events.py`). Dort wird die Bot-Instanz den REST-Aufruf machen, um die Guild-Befehle zu erstellen/überschreiben und danach die lokale CommandTree-Synchronisation auszuführen.
- Wenn du die automatische Registrierung deaktivieren möchtest, kannst du temporär die Registrierung im `events.on_ready`-Block auskommentieren oder den Guard `_commands_registered` setzen. Falls du eine konfigurierbare Schalter-Variable bevorzugst (z. B. `DISABLE_AUTO_REGISTER`), kann ich das gerne hinzufügen.

Entwicklung & Debugging

- Für lokale Tests wurden während der Entwicklung kurze Debug-Skripte genutzt, die manuell REST-Calls an Discord senden konnten. Diese Debug-Helper wurden aus dem Produktiv-Code entfernt, da der Bot die Registrierung nun selbst vornimmt.
- Wenn du Probleme bei der Installation oder beim Starten hast, überprüfe zuerst die `.env`-Werte und die installierten Abhängigkeiten per:

```powershell
pip install -r requirements.txt
python .\Discord_Bot\bot.py
```

Wenn du möchtest, füge ich auf Wunsch noch ein optionales Konfigurations-Flag hinzu, um die automatische Registrierung zu steuern (bspw. `DISABLE_AUTO_REGISTER=true`).