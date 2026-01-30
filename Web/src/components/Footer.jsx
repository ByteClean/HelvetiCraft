// src/components/Footer.jsx
import React, { useState } from 'react';

export default function Footer() {
  const currentYear = new Date().getFullYear();

  const [activeModal, setActiveModal] = useState(null); // null | 'impressum' | 'datenschutz'

  return (
    <>
      <footer className="site-footer">
        <div className="footer-inner">
          <div className="footer-copyright">
            © {currentYear} <strong>HelvetiCraft</strong> – Alle Rechte vorbehalten.
          </div>

          <div className="footer-links">
            <button
              type="button"
              onClick={() => setActiveModal('impressum')}
              className="footer-link-btn"
            >
              Impressum
            </button>

            <button
              type="button"
              onClick={() => setActiveModal('datenschutz')}
              className="footer-link-btn"
            >
              Datenschutz
            </button>

            <a
              href="https://discord.gg/q2mMrXad9h"
              target="_blank"
              rel="noopener noreferrer"
            >
              Discord
            </a>
          </div>
        </div>
      </footer>

      {/* Modals */}
      {activeModal && (
        <div
          className="modal-overlay"
          onClick={() => setActiveModal(null)} // Klick ausserhalb schliesst
        >
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()} // verhindert Schliessen beim Klick ins Modal
          >
            <button
              className="modal-close-btn"
              onClick={() => setActiveModal(null)}
            >
              ×
            </button>

            {activeModal === 'impressum' && (
              <div className="modal-body">
                <h2>Impressum</h2>

                <p>
                  <strong>Betreiber:</strong><br />
                  Aram, Spyros, Nicolas<br />
                  Bern<br />
                  Schweiz
                </p>

                <p>
                  <strong>Kontakt:</strong><br />
                  Discord: <a href="https://discord.gg/q2mMrXad9h" target="_blank" rel="noopener">HelvetiCraft</a>
                </p>

                <p>
                  <strong>Haftungsausschluss:</strong><br />
                  HelvetiCraft ist ein Schulprojekt. Jegliche Nutzung erfolgt auf eigene Gefahr. 
                  Wir übernehmen keine Haftung für Schäden durch Servernutzung, Bugs oder externe Links.
                </p>

                <p className="small-note">Stand: Januar 2026</p>
              </div>
            )}

            {activeModal === 'datenschutz' && (
              <div className="modal-body">
                <h2>Datenschutzerklärung</h2>

                <p>
                  <strong>Verantwortliche:</strong><br />
                  Aram, Bern, Schweiz<br />
                  aram.j.amir@proton.me<br /><br />
                  
                  Spyros, Bern, Schweiz<br />
                  spyroscatechis@proton.me<br /><br />
                  
                  Nicolas, Bern, Schweiz<br />
                  nicolas.ammeter@proton.me
                </p>

                <p><strong>Erhobene Daten:</strong></p>
                <ul>
                  <li>Minecraft-Benutzername</li>
                  <li>IP-Adresse (Server & Website)</li>
                  <li>Chat-Nachrichten & Spielaktionen</li>
                  <li>Discord-ID (bei Verknüpfung)</li>
                </ul>

                <p><strong>Zweck:</strong> Betrieb des Servers, Moderation, Community-Funktionen.</p>

                <p><strong>Speicherdauer:</strong> Solange Account aktiv + max. 90 Tage für Logs.</p>

                <p>
                  <strong>Rechte:</strong> Auskunft, Löschung etc. per E-Mail an uns.
                </p>

                <p className="small-note">Stand: Januar 2026 – gemäss revDSG (Schweiz)</p>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}