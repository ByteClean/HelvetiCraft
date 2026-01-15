export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div>
          © {new Date().getFullYear()} <strong>HelvetiCraft</strong>
        </div>

        <div className="footer-links">
          <a href="#">Impressum</a>
          <a href="#">Datenschutz</a>
          <a href="https://discord.gg/q2mMrXad9h" target="_blank" rel="noopener">
            Discord
          </a>
        </div>
      </div>
    </footer>
  )
}
