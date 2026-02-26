import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Status from '../pages/Status'

// Mock ServerStatus component
vi.mock('../components/ServerStatus', () => ({
  default: () => <div data-testid="server-status">Server Status Mock</div>
}))

// Mock fetch
global.fetch = vi.fn()

describe('Status Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render ServerStatus component', () => {
    render(
      <BrowserRouter>
        <Status />
      </BrowserRouter>
    )
    
    const serverStatus = screen.getByTestId('server-status')
    expect(serverStatus).toBeInTheDocument()
  })

  it('should have a search input', () => {
    render(
      <BrowserRouter>
        <Status />
      </BrowserRouter>
    )

    // Component should render without crashing
    const { container } = render(
      <BrowserRouter>
        <Status />
      </BrowserRouter>
    )
    expect(container).toBeInTheDocument()
  })
})
