import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import App from '../App'

// Mock all page components
vi.mock('../pages/Home', () => ({
  default: () => <div data-testid="home-page">Home Page</div>
}))
vi.mock('../pages/Initiatives', () => ({
  default: () => <div data-testid="initiatives-page">Initiatives Page</div>
}))
vi.mock('../pages/Status', () => ({
  default: () => <div data-testid="status-page">Status Page</div>
}))
vi.mock('../pages/Guidance', () => ({
  default: () => <div data-testid="guidance-page">Guidance Page</div>
}))
vi.mock('../pages/News', () => ({
  default: () => <div data-testid="news-page">News Page</div>
}))
vi.mock('../pages/About', () => ({
  default: () => <div data-testid="about-page">About Page</div>
}))

describe('App', () => {
  it('should render Header component', () => {
    render(
      <BrowserRouter>
        <App />
      </BrowserRouter>
    )
    const heading = screen.getByRole('heading', { name: /helveticraft/i })
    expect(heading).toBeInTheDocument()
  })

  it('should render Footer component', () => {
    render(
      <BrowserRouter>
        <App />
      </BrowserRouter>
    )
    const currentYear = new Date().getFullYear()
    const copyright = screen.getByText(new RegExp(`© ${currentYear}`))
    expect(copyright).toBeInTheDocument()
  })

  it('should render home page at root path', () => {
    render(
      <BrowserRouter>
        <App />
      </BrowserRouter>
    )
    const homePage = screen.getByTestId('home-page')
    expect(homePage).toBeInTheDocument()
  })

  it('should have main-content section', () => {
    const { container } = render(
      <BrowserRouter>
        <App />
      </BrowserRouter>
    )
    const mainContent = container.querySelector('.main-content')
    expect(mainContent).toBeInTheDocument()
  })
})
