// src/components/ServerStatus.jsx
import React, { useState, useEffect } from "react";
import PixelButton from "./PixelButton";
import "./styles/_serverStatus.scss";

// Farbcode-Mapping für Minecraft §-Codes
const colorMap = {
  "0": "#000000",
  "1": "#0000AA",
  "2": "#00AA00",
  "3": "#00AAAA",
  "4": "#AA0000",
  "5": "#AA00AA",
  "6": "#FFAA00",
  "7": "#AAAAAA",
  "8": "#555555",
  "9": "#5555FF",
  a: "#55FF55",
  b: "#55FFFF",
  c: "#FF5555",
  d: "#FF55FF",
  e: "#FFFF55",
  f: "#FFFFFF",
};

// Funktion: Minecraft §-Codes in React JSX parsen
function parseMinecraftText(text) {
  const parts = [];
  let currentColor = "";
  let buffer = "";

  for (let i = 0; i < text.length; i++) {
    if (text[i] === "§" && i + 1 < text.length) {
      if (buffer) {
        parts.push(
          <span key={parts.length} style={{ color: currentColor || undefined }}>
            {buffer}
          </span>
        );
        buffer = "";
      }
      const code = text[i + 1].toLowerCase();
      i++;
      if (colorMap[code]) currentColor = colorMap[code];
      else if (code === "r") currentColor = "";
    } else {
      buffer += text[i];
    }
  }

  if (buffer) {
    parts.push(
      <span key={parts.length} style={{ color: currentColor || undefined }}>
        {buffer}
      </span>
    );
  }

  return parts;
}

export default function ServerStatus({ serverIP = "helveticraft.com" }) {
  const [copied, setCopied] = useState(false);
  const [serverData, setServerData] = useState({
    online: false,
    players: 0,
    maxPlayers: 0,
    description: [], // MOTD zwei Zeilen
  });

  const copyIP = async () => {
    try {
      await navigator.clipboard.writeText(serverIP);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchServerStatus = async () => {
    try {
      const res = await fetch(`https://api.mcsrvstat.us/2/${serverIP}`);
      if (!res.ok) throw new Error("API nicht erreichbar");
      const data = await res.json();

      // nur die ersten zwei Zeilen aus motd.raw parsen
      const description = Array.isArray(data?.motd?.raw)
        ? data.motd.raw.slice(0, 2).map((line) => parseMinecraftText(line))
        : [];

      setServerData({
        online: data?.online || false,
        players: data?.players?.online ?? 0,
        maxPlayers: data?.players?.max ?? 0,
        description,
      });
    } catch (err) {
      console.error("Fehler beim Abrufen des Serverstatus", err);
      setServerData({ online: false, players: 0, maxPlayers: 0, description: [] });
    }
  };

  useEffect(() => {
    fetchServerStatus();
    const interval = setInterval(fetchServerStatus, 30000);
    return () => clearInterval(interval);
  }, [serverIP]);

  return (
    <div className="minecraft-panel server-status-card">
      <div className="server-status-inner">
        <img
          src="/imgs/pngs/logo.png"
          alt="HelvetiCraft"
          className="server-icon"
        />
        <div className="server-status-text">
          <div className="server-name">HelvetiCraft</div>

          <div className="server-info-row">
            <span className={serverData.online ? "status-online" : "status-offline"}>
              ● {serverData.online ? "Online" : "Offline"}
            </span>
            <span className="player-count">
              • {serverData.players} / {serverData.maxPlayers} Spieler
            </span>
          </div>

          <div className="server-description">
            {serverData.description.map((line, idx) => (
              <div key={idx} className="motd-line">
                {line}
              </div>
            ))}
          </div>

          <div className="server-ip-row">
            <span className="server-ip">{serverIP}</span>
            <PixelButton onClick={copyIP} size="small" className="copy-btn-small">
              {copied ? "Kopiert" : "Kopieren"}
            </PixelButton>
          </div>
        </div>
      </div>
    </div>
  );
}
