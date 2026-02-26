import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import About from '../pages/About'

describe('About Page', () => {
  it('should render the about page', () => {
    const { container } = render(
      <BrowserRouter>
        <About />
      </BrowserRouter>
    )
    expect(container).toBeInTheDocument()
  })

  it('should have about-page class', () => {
    const { container } = render(
      <BrowserRouter>
        <About />
      </BrowserRouter>
    )
    
    const aboutPage = container.querySelector('.about-page')
    expect(aboutPage).toBeTruthy()
  })
})
