// src/components/Header.jsx
import React from 'react'
import PixelButton from './PixelButton'
import { useNavigate } from 'react-router-dom'

export default function Header() {
  const navigate = useNavigate()
  const username = typeof window !== 'undefined' ? localStorage.getItem('hc_username') : null

  function handleLogout(){
    try{
      localStorage.removeItem('hc_token')
      localStorage.removeItem('hc_username')
    }catch(e){}
    navigate('/')
    window.location.reload()
  }

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
          {username ? (
            <>
              <PixelButton as="a" href="/profile">{username}</PixelButton>
              <PixelButton as="button" onClick={handleLogout}>Logout</PixelButton>
            </>
          ) : (
            <PixelButton as="a" href="/login">Login</PixelButton>
          )}
        </nav>
      </div>
    </header>
  )
}
