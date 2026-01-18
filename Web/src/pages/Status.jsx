// src/pages/Status.jsx
import React from "react";
import ServerStatus from "../components/ServerStatus";
import "./styles/_status.scss";

export default function Status() {
  return (
    <div className="page container status-page">
      <h2>Status</h2>
      <p className="desc">
        Hier siehst du den aktuellen Status des Minecraft-Servers sowie die
        Live-Karte der Welt.
      </p>

      {/* SERVER STATUS */}
      <section className="status-section">
        <h3>Serverstatus</h3>

        {/* ServerStatus bringt sein eigenes Panel mit */}
        <ServerStatus serverIP="helveticraft.com" />
      </section>

      {/* PL3XMAP */}
      <section className="status-section">
        <h3>Live-Karte (Pl3xMap)</h3>

        <div className="map-panel">
          <iframe
            title="HelvetiCraft Live Map"
            src="https://map.helveticraft.com"
            loading="lazy"
            referrerPolicy="no-referrer"
          />
        </div>
      </section>
    </div>
  );
}
