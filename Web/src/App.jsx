import React from 'react'
import { Routes, Route } from 'react-router-dom'
import Header from './components/Header'
import Footer from './components/Footer'
import Home from './pages/Home'
import Initiatives from './pages/Initiatives'
import Status from './pages/Status'
import Guidance from './pages/Guidance'
import News from './pages/News'
import About from './pages/About'

export default function App() {
  return (
    <div className="app">
      <Header />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/initiatives" element={<Initiatives />} />
          <Route path="/status" element={<Status />} />
          <Route path="/guidance" element={<Guidance />} />
          <Route path="/news" element={<News />} />
          <Route path="/about" element={<About />} />
        </Routes>
      </main>
      <Footer />
    </div>
  )
}