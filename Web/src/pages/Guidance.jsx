// src/pages/Guidance.jsx
import React, { useState } from "react";
import "./styles/_guidance.scss";

export default function Guidance() {
  const [launcher, setLauncher] = useState("minecraft");

  return (
    <div className="page container guidance-page">
      <h2 className="guidance-title">Anleitung</h2>

      {/* Launcher-Auswahl */}
      <div className="launcher-select">
        <div className="buttons">
          <button
            className={launcher === "minecraft" ? "active" : ""}
            onClick={() => setLauncher("minecraft")}
          >
            Minecraft Launcher
          </button>
          <button
            className={launcher === "tlauncher" ? "active" : ""}
            onClick={() => setLauncher("tlauncher")}
          >
            TLauncher
          </button>
        </div>
      </div>

      {/* Minecraft Launcher Inhalt */}
      {launcher === "minecraft" && (
        <div className="launcher-content">
          {/* Minecraft kaufen */}
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
          </div>

          {/* Offizieller Launcher */}
          <div className="minecraft-panel guidance-box">
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
                    Name: <strong>HelvetiCraft</strong>
                  </li>
                  <li>
                    Adresse: <code>helveticraft.com</code>
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
        </div>
      )}

      {/* TLauncher Inhalt */}
      {launcher === "tlauncher" && (
        <div className="launcher-content">
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
                    Name: <strong>HelvetiCraft</strong>
                  </li>
                  <li>
                    Adresse: <code>helveticraft.com</code>
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
        </div>
      )}
    </div>
  );
}
