import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import PixelButton from '../components/PixelButton'

describe('PixelButton', () => {
  it('should render a button element by default', () => {
    render(<PixelButton>Click me</PixelButton>)
    const button = screen.getByRole('button', { name: /click me/i })
    expect(button).toBeInTheDocument()
  })

  it('should render as a link when as prop is "a"', () => {
    render(<PixelButton as="a" href="/test">Home</PixelButton>)
    const link = screen.getByRole('link', { name: /home/i })
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', '/test')
  })

  it('should apply the minecraft-button class', () => {
    render(<PixelButton>Test</PixelButton>)
    const button = screen.getByRole('button')
    expect(button).toHaveClass('minecraft-button')
  })

  it('should apply additional className', () => {
    render(<PixelButton className="custom-class">Test</PixelButton>)
    const button = screen.getByRole('button')
    expect(button).toHaveClass('minecraft-button', 'custom-class')
  })
})
