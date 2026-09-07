import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { searchUsers, type UserSearchResponse } from '../api/search'
import { SearchPage } from './SearchPage'

vi.mock('../api/search', () => ({
  searchUsers: vi.fn(),
}))

function renderSearchPage(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/search" element={<SearchPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('SearchPage', () => {
  beforeEach(() => {
    vi.mocked(searchUsers).mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows a prompt when no keyword is given', () => {
    renderSearchPage('/search')

    expect(screen.getByText('ユーザー名(@username)を入力して検索してください。')).toBeInTheDocument()
    expect(searchUsers).not.toHaveBeenCalled()
  })

  it('renders matching results with the total count', async () => {
    const response: UserSearchResponse = {
      results: [{ username: 'kobochanTaro', displayName: '太郎', avatarUrl: null }],
      totalCount: 1,
    }
    vi.mocked(searchUsers).mockResolvedValue(response)

    renderSearchPage('/search?q=kobochan')

    await waitFor(() => expect(searchUsers).toHaveBeenCalledWith('kobochan'))
    expect(await screen.findByText('「kobochan」の検索結果(1件)')).toBeInTheDocument()
    expect(screen.getByText('太郎')).toBeInTheDocument()
    expect(screen.getByText('@kobochanTaro')).toBeInTheDocument()
  })

  it('shows an empty state when there are no matches', async () => {
    vi.mocked(searchUsers).mockResolvedValue({ results: [], totalCount: 0 })

    renderSearchPage('/search?q=nobody')

    expect(await screen.findByText('該当するユーザーが見つかりませんでした。')).toBeInTheDocument()
  })

  it('shows an error message when the search request fails', async () => {
    vi.mocked(searchUsers).mockRejectedValue(new Error('boom'))

    renderSearchPage('/search?q=taro')

    expect(await screen.findByText('検索に失敗しました')).toBeInTheDocument()
  })
})
