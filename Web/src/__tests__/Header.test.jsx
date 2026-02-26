import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Header from '../components/Header'

describe('Header', () => {
  it('should render the HelvetiCraft title', () => {
    render(
      <BrowserRouter>
        <Header />
      </BrowserRouter>
    )
    const title = screen.getByRole('heading', { name: /helveticraft/i })
    expect(title).toBeInTheDocument()
  })

  it('should render all navigation links', () => {
    render(
      <BrowserRouter>
        <Header />
      </BrowserRouter>
    )
    expect(screen.getByRole('link', { name: /home/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /initiativen/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /anleitung/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /news/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /über uns/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /status/i })).toBeInTheDocument()
  })

  it('should have correct href for home link', () => {
    render(
      <BrowserRouter>
        <Header />
      </BrowserRouter>
    )
    const homeLink = screen.getByRole('link', { name: /home/i })
    expect(homeLink).toHaveAttribute('href', '/')
  })
})
