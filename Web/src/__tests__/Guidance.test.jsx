import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import Guidance from '../pages/Guidance'

describe('Guidance Page', () => {
  it('should render the guidance page', () => {
    const { container } = render(
      <BrowserRouter>
        <Guidance />
      </BrowserRouter>
    )
    expect(container).toBeInTheDocument()
  })

  it('should have guidance-page class', () => {
    const { container } = render(
      <BrowserRouter>
        <Guidance />
      </BrowserRouter>
    )
    
    const guidancePage = container.querySelector('.guidance-page')
    expect(guidancePage).toBeTruthy()
  })
})
