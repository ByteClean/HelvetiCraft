import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Home from '../pages/Home'

// Mock the ServerStatus component
vi.mock('../components/ServerStatus', () => ({
  default: () => <div data-testid="server-status">Server Status</div>
}))

describe('Home Page', () => {
  it('should render the HelvetiCraft title', () => {
    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    )
    const title = screen.getByRole('heading', { name: /helveticraft/i })
    expect(title).toBeInTheDocument()
  })

  it('should render the hero subtitle', () => {
    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    )
    const subtitle = screen.getByText(/demokratie & wirtschaft spielerisch erleben/i)
    expect(subtitle).toBeInTheDocument()
  })

  it('should render copy button', () => {
    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    )
    const buttons = screen.getAllByRole('button')
    expect(buttons.length).toBeGreaterThan(0)
  })

  it('should render ServerStatus component', () => {
    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    )
    const serverStatus = screen.getByTestId('server-status')
    expect(serverStatus).toBeInTheDocument()
  })
})
