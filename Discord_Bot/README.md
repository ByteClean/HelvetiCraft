# HelvetiCraft Discord Bot

Ein Discord-Bot für den HelvetiCraft Server.  
Er verwaltet Rollen, sendet Willkommensnachrichten, zeigt Live-Statistiken wie Mitgliederzahl und Minecraft-Serverstatus an und unterstützt das neue **Initiativen-System**.

---

## Setup

### 1. Repository klonen

`git clone https://github.com/ByteClean/HelvetiCraft.git cd HelvetiCraft/Discord_Bot`

### 2. Virtuelle Umgebung erstellen (empfohlen)

`python -m venv venv source venv/bin/activate   # Linux/macOS venv\Scripts\activate      # Windows`

### 3. Abhängigkeiten installieren

`pip install -r requirements.txt`

### 4. Bot starten

`python bot.py`

---

## Features

### Basisfunktionen

- ✅ Rollenmanagement (Gast → Spieler nach Verifizierung)
    
- 👋 Willkommensnachricht für neue Mitglieder
    
- 📊 Server-Statistiken als Voice-Channels (Mitgliederzahl, Minecraft-Status)
    
- 🔄 Automatische Aktualisierung der Statistiken
    

### Slash Commands

- Die Bot-Instanz registriert automatisch Guild-scoped Slash-/Application-Commands (`commands.py`) beim Start. Änderungen werden beim nächsten Neustart übernommen.
    

---

## Verify-Funktion

- Neue Mitglieder erhalten zunächst die Rolle **Gast**.
    
- Durch Reagieren auf die Regeln können sie bestätigen, dass sie die Regeln gelesen haben, und erhalten dann die Rolle **Spieler (verifiziert)**.
    
    - Erst danach können sie normal auf dem Discord-Server interagieren.
        
- Der Slash-Command `/verify` dient zur **Verknüpfung des Minecraft- und Discord-Kontos**.
    
    - Er ist notwendig, damit andere Discord-Befehle und Funktionen korrekt genutzt werden können.

---

## Initiativen-System

Der Bot unterstützt ein zweistufiges Initiativen-System mit Discord-Buttons:

1. **Volksinitiative (Schritt 1)**
    
    - Jeder Spieler kann über den Button **"Unterschreiben"** eine Initiative unterstützen.
        
    - Signaturen werden an das Backend gesendet.
        
    - Das Embed zeigt die aktuelle Signaturanzahl.
        
2. **Running Initiative (Schritt 2)**
    
    - Nach Freigabe durch die Admins wird die Initiative erneut gepostet.
        
    - Spieler können über Buttons **"Für"** oder **"Gegen"** abstimmen.
        
    - Stimmen werden an das Backend gesendet und die Embed-Fußzeile wird aktualisiert.
        
3. **Finalisierung**
    
    - Nach Abschluss der Abstimmung wird die Initiative in **akzeptierte** oder **abgelehnte** Channels verschoben.
        
    - Der Name des Initiativen-Erstellers wird in diesen Embeds angezeigt.
        

---

## News-System

- Administratoren oder Bots können neue News posten.
    
- News bestehen aus:
    
    - Titel
        
    - Textinhalt
        
    - Autor
        
    - Optional: Bild
        
- Nachrichten werden als Embed in den `ankündigung`-Channel gepostet.
    
- Backend kann über Webhook die IDs aller aktuellen News abrufen oder einzelne Nachrichten löschen.
    

### Beispiel: Neue News erstellen

```
curl -X POST http://127.0.0.1:8081/news-create \
     -H "Content-Type: application/json" \
     -d '{
           "title": "Server Update 🚀",
           "content": "Wir haben ein neues Feature eingeführt! Bitte schaut es euch an.",
           "author": "Admin Team",
           "image_url": "https://example.com/image.png"
         }'

```

### Beispiel: Alle News abrufen

```
curl http://127.0.0.1:8081/news-list

```

