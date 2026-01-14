// src/pages/Initiatives.jsx
import React, { useEffect, useMemo, useState } from "react";
import "./styles/_initiatives.scss";

function ProgressBar({ value }) {
  const safe = Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
  return (
    <div className="mc-progress">
      <div className="mc-progress-fill" style={{ width: `${safe}%` }} />
    </div>
  );
}

function phaseLabel(phaseNumber) {
  switch (phaseNumber) {
    case 0:
      return "Phase 0: Normales Voting";
    case 1:
      return "Phase 1: Admin Finalvote";
    case 2:
      return "Phase 2: Spieler Finalvote";
    case 3:
      return "Phase 3: Angenommen (Ende)";
    default:
      return `Phase ${phaseNumber ?? "?"}`;
  }
}

function calcPct(ja, nein) {
  const j = Number(ja) || 0;
  const n = Number(nein) || 0;
  const total = j + n;
  if (total <= 0) return { jaPct: 0, neinPct: 0, total: 0 };
  const jaPct = Math.round((j / total) * 100);
  const neinPct = 100 - jaPct;
  return { jaPct, neinPct, total };
}

export default function Initiatives() {
  const [initiatives, setInitiatives] = useState([]);
  const [phase, setPhase] = useState({ id: 0, label: "Phase 0: Normales Voting" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError("");

        const [resInitiatives, resPhase] = await Promise.all([
          fetch("http://localhost:3000/initiatives/"),
          fetch("http://localhost:3000/phases/current"),
        ]);

        if (!resInitiatives.ok) {
          throw new Error(`initiatives_http_${resInitiatives.status}`);
        }
        if (!resPhase.ok) {
          throw new Error(`phases_http_${resPhase.status}`);
        }

        const dataInitiatives = await resInitiatives.json();
        const dataPhase = await resPhase.json();

        // phases/current liefert bei dir typischerweise { phase: 0..3, ... }
        const phaseId = Number(dataPhase.phase ?? dataPhase.id ?? 0);
        setPhase({
          id: phaseId,
          label: phaseLabel(phaseId),
          raw: dataPhase,
        });

        setInitiatives(Array.isArray(dataInitiatives) ? dataInitiatives : []);
      } catch (err) {
        console.error("Fehler beim Laden:", err);
        setError(String(err?.message || err));
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const mapped = useMemo(() => {
    return initiatives.map((item) => {
      const ja = item?.stimmen?.ja ?? 0;
      const nein = item?.stimmen?.nein ?? 0;
      const normal = item?.stimmen?.normal ?? 0;

      const { jaPct, neinPct, total } = calcPct(ja, nein);

      // Styling: status ist bei dir numerisch (0..3)
      // 0=Voting, 1=Admin, 2=Spieler, 3=Angenommen (oder dein eigenes Mapping)
      const statusNum = Number(item.status);
      const statusText =
        statusNum === 3
          ? "Angenommen"
          : statusNum === 2
          ? "Finalvote (Spieler)"
          : statusNum === 1
          ? "Finalvote (Admin)"
          : "Voting";

      const statusClass =
        statusText === "Angenommen"
          ? "accepted"
          : statusText.includes("Finalvote")
          ? "voting"
          : "voting";

      return {
        ...item,
        _ui: {
          ja,
          nein,
          normal,
          jaPct,
          neinPct,
          totalFinal: total,
          statusText,
          statusClass,
        },
      };
    });
  }, [initiatives]);

  if (loading) return <p>Lade Initiativen…</p>;

  if (error) {
    return (
      <div className="page container initiatives-page">
        <h2>Initiativen</h2>
        <p className="desc">Fehler beim Laden: {error}</p>
      </div>
    );
  }

  return (
    <div className="page container initiatives-page">
      <h2>Initiativen</h2>
      <p className="desc">
        Reiche neue Initiativen ein oder schau dir aktuelle Abstimmungen an.
      </p>

      {/* Anzeige der aktuellen Phase */}
      <div className={`current-phase phase-${phase.id}`}>{phase.label}</div>

      <div className="initiatives-grid">
        {mapped.map((item) => (
          <article key={item.id} className="initiative-card">
            <div className="card-head">
              <div>
                <h3>{item.title}</h3>
                <div className={`status ${item._ui.statusClass}`}>
                  {item._ui.statusText}
                </div>
              </div>
            </div>

            <p className="card-desc">{item.description}</p>

            <div className="card-footer">
              {/* Finalvote-Anzeige als Prozent (ja/nein) */}
              <ProgressBar value={item._ui.jaPct} />
              <div className="vote-meta">
                <span>Ja: {item._ui.jaPct}% ({item._ui.ja})</span>
                <span>Nein: {item._ui.neinPct}% ({item._ui.nein})</span>
              </div>

              {/* Normale Votes (Phase 0) sind COUNT, nicht Prozent */}
              <div className="vote-meta" style={{ marginTop: 6 }}>
                <span>Normale Votes: {item._ui.normal}</span>
                <span>Finalvotes total: {item._ui.totalFinal}</span>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
