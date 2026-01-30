// src/pages/About.jsx
import React from "react";
import "./styles/_about.scss";

export default function About() {
  return (
    <div className="page container about-page">
      <h2>Über uns</h2>
      <p className="desc">
        HelvetiCraft wird von einem kleinen, engagierten Team entwickelt,
        das Technik, Gameplay und Organisation miteinander verbindet.
      </p>

      <div className="team-grid">
        <article className="team-card">
          <h3>Aram</h3>
          <p className="role">Frontend & Logik</p>
          <p className="info">
            18 Jahre alt. Hauptverantwortlich für die Frontend-Entwicklung
            und grosse Teile der Spiellogik. Zusätzlich Erbauer des Bundeshauses
            auf dem Server.
          </p>
        </article>

        <article className="team-card">
          <h3>Spyros</h3>
          <p className="role">Discord & Plugin-Entwicklung</p>
          <p className="info">
            18 Jahre alt. Zuständig für Discord-Integration und Plugin-Entwicklung.
            Unterstützt zudem bei der technischen Logik des Systems.
          </p>
        </article>

        <article className="team-card">
          <h3>Nicolas</h3>
          <p className="role">Backend & Middleware</p>
          <p className="info">
            17 Jahre alt. Verantwortlich für Backend- und Middleware-Entwicklung.
            Unterstützt sowohl bei der Logik als auch bei der Web-Integration.
          </p>
        </article>
      </div>
    </div>
  );
}