Antwort:

```
{
  "news_posts": [
    {"id": 123456789012345678, "title": "Server Update 🚀"},
    {"id": 123456789012345679, "title": "Weekly Event"}
  ]
}

```

### Beispiel: News löschen

```
curl -X POST http://127.0.0.1:8081/news-delete \
     -H "Content-Type: application/json" \
     -d '{ "message_id": 123456789012345678 }'

```

---

## Dummy-Webhooks für Initiativen-Tests

1️⃣ Neue Initiative erstellen (Schritt 1):

```
curl -X POST http://127.0.0.1:8081/initiative-webhook \
     -H "Content-Type: application/json" \
     -d '{ "id": 43, "step": 1, "title": "Build a Redstone Tower", "description": "A massive tower to show off our redstone skills!", "owner_discord": "RedstoneMaster#1234", "owner_minecraft": "RSBuilder" }'

```

2️⃣ Signaturen aktualisieren (Schritt 1):

```
curl -X POST http://127.0.0.1:8081/initiative-votes-update \
     -H "Content-Type: application/json" \
     -d '{ "id": 43, "step": 1, "signatures": 15 }'

```

3️⃣ Initiative promoten auf Schritt 2:

```
curl -X POST http://127.0.0.1:8081/initiative-change \
     -H "Content-Type: application/json" \
     -d '{ "id": 43, "action": "promote", "title": "Build a Redstone Tower", "description": "Promoted after signatures and admin verification.", "owner_discord": "RedstoneMaster#1234", "owner_minecraft": "RSBuilder" }'

```

4️⃣ Stimmen aktualisieren (Schritt 2):

```
curl -X POST http://127.0.0.1:8081/initiative-votes-update \
     -H "Content-Type: application/json" \
     -d '{ "id": 43, "step": 2, "votes_for": 120, "votes_against": 30 }'

```

5️⃣ Initiative finalisieren – akzeptiert:

```
curl -X POST http://127.0.0.1:8081/initiative-change \
     -H "Content-Type: application/json" \
     -d '{ "id": 43, "action": "finalize", "result": "accepted", "title": "Build a Redstone Tower", "description": "Accepted by the council.", "owner_discord": "RedstoneMaster#1234", "owner_minecraft": "RSBuilder" }'

```

6️⃣ Initiative finalisieren – abgelehnt:

```
curl -X POST http://127.0.0.1:8081/initiative-change \
     -H "Content-Type: application/json" \
     -d '{ "id": 43, "action": "finalize", "result": "rejected", "title": "Build a Redstone Tower", "description": "Rejected after review.", "owner_discord": "RedstoneMaster#1234", "owner_minecraft": "RSBuilder" }'

```

---

## Admin-Rollenänderungen Logging

Der Bot protokolliert auch Änderungen von Admin-Rechten, die über den Webhook-Endpunkt gemeldet werden.

Beispiel für eine Admin-Rollenänderung loggen:

```
curl -X POST http://127.0.0.1:8081/made-admin \
     -H "Content-Type: application/json" \
     -d '{
           "username": "UserName#1234",
           "minecraft_name": "MCPlayer",
           "previous_role": "Moderator",
           "new_role": "Administrator",
           "duration": "24 Stunden",
           "reason": "Wartungsarbeiten am Server"
         }'
```

Dies erstellt eine Benachrichtigung im konfigurierten Admin-Log-Channel mit allen relevanten Informationen zur Rollenänderung.

---

## Hinweise

- Stelle sicher, dass der Bot die richtigen Berechtigungen hat:
    
    - Rollen verwalten
        
    - Nachrichten lesen/schreiben
        
    - Reaktionen hinzufügen
        
- Nur eine Bot-Instanz pro Token gleichzeitig starten, um Command-Registrierungskonflikte zu vermeiden.
    
- Für das News- und Initiativen-System muss der Bot Schreibrechte im jeweiligen Channel haben.