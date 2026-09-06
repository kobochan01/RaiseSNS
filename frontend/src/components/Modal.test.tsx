import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Modal } from './Modal'

describe('Modal', () => {
  it('renders children inside a dialog', () => {
    render(
      <Modal onClose={vi.fn()}>
        <p>モーダルの中身</p>
      </Modal>,
    )

    expect(screen.getByText('モーダルの中身')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('calls onClose when the overlay is clicked', () => {
    const onClose = vi.fn()
    render(
      <Modal onClose={onClose}>
        <p>モーダルの中身</p>
      </Modal>,
    )

    fireEvent.click(screen.getByRole('dialog').parentElement as HTMLElement)

    expect(onClose).toHaveBeenCalled()
  })

  it('does not call onClose when the dialog content is clicked', () => {
    const onClose = vi.fn()
    render(
      <Modal onClose={onClose}>
        <p>モーダルの中身</p>
      </Modal>,
    )

    fireEvent.click(screen.getByText('モーダルの中身'))

    expect(onClose).not.toHaveBeenCalled()
  })

  it('calls onClose when the Escape key is pressed', () => {
    const onClose = vi.fn()
    render(
      <Modal onClose={onClose}>
        <p>モーダルの中身</p>
      </Modal>,
    )

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(onClose).toHaveBeenCalled()
  })
})
