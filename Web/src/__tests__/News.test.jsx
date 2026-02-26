import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import News from '../pages/News'

describe('News Page', () => {
  it('should render the news page', () => {
    const { container } = render(
      <BrowserRouter>
        <News />
      </BrowserRouter>
    )
    expect(container).toBeInTheDocument()
  })

  it('should have news-page class', () => {
    const { container } = render(
      <BrowserRouter>
        <News />
      </BrowserRouter>
    )
    
    const newsPage = container.querySelector('.news-page')
    expect(newsPage).toBeTruthy()
  })
})
