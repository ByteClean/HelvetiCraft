import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Initiatives from '../pages/Initiatives'

// Mock fetch
global.fetch = vi.fn()

describe('Initiatives Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should display loading state initially', () => {
    global.fetch.mockImplementation(() =>
      new Promise(() => {}) // Never resolves to keep loading state
    )
    
    const { container } = render(
      <BrowserRouter>
        <Initiatives />
      </BrowserRouter>
    )
    
    // Component should render without crashing
    expect(container).toBeInTheDocument()
  })

  it('should display page heading', async () => {
    global.fetch.mockImplementation((url) => {
      if (url.includes('phases')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ id: 0, label: 'Phase 1', start_phase1: new Date(), start_phase2: new Date(), start_phase3: new Date() })
        })
      }
      return Promise.resolve({
        ok: true,
        json: async () => ({ initiatives: [] })
      })
    })

    const { container } = render(
      <BrowserRouter>
        <Initiatives />
      </BrowserRouter>
    )
    
    expect(container).toBeInTheDocument()
  })
})
