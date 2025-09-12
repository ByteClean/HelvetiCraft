# Projekt Struktur

## Projekt aufteilung
1. **Minecraft Server**
    - Community Plugins +
    - Eigene Plugins für den Wirtschaftssystem
2. **Discord Server & Discord Bot**
3. **Website**
    - Frontend (react)
4. **Backend** (node.js Express)
    - Zugriff von Minecraft, Discord & Website.


### Genaueres zu den Teilen

#### 1. Minecraft Server
Der Server bildet die zentrale Spielwelt, in der Spieler grundlegende Funktionen durch Community-Plugins nutzen können. Zusätzlich integrieren eigene Plugins Elemente des Schweizer Rechts- und Finanzsystems, insbesondere das Thema Volksinitiative, um das Projektziel zu unterstützen.
[Hier finden Sie mehr dazu](1.1.minecraftServer.md)

#### 2. Discord Server
Der Discord-Server dient als Community-Plattform, auf der Spieler miteinander kommunizieren, Neuigkeiten zum Projekt erfahren und ihre Minecraft-Konten mit Discord verknüpfen können. Er bietet Rollen, Regeln und strukturierte Kanäle für Interaktion und Organisation.
[Hier finden Sie mehr dazu](1.2.discordServer.md)

#### 3. Discord Bot
Der Bot automatisiert zentrale Funktionen wie Mitgliederverifizierung, Rollenzuweisung, Begrüßung neuer Nutzer und Serverstatistiken. Außerdem ermöglicht er die Nutzung von Discord-Funktionen in Verbindung mit dem Minecraft-Konto, z. B. Initiativen starten, eigene Finanzen verwalten oder Tickets erstellen.
[Hier finden Sie mehr dazu](1.3.discordBot.md)

#### 4. Website
Die React-basierte Website bietet Informationen, Statistiken und Interaktionsmöglichkeiten für Spieler. Sie greift auf das gemeinsame Backend zu und ermöglicht den Zugriff auf Profil- und Fortschrittsdaten.
[Hier finden Sie mehr dazu](1.4.website.md)

#### 5. Backend
Das Node.js/Express-Backend zentralisiert die Datenverwaltung für Minecraft, Discord und die Website. Es sorgt für Authentifizierung, synchronisiert alle Systemteile und verhindert redundante Datenhaltung.
[Hier finden Sie mehr dazu](1.5.backend.md)




