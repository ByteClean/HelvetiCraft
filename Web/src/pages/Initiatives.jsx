// src/pages/Initiatives.jsx
import React from 'react'
import './styles/_initiatives.scss'

const MOCK = [
  { id: 1, title: "Steuersenkung für Spieler", desc: "Reduziert Steuern für kleine Betriebe.", yes: 62, no: 38, status: "In Abstimmung" },
  { id: 2, title: "Neues Gemeindehaus", desc: "Bau eines öffentlichen Gemeindehauses im Spawn.", yes: 85, no: 15, status: "Angenommen" },
  { id: 3, title: "Verbote von TNT", desc: "Strengere Regeln gegen Griefing.", yes: 44, no: 56, status: "Abgelehnt" },
]

function ProgressBar({ value }) {
  return (
    <div className="mc-progress">
      <div className="mc-progress-fill" style={{ width: `${value}%` }} />
      <div className="mc-progress-label">{value}% Ja</div>
    </div>
  )
}

export default function Initiatives() {
  return (
    <div className="page container initiatives-page">
      <h2>Initiativen</h2>
      <p className="desc">Reiche neue Initiativen ein oder schau dir aktuelle Abstimmungen an.</p>

      <div className="initiatives-grid">
        {MOCK.map(item => (
          <article key={item.id} className="initiative-card">
            <div className="card-head">
              <img src="/imgs/items/diamond.png" alt="" />
              <div>
                <h3>{item.title}</h3>
                <div className={`status ${item.status === "Angenommen" ? 'accepted' : item.status === "Abgelehnt" ? 'rejected' : 'voting'}`}>{item.status}</div>
              </div>
            </div>

            <p className="card-desc">{item.desc}</p>

            <div className="card-footer">
              <ProgressBar value={item.yes} />
              <div className="vote-meta">
                <span>Ja: {item.yes}%</span> <span>Nein: {item.no}%</span>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
