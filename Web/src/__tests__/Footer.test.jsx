import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import Footer from '../components/Footer'

describe('Footer', () => {
  it('should render the current year in copyright text', () => {
    const currentYear = new Date().getFullYear()
    render(<Footer />)
    const copyrightText = screen.getByText(new RegExp(`© ${currentYear}`))
    expect(copyrightText).toBeInTheDocument()
  })

  it('should render HelvetiCraft branding in footer', () => {
    render(<Footer />)
    const helveticraft = screen.getByText(/helveticraft/i)
    expect(helveticraft).toBeInTheDocument()
  })

  it('should render Impressum button', () => {
    render(<Footer />)
    const impressumBtn = screen.getByRole('button', { name: /impressum/i })
    expect(impressumBtn).toBeInTheDocument()
  })

  it('should render Datenschutz button', () => {
    render(<Footer />)
    const datenschutzBtn = screen.getByRole('button', { name: /datenschutz/i })
    expect(datenschutzBtn).toBeInTheDocument()
  })

  it('should render Discord link', () => {
    render(<Footer />)
    const discordLink = screen.getByRole('link', { name: /discord/i })
    expect(discordLink).toBeInTheDocument()
    expect(discordLink).toHaveAttribute('href', 'https://discord.gg/q2mMrXad9h')
    expect(discordLink).toHaveAttribute('target', '_blank')
  })
})
