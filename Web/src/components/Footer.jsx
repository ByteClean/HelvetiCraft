import React from 'react'

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div>
          © {new Date().getFullYear()} <strong>HelvetiCraft</strong> – Schweizer Demokratie-Server
        </div>
        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', justifyContent: 'center' }}>
          <a href="#">Impressum</a>
          <a href="#">Datenschutz</a>
          <a href="https://discord.gg/DEIN_LINK" target="_blank" rel="noopener">
            Discord
          </a>
        </div>
      </div>
    </footer>
  )
}