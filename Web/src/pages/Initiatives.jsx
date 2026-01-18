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
    case 0: return "Phase 1: Aktive Initiativen";
    case 1: return "Phase 2: Bearbeitung durch Admins";
    case 2: return "Phase 3: Volksabstimmung";
    case 3: return "Phase 4: Pause";
    default: return "Unbekannte Phase";
  }
}

function getCurrentPhase(phaseData) {
  const now = new Date();
  if (now >= new Date(phaseData.start_phase3)) return 3;
  if (now >= new Date(phaseData.start_phase2)) return 2;
  if (now >= new Date(phaseData.start_phase1)) return 1;
  return 0;
}

function calcPct(ja, nein) {
  const j = Number(ja) || 0;
  const n = Number(nein) || 0;
  const total = j + n;
  if (!total) return { jaPct: 0, neinPct: 0 };
  const jaPct = Math.round((j / total) * 100);
  return { jaPct, neinPct: 100 - jaPct };
}

export default function Initiatives() {
  const [initiatives, setInitiatives] = useState([]);
  const [phase, setPhase] = useState({ id: 0, label: "" });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const [iRes, pRes] = await Promise.all([
        fetch("https://helveticraft.com/api/initiatives/"),
        fetch("https://helveticraft.com/api/phases/current"),
      ]);

      const iData = await iRes.json();
      const pData = await pRes.json();

      const phaseId = getCurrentPhase(pData);
      setPhase({ id: phaseId, label: phaseLabel(phaseId) });
      setInitiatives(iData);
      setLoading(false);
    })();
  }, []);

  const mapped = useMemo(() => initiatives.map(i => {
    const ja = Number(i?.stimmen?.ja ?? 0);
    const nein = Number(i?.stimmen?.nein ?? 0);
    const normal = Number(i?.stimmen?.normal ?? 0);
    const { jaPct, neinPct } = calcPct(ja, nein);

    return { ...i, _ui: { ja, nein, normal, jaPct, neinPct } };
  }), [initiatives]);

  if (loading) return <p>Lade Initiativen…</p>;

  return (
    <div className="page container initiatives-page">
      <h2>Initiativen</h2>
      <p className="desc">Reiche neue Initiativen ein oder beteilige dich an Abstimmungen.</p>

      <div className={`current-phase phase-${phase.id}`}>
        {phase.label}
      </div>

      {/* PHASE 2 & 4 – NUR TEXT */}
      {(phase.id === 1 || phase.id === 3) && (
        <div className="phase-message">
          {phase.id === 1
            ? "Die Initiativen werden aktuell von der Administration bearbeitet."
            : "Aktuell befindet sich das System in einer Pause."}
        </div>
      )}

      {/* PHASE 1 & 3 – INITIATIVEN */}
      {(phase.id === 0 || phase.id === 2) && (
        <div className="initiatives-grid">
          {mapped.map(item => (
            <article key={item.id} className="initiative-card">
              <h3>{item.title}</h3>
              <p className="card-desc">{item.description}</p>

              <div className="card-footer">
                {phase.id === 0 && (
                  <div className="normal-votes">
                    Unterstützungen: <strong>{item._ui.normal}</strong>
                  </div>
                )}

                {phase.id === 2 && (
                  <>
                    <ProgressBar value={item._ui.jaPct} />
                    <div className="final-stats">
                      <span className="yes">Ja: {item._ui.ja}</span>
                      <span className="no">Nein: {item._ui.nein}</span>
                    </div>
                  </>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
