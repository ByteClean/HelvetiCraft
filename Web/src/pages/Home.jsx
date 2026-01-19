// src/pages/Home.jsx
import React, { useState } from "react";
import PixelButton from "../components/PixelButton";
import ServerStatus from "../components/ServerStatus";

export default function Home() {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard
      .writeText("helveticraft.com")
      .then(() => {
        setCopied(true);
        // nach 2,5 Sekunden zurücksetzen
        setTimeout(() => setCopied(false), 2500);
      })
      .catch((err) => {
        console.error("Fehler beim Kopieren:", err);
        // Optional: hier könntest du eine Fehlermeldung anzeigen
        // z. B. alert("Kopieren fehlgeschlagen – bitte manuell kopieren");
      });
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
              <PixelButton
                size="large"
                onClick={handleCopy}
                className="copy-ip-button"
              >
                {copied ? "IP kopiert" : "Minecraft beitreten"}
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

            {/* Serverstatus */}
            <ServerStatus serverIP="helveticraft.com" />
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
