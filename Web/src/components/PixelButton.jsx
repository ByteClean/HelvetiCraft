import React from 'react'

export default function PixelButton({ as = 'button', children, className = '', ...props }) {
  const Tag = as
  return (
    <Tag className={`minecraft-button ${className}`} {...props}>
      {children}
    </Tag>
  )
}