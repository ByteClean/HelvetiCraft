// src/pages/Home.jsx
import React, { useState } from "react";
import PixelButton from "../components/PixelButton";

export default function Home() {
  const [copied, setCopied] = useState(false);
  const serverIP = "helveticraft.com";

  const copyIP = async () => {
    try {
      await navigator.clipboard.writeText(serverIP);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="page container home-page">
      <section className="hero">
        <div className="hero-content">
          {/* Linke Seite – Text & Buttons */}
          <div className="hero-left">
            <h1 className="hero-title">HelvetiCraft</h1>
            <p className="hero-subtitle">
              Demokratie & Wirtschaft spielerisch erleben
            </p>

            <p className="hero-desc">
              HelvetiCraft ist ein Ecosystem das spielerisch das Schweizer
              Wirtschaftssystem widerspiegelt, indem der Nutzer gewisse
              Wirtschaftsobjekte wie Initiativen, Landkauf, Nachfrage & Angebot,
              Konjunktur und Einzelfirmen auf unseren Minecraft Server
              simulieren kann.
            </p>

            <div className="hero-ctas">
              <PixelButton onClick={copyIP} size="large">
                {copied ? "IP kopiert!" : "Minecraft beitreten"}
              </PixelButton>
              <PixelButton
                as="a"
                href="https://discord.gg/q2mMrXad9h"
                target="_blank"
                rel="noopener"
                className="outline"
              >
                Discord beitreten
              </PixelButton>
            </div>

            {/* Server-Status wie im echten Minecraft */}
            <div className="minecraft-panel server-status-card">
              <div className="server-status-inner">
                <img
                  src="/imgs/items/diamond.png"
                  alt="Server"
                  className="server-icon"
                />
                <div>
                  <div className="server-name">HelvetiCraft</div>
                  <div className="server-ip-row">
                    <span className="server-ip">{serverIP}</span>
                    <button onClick={copyIP} className="copy-btn-small">
                      {copied ? "Kopiert" : "Kopieren"}
                    </button>
                  </div>
                  <div>
                    <span className="status-online">● Online</span> •
                    <strong> 12 / 100 Spieler</strong> • Ping:{" "}
                    <span className="ping">22 ms</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Rechte Seite – Weltbild mit Panel-Rahmen */}
          <div className="hero-right">
            <div className="world-preview">
              <img src="/imgs/pngs/logo.png" alt="HelvetiCraft" />
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
