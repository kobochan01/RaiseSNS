import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

describe('AuthContext', () => {
  it('useAuth throws when used outside AuthProvider', () => {
    expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used within AuthProvider')
  })

  it('AuthProvider provides null user by default', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })

    expect(result.current.user).toBeNull()
  })

  it('setUser updates the user exposed by useAuth', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    const user = { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null }

    act(() => {
      result.current.setUser(user)
    })

    expect(result.current.user).toEqual(user)
  })
})
