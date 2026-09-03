import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { login, register } from '../api/auth'
import { AuthProvider } from '../context/AuthContext'
import { SignupPage } from './SignupPage'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/auth', () => ({
  register: vi.fn(),
  login: vi.fn(),
}))

function renderSignupPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <SignupPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('SignupPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear()
    vi.mocked(register).mockReset()
    vi.mocked(login).mockReset()
  })

  it('renders all fields with a signup button', () => {
    renderSignupPage()

    expect(screen.getByLabelText('ユーザー名（@username）')).toBeInTheDocument()
    expect(screen.getByLabelText('表示名')).toBeInTheDocument()
    expect(screen.getByLabelText('メールアドレス')).toBeInTheDocument()
    expect(screen.getByLabelText('パスワード（8文字以上）')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '登録する' })).toBeInTheDocument()
  })

  it('shows a client-side validation error and does not call the API for an invalid username', async () => {
    renderSignupPage()

    await userEvent.type(screen.getByLabelText('ユーザー名（@username）'), 'ab')
    await userEvent.type(screen.getByLabelText('表示名'), '太郎')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'taro@example.com')
    await userEvent.type(screen.getByLabelText('パスワード（8文字以上）'), 'password123')
    await userEvent.click(screen.getByRole('button', { name: '登録する' }))

    expect(
      await screen.findByText('ユーザー名は3〜30文字の英数字とアンダースコアのみ使用できます。'),
    ).toBeInTheDocument()
    expect(register).not.toHaveBeenCalled()
  })

  it('registers, auto-logs in, and navigates to home on success', async () => {
    vi.mocked(register).mockResolvedValue({ id: 1, username: 'taro_dev', displayName: '太郎' })
    vi.mocked(login).mockResolvedValue({ id: 1, username: 'taro_dev', displayName: '太郎', avatarUrl: null })
    renderSignupPage()

    await userEvent.type(screen.getByLabelText('ユーザー名（@username）'), 'taro_dev')
    await userEvent.type(screen.getByLabelText('表示名'), '太郎')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'taro@example.com')
    await userEvent.type(screen.getByLabelText('パスワード（8文字以上）'), 'password123')
    await userEvent.click(screen.getByRole('button', { name: '登録する' }))

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'))
    expect(login).toHaveBeenCalledWith({ email: 'taro@example.com', password: 'password123' })
  })
})
