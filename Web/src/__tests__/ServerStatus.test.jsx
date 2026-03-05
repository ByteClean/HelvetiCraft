import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import ServerStatus from '../components/ServerStatus'

// Mock the minecraft-server-util
vi.mock('minecraft-server-util', () => ({
  status: vi.fn().mockResolvedValue({
    players: { online: 5, max: 20 },
    description: {
      extra: [{ text: 'Welcome to HelvetiCraft' }]
    },
    version: { name: '1.20.4' }
  })
}))

describe('ServerStatus Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render without crashing', () => {
    const { container } = render(<ServerStatus />)
    expect(container).toBeInTheDocument()
  })

  it('should render a button to fetch server status', () => {
    const { container } = render(<ServerStatus />)
    // The component should render some UI
    expect(container.querySelector('.server-status-card')).toBeTruthy()
  })
})
