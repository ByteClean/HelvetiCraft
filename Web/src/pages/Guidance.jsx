// src/pages/Guidance.jsx
import React, { useState } from "react";
import "./styles/_guidance.scss";

export default function Guidance() {
  const [activeTab, setActiveTab] = useState("minecraft");

  return (
    <div className="page container guidance-page">
      <h2 className="guidance-title">Anleitung & Regelwerk</h2>

      {/* Tab-Auswahl */}
      <div className="launcher-select">
        <div className="buttons">
          <button
            className={activeTab === "minecraft" ? "active" : ""}
            onClick={() => setActiveTab("minecraft")}
          >
            Minecraft Launcher
          </button>

          <button
            className={activeTab === "tlauncher" ? "active" : ""}
            onClick={() => setActiveTab("tlauncher")}
          >
            TLauncher
          </button>

          <button
            className={activeTab === "regeln" ? "active" : ""}
            onClick={() => setActiveTab("regeln")}
          >
            Regelwerk
          </button>
        </div>
      </div>

      {/* Inhalte */}
      <div className="launcher-content">
        {/* ──────────────────────────────────────── */}
        {/* Minecraft Launcher */}
        {activeTab === "minecraft" && (
          <div className="minecraft-panel guidance-box">
            <h3>Minecraft kaufen</h3>
            <p>
              Um auf HelvetiCraft spielen zu können, benötigst du die
              <strong> Minecraft Java Edition</strong>.
            </p>
            <p>
              👉 Kaufen bei:{" "}
              <a
                href="https://www.minecraft.net/de-de/store/minecraft-java-bedrock-edition-pc"
                target="_blank"
                rel="noopener noreferrer"
              >
                minecraft.net
              </a>
            </p>
            <img src="/imgs/pngs/MC_buy.png" alt="Minecraft kaufen" />

            <h3>Offizieller Minecraft Launcher</h3>
            <ol>
              <li>
                Minecraft Launcher öffnen und auf{" "}
                <strong>Installationen</strong> klicken.
                <img
                  src="/imgs/pngs/MC_Launch_Instl.png"
                  alt="Installationen"
                />
              </li>
              <li>
                Auf <strong>Neue Installation</strong> klicken.
                <img
                  src="/imgs/pngs/MC_Launch_New_Instl.png"
                  alt="Neue Installation"
                />
              </li>
              <li>
                Namen vergeben und Version <strong>Release 1.21.8</strong>{" "}
                auswählen.
                <img
                  src="/imgs/pngs/MC_Launch_Instl_V.png"
                  alt="Version auswählen"
                />
              </li>
              <li>
                Installation speichern.
                <img
                  src="/imgs/pngs/MC_Launch_Save.png"
                  alt="Installation speichern"
                />
              </li>
              <li>
                Richtige Version auswählen und auf <strong>Spielen</strong>{" "}
                klicken.
                <img src="/imgs/pngs/MC_Launch_V.png" alt="Version starten" />
                <img
                  src="/imgs/pngs/MC_Launch_start.png"
                  alt="Spielen klicken"
                />
              </li>
              <li>
                Im Hauptmenü auf <strong>Multiplayer</strong> klicken.
                <img src="/imgs/pngs/MC_Multi.png" alt="Multiplayer" />
              </li>
              <li>
                Auf <strong>Server hinzufügen</strong> klicken.
                <img src="/imgs/pngs/MC_Add_Srv.png" alt="Server hinzufügen" />
              </li>
              <li>
                Serverdaten eingeben:
                <ul>
                  <li>
                    <strong>Name:</strong> HelvetiCraft
                  </li>
                  <li>
                    <strong>Adresse:</strong> <code>helveticraft.com</code>
                  </li>
                </ul>
                <img src="/imgs/pngs/MC_Srv_Adresse.png" alt="Server Adresse" />
              </li>
              <li>
                Server auswählen und <strong>Beitreten</strong>.
                <img src="/imgs/pngs/MC_Join.png" alt="Server beitreten" />
              </li>
              <li>
                Beim ersten Betreten Passwort setzen:
                <pre>/register DEIN_PASSWORT PASSWORT_BESTÄTIGEN</pre>
                <img src="/imgs/pngs/MC_register.png" alt="Register Befehl" />
              </li>
              <li>
                Danach mit dem gesetzten Passwort einloggen:
                <pre>/login DEIN_PASSWORT</pre>
                <img src="/imgs/pngs/MC_PW_Login.png" alt="Login Befehl" />
              </li>
            </ol>
          </div>
        )}

        {/* ──────────────────────────────────────── */}
        {/* TLauncher */}
        {activeTab === "tlauncher" && (
          <div className="minecraft-panel guidance-box">
            <h3>TLauncher</h3>
            <p>
              TLauncher ist ein alternativer Launcher. Wir übernehmen keinen
              Support für Account-Probleme.
            </p>
            <p>
              👉 Download:{" "}
              <a
                href="https://tlauncher.org"
                target="_blank"
                rel="noopener noreferrer"
              >
                tlauncher.org
              </a>
            </p>
            <ol>
              <li>
                TLauncher herunterladen.
                <img
                  src="/imgs/pngs/Tlauncher_DL.png"
                  alt="TLauncher Download"
                />
              </li>
              <li>
                Benutzername eingeben oder Konto erstellen.
                <img src="/imgs/pngs/Tlauncher_Name.png" alt="TLauncher Name" />
              </li>
              <li>
                Richtige Version auswählen.
                <img src="/imgs/pngs/Tlauncher_V.png" alt="TLauncher Version" />
              </li>
              <li>
                Auf <strong>Enter the Game</strong> klicken.
                <img
                  src="/imgs/pngs/Tlauncher_start.png"
                  alt="TLauncher Start"
                />
              </li>
              <li>
                Im Hauptmenü auf <strong>Multiplayer</strong> klicken.
                <img src="/imgs/pngs/MC_Multi.png" alt="Multiplayer" />
              </li>
              <li>
                Auf <strong>Server hinzufügen</strong> klicken.
                <img src="/imgs/pngs/MC_Add_Srv.png" alt="Server hinzufügen" />
              </li>
              <li>
                Serverdaten eingeben:
                <ul>
                  <li>
                    <strong>Name:</strong> HelvetiCraft
                  </li>
                  <li>
                    <strong>Adresse:</strong> <code>helveticraft.com</code>
                  </li>
                </ul>
                <img src="/imgs/pngs/MC_Srv_Adresse.png" alt="Server Adresse" />
              </li>
              <li>
                Server auswählen und <strong>Beitreten</strong>.
                <img src="/imgs/pngs/MC_Join.png" alt="Server beitreten" />
              </li>
              <li>
                Beim ersten Betreten Passwort setzen:
                <pre>/register DEIN_PASSWORT PASSWORT_BESTÄTIGEN</pre>
                <img src="/imgs/pngs/MC_register.png" alt="Register Befehl" />
              </li>
              <li>
                Danach mit dem gesetzten Passwort einloggen:
                <pre>/login DEIN_PASSWORT</pre>
                <img src="/imgs/pngs/MC_PW_Login.png" alt="Login Befehl" />
              </li>
            </ol>
          </div>
        )}

        {/* ──────────────────────────────────────── */}
        {/* Regelwerk */}
        {activeTab === "regeln" && (
          <div className="minecraft-panel guidance-box rules-panel">
            <h3>📜 Regelwerk – HelvetiCraft</h3>

            <p>Dieses Regelwerk gilt für alle Bereiche des Projekts:</p>
            <ul>
              <li>Minecraft-Server</li>
              <li>Discord-Server</li>
              <li>Website & verbundene Systeme</li>
            </ul>

            <p>
              <strong>
                Mit dem Beitritt zu HelvetiCraft erklärst du dich mit allen
                folgenden Regeln einverstanden.
              </strong>
            </p>

            <h4>1️⃣ Allgemeine Grundregeln</h4>
            <ol>
              <li>
                <strong>Respektvoller Umgang</strong>
                <ul>
                  <li>
                    Beleidigungen, Diskriminierung, Hassrede oder Provokationen
                    sind verboten.
                  </li>
                  <li>
                    Rassismus, Sexismus, Extremismus oder persönliche Angriffe
                    führen zu Sanktionen.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Fairplay</strong>
                <ul>
                  <li>
                    Cheating, Exploits, Bugs ausnutzen oder Modifikationen mit
                    Vorteil sind verboten.
                  </li>
                  <li>
                    Bugs müssen gemeldet und dürfen nicht absichtlich ausgenutzt
                    werden.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Anweisungen des Teams</strong>
                <ul>
                  <li>
                    Anweisungen von Administratoren und Moderatoren sind zu
                    befolgen.
                  </li>
                  <li>
                    Diskussionen über Entscheidungen können sachlich, aber nicht
                    im Streit geführt werden.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Mehrfachaccounts</strong>
                <ul>
                  <li>
                    Mehrere Accounts zur Vorteilsverschaffung sind verboten.
                  </li>
                  <li>
                    Discord- und Minecraft-Verknüpfungen dürfen nur für den
                    eigenen Account genutzt werden.
                  </li>
                </ul>
              </li>
            </ol>

            <h4>2️⃣ Minecraft-Server Regeln</h4>
            <h5>🧱 Bau- & Weltschutz</h5>
            <ol start="1">
              <li>
                <strong>Kein Griefing</strong>
                <ul>
                  <li>
                    Das Zerstören, Verändern oder Stehlen fremder Bauten ohne
                    Erlaubnis ist verboten.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Baustil & Umgebung</strong>
                <ul>
                  <li>
                    Unfertige, absichtlich hässliche oder störende Bauten können
                    entfernt werden.
                  </li>
                  <li>
                    Öffentliche Gebiete müssen sauber und sinnvoll bebaut
                    werden.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Landschutz</strong>
                <ul>
                  <li>
                    Grundstücke müssen über die vorgesehenen Systeme gesichert
                    werden.
                  </li>
                  <li>
                    Ungenutztes Land kann nach längerer Inaktivität freigegeben
                    werden.
                  </li>
                </ul>
              </li>
            </ol>

            <h5>⚙️ Gameplay & Wirtschaft</h5>
            <ol start="4">
              <li>
                <strong>Wirtschaftssystem</strong>
                <ul>
                  <li>
                    Das Wirtschaftssystem basiert auf spielinternen Regeln (z.
                    B. Währung, Steuern).
                  </li>
                  <li>Manipulation oder Umgehung des Systems ist verboten.</li>
                </ul>
              </li>
              <li>
                <strong>Handel</strong>
                <ul>
                  <li>
                    Scamming, Betrug oder Täuschung bei Handel ist untersagt.
                  </li>
                  <li>Verträge und Abmachungen sind einzuhalten.</li>
                </ul>
              </li>
              <li>
                <strong>Finanzen</strong>
                <ul>
                  <li>
                    Unberechtigter Zugriff auf fremde Konten oder Gelder ist
                    verboten.
                  </li>
                  <li>
                    Finanztransaktionen müssen über die vorgesehenen Systeme
                    erfolgen.
                  </li>
                </ul>
              </li>
            </ol>

            <h4>3️⃣ Initiativen & Demokratie-System</h4>
            <h5>🗳️ Initiativen</h5>
            <ol>
              <li>
                <strong>Erstellen von Initiativen</strong>
                <ul>
                  <li>
                    Initiativen müssen sachlich, verständlich und realistisch
                    formuliert sein.
                  </li>
                  <li>
                    Spam-Initiativen oder offensichtlich sinnlose Vorschläge
                    sind verboten.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Abstimmungen</strong>
                <ul>
                  <li>Jede Person stimmt nur einmal pro Abstimmung.</li>
                  <li>
                    Beeinflussung durch Zwang, Bestechung oder Manipulation ist
                    verboten.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Phasen</strong>
                <ul>
                  <li>Phase 1: Einreichen & Sammeln von Stimmen</li>
                  <li>Phase 2: Prüfung durch Administratoren</li>
                  <li>Phase 3: Volksabstimmung</li>
                  <li>Phase 4: Pause / Auswertung</li>
                </ul>
                <p>Entscheidungen sind nach Abschluss verbindlich.</p>
              </li>
              <li>
                <strong>Ergebnisse</strong>
                <ul>
                  <li>
                    Angenommene Initiativen werden umgesetzt, sofern technisch &
                    spielerisch möglich.
                  </li>
                  <li>
                    Abgelehnte Initiativen können nicht sofort erneut
                    eingereicht werden.
                  </li>
                </ul>
              </li>
            </ol>

            <h4>4️⃣ Discord-Server Regeln</h4>
            <ol>
              <li>
                <strong>Verhalten</strong>
                <ul>
                  <li>Die allgemeinen Grundregeln gelten auch auf Discord.</li>
                  <li>Spam, Flooding oder unnötiges Pingen ist verboten.</li>
                </ul>
              </li>
              <li>
                <strong>Kanäle</strong>
                <ul>
                  <li>Jeder Kanal hat ein Thema – halte dich daran.</li>
                  <li>
                    Off-Topic gehört ausschliesslich in dafür vorgesehene
                    Kanäle.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Verifikation</strong>
                <ul>
                  <li>
                    Neue Nutzer müssen die Regeln akzeptieren, um Zugriff zu
                    erhalten.
                  </li>
                  <li>Umgehung der Verifikation ist verboten.</li>
                </ul>
              </li>
              <li>
                <strong>Bot-Nutzung</strong>
                <ul>
                  <li>
                    Bots dürfen nicht missbraucht oder absichtlich überlastet
                    werden.
                  </li>
                  <li>Exploits oder Fehlfunktionen sind zu melden.</li>
                </ul>
              </li>
            </ol>

            <h4>5️⃣ Website & Account-Nutzung</h4>
            <ol>
              <li>
                <strong>Accounts</strong>
                <ul>
                  <li>Jeder Nutzer darf nur einen Account besitzen.</li>
                  <li>Account-Sharing ist verboten.</li>
                </ul>
              </li>
              <li>
                <strong>Inhalte</strong>
                <ul>
                  <li>
                    Beleidigende, irreführende oder illegale Inhalte sind
                    untersagt.
                  </li>
                  <li>
                    News, Kommentare oder Einträge dürfen nicht missbraucht
                    werden.
                  </li>
                </ul>
              </li>
              <li>
                <strong>Sicherheit</strong>
                <ul>
                  <li>Sicherheitslücken dürfen nicht ausgenutzt werden.</li>
                  <li>Verdächtige Aktivitäten sind dem Team zu melden.</li>
                </ul>
              </li>
            </ol>

            <h4>6️⃣ Sanktionen</h4>

            <p>
              Je nach Art und Schwere des Verstosses können – einzeln oder
              kombiniert – folgende Massnahmen verhängt werden:
            </p>

            <ul>
              <li>
                <strong>Verwarnung</strong> (mündlich oder schriftlich)
              </li>
              <li>
                <strong>Temporärer Mute</strong> (Chat-Sperre auf Discord oder
                im Spiel)
              </li>
              <li>
                <strong>Temporärer Kick</strong> (vorübergehender Rauswurf aus
                dem Server)
              </li>
              <li>
                <strong>Temporärer Bann</strong> (zeitlich befristete Sperre, z.
                B. 3 Tage – 3 Monate)
              </li>
              <li>
                <strong>Permanenter Bann</strong> (dauerhafter Ausschluss vom
                Minecraft-Server, Discord und/oder Website)
              </li>
              <li>
                <strong>Ausschluss von Initiativen & Abstimmungen</strong>{" "}
                (Verlust des Stimm- und Initiativrechts für eine bestimmte Zeit
                oder dauerhaft)
              </li>
              <li>
                <strong>Rücksetzung von Fortschritt / Finanzen</strong>{" "}
                (Löschung oder Zurücksetzung von Geld, Grundstücken, Items etc.)
              </li>
              <li>
                <strong>Weitere Massnahmen</strong> (z. B. Entfernung von Bauten,
                Konfiszierung von Gegenständen, Sperrung von Rechten im
                Wirtschaftssystem)
              </li>
            </ul>

            <p>
              <strong>Wichtig:</strong>
            </p>
            <ul>
              <li>
                Das Team entscheidet über das angemessene Strafmass – die
                Entscheidungen des Teams sind{" "}
                <strong>final und nicht verhandelbar</strong>.
              </li>
              <li>
                Das bewusste Ausnutzen von Grauzonen, Regel-Lücken oder unklaren
                Formulierungen wird wie ein direkter Regelverstoss behandelt.
              </li>
              <li>
                Bei Verhalten, das dem Geist des Regelwerks (Fairness, Respekt,
                Demokratie, Schweizer Werte) widerspricht, behält sich das Team
                vor, auch ohne exakte Regel-Nennung zu sanktionieren.
              </li>
              <li>
                Es gibt <strong>keine öffentliche Diskussion</strong> über
                Strafen – Reklamationen werden intern geprüft, aber das Urteil
                bleibt bestehen.
              </li>
            </ul>

            <p>
              <strong>Ziel der Sanktionen:</strong> Den Server fair, respektvoll
              und spassig halten – nicht als Strafe um der Strafe willen.
            </p>

            <h4>7️⃣ Änderungen am Regelwerk</h4>
            <ul>
              <li>
                Das Regelwerk kann jederzeit angepasst oder erweitert werden.
              </li>
              <li>
                Änderungen werden über die Website oder Discord bekannt gegeben.
              </li>
              <li>
                Mit weiterer Nutzung akzeptierst du automatisch die
                aktualisierten Regeln.
              </li>
            </ul>

            <h4>✅ Abschluss</h4>
            <p>HelvetiCraft soll:</p>
            <ul>
              <li>fair</li>
              <li>respektvoll</li>
              <li>lehrreich</li>
              <li>spielerisch demokratisch</li>
            </ul>
            <p>Danke, dass du Teil des Projekts bist 💚</p>
          </div>
        )}
      </div>
    </div>
  );
}
