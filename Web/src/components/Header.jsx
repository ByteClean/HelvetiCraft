import React from 'react'
import PixelButton from './PixelButton'

export default function Header() {
  return (
    <header className="site-header">
      <div className="header-inner">

        {/* Logo + Titel */}
        <div className="brand">
          <img
            src="/imgs/items/diamond.png"
            alt="HelvetiCraft"
            className="brand-icon"
          />
          <div className="brand-text">
            <h1>HelvetiCraft</h1>
            <small>Schweizer Minecraft</small>
          </div>
        </div>

        {/* Navigation */}
        <nav className="nav">
          <PixelButton as="a" href="/">Home</PixelButton>
          <PixelButton as="a" href="/initiatives">Initiativen</PixelButton>
          <PixelButton as="a" href="/features">Features</PixelButton>
          <PixelButton as="a" href="/news">News</PixelButton>
          <PixelButton as="a" href="/about">Über uns</PixelButton>
          <PixelButton as="a" href="/status">Status</PixelButton>
        </nav>

      </div>
    </header>
  )
}
