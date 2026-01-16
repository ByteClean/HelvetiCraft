// src/components/Header.jsx
import React from 'react'
import PixelButton from './PixelButton'

export default function Header() {
  return (
    <header className="site-header">
      <div className="header-inner">
        {/* Nur Titel – zentriert */}
        <div className="brand">
          <div className="brand-text">
            <h1>HelvetiCraft</h1>
          </div>
        </div>

        {/* Navigation */}
        <nav className="nav">
          <PixelButton as="a" href="/">Home</PixelButton>
          <PixelButton as="a" href="/initiatives">Initiativen</PixelButton>
          <PixelButton as="a" href="/guidance">Anleitung</PixelButton>
          <PixelButton as="a" href="/news">News</PixelButton>
          <PixelButton as="a" href="/about">Über uns</PixelButton>
          <PixelButton as="a" href="/status">Status</PixelButton>
        </nav>
      </div>
    </header>
  )
}
