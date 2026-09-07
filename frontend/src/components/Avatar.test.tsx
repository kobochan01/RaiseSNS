import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Avatar } from './Avatar'

describe('Avatar', () => {
  it('renders the display name initial when no avatarUrl is set', () => {
    render(<Avatar userId={1} displayName="太郎" avatarUrl={null} />)

    expect(screen.getByText('太')).toBeInTheDocument()
  })

  it('renders an image background when avatarUrl is set', () => {
    render(<Avatar userId={1} displayName="太郎" avatarUrl="https://example.com/avatar.png" />)

    expect(screen.queryByText('太')).not.toBeInTheDocument()
    const avatar = screen.getByRole('img', { name: '太郎' })
    expect(avatar).toHaveStyle('background-image: url(https://example.com/avatar.png)')
  })

  it('applies the requested size class', () => {
    render(<Avatar userId={1} displayName="太郎" avatarUrl={null} size="lg" />)

    expect(screen.getByText('太')).toHaveClass('avatar--lg')
  })
})
