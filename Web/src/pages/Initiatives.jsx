// src/pages/Initiatives.jsx
import React, { useEffect, useState } from "react";
import "./styles/_initiatives.scss";

function ProgressBar({ value }) {
  return (
    <div className="mc-progress">
      <div className="mc-progress-fill" style={{ width: `${value}%` }} />
    </div>
  );
}

export default function Initiatives() {
  const [initiatives, setInitiatives] = useState([]);
  const [phase, setPhase] = useState({ id: 0, label: "" });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Initiativen + Phase vom Backend laden
    const fetchData = async () => {
      try {
        const resInitiatives = await fetch("/initiatives");
        const dataInitiatives = await resInitiatives.json();

        // Optional: aktuelle Phase vom Backend holen
        const resPhase = await fetch("/phases/current");
        const dataPhase = await resPhase.json();

        setInitiatives(dataInitiatives);
        setPhase(dataPhase);
        setLoading(false);
      } catch (err) {
        console.error("Fehler beim Laden der Initiativen:", err);
      }
    };

    fetchData();
  }, []);

  if (loading) return <p>Lade Initiativen…</p>;

  return (
    <div className="page container initiatives-page">
      <h2>Initiativen</h2>
      <p className="desc">
        Reiche neue Initiativen ein oder schau dir aktuelle Abstimmungen an.
      </p>

      {/* Anzeige der aktuellen Phase */}
      <div className={`current-phase phase-${phase.id}`}>
        {phase.label}
      </div>

      <div className="initiatives-grid">
        {initiatives.map((item) => {
          // Status-Klasse für Styling
          const statusClass =
            item.status === "Angenommen"
              ? "accepted"
              : item.status === "Abgelehnt"
              ? "rejected"
              : "voting";

          return (
            <article key={item.id} className="initiative-card">
              <div className="card-head">
                <div>
                  <h3>{item.title}</h3>
                  <div className={`status ${statusClass}`}>{item.status}</div>
                </div>
              </div>

              <p className="card-desc">{item.description}</p>

              <div className="card-footer">
                <ProgressBar value={item.stimmen.ja} />
                <div className="vote-meta">
                  <span>Ja: {item.stimmen.ja}%</span>
                  <span>Nein: {item.stimmen.nein}%</span>
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
}
