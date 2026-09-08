import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, apiUpload, ApiError } from './client'

function mockResponse(overrides: { ok: boolean; status: number; json?: () => Promise<unknown> }) {
  return {
    ok: overrides.ok,
    status: overrides.status,
    json: overrides.json ?? vi.fn().mockResolvedValue({}),
  } as Response
}

describe('apiFetch', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends credentials include and JSON content-type header', async () => {
    fetchMock.mockResolvedValue(mockResponse({ ok: true, status: 200, json: vi.fn().mockResolvedValue({ a: 1 }) }))

    await apiFetch('/posts')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/posts',
      expect.objectContaining({
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      }),
    )
  })

  it('merges caller-provided headers with default content-type', async () => {
    fetchMock.mockResolvedValue(mockResponse({ ok: true, status: 200, json: vi.fn().mockResolvedValue({}) }))

    await apiFetch('/posts', { headers: { 'X-Custom': 'value' } })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/posts',
      expect.objectContaining({
        headers: { 'Content-Type': 'application/json', 'X-Custom': 'value' },
      }),
    )
  })

  it('returns parsed JSON body on success', async () => {
    fetchMock.mockResolvedValue(mockResponse({ ok: true, status: 200, json: vi.fn().mockResolvedValue({ a: 1 }) }))

    const result = await apiFetch('/posts')

    expect(result).toEqual({ a: 1 })
  })

  it('returns undefined without parsing body on 204 No Content', async () => {
    const json = vi.fn()
    fetchMock.mockResolvedValue(mockResponse({ ok: true, status: 204, json }))

    const result = await apiFetch('/posts/1')

    expect(result).toBeUndefined()
    expect(json).not.toHaveBeenCalled()
  })

  it('throws ApiError with status and message from response body on failure', async () => {
    fetchMock.mockResolvedValue(
      mockResponse({ ok: false, status: 400, json: vi.fn().mockResolvedValue({ message: '不正なリクエストです' }) }),
    )

    await expect(apiFetch('/posts')).rejects.toMatchObject({
      status: 400,
      message: '不正なリクエストです',
    })
  })

  it('throws ApiError with fallback message when body has no message field', async () => {
    fetchMock.mockResolvedValue(mockResponse({ ok: false, status: 500, json: vi.fn().mockResolvedValue({}) }))

    await expect(apiFetch('/posts')).rejects.toMatchObject({
      status: 500,
      message: 'エラーが発生しました',
    })
  })

  it('throws ApiError with fieldErrors when present, null when absent', async () => {
    fetchMock.mockResolvedValueOnce(
      mockResponse({
        ok: false,
        status: 400,
        json: vi.fn().mockResolvedValue({ message: '入力エラー', fieldErrors: { username: '必須です' } }),
      }),
    )
    await expect(apiFetch('/posts')).rejects.toMatchObject({ fieldErrors: { username: '必須です' } })

    fetchMock.mockResolvedValueOnce(
      mockResponse({ ok: false, status: 400, json: vi.fn().mockResolvedValue({ message: '入力エラー' }) }),
    )
    await expect(apiFetch('/posts')).rejects.toMatchObject({ fieldErrors: null })
  })

  it('falls back to null data when response body is not valid JSON', async () => {
    fetchMock.mockResolvedValue(
      mockResponse({ ok: true, status: 200, json: vi.fn().mockRejectedValue(new Error('invalid json')) }),
    )

    const result = await apiFetch('/posts')

    expect(result).toBeNull()
  })
})

describe('apiUpload', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('posts FormData with credentials include and without explicit content-type header', async () => {
    fetchMock.mockResolvedValue(
      mockResponse({ ok: true, status: 200, json: vi.fn().mockResolvedValue({ imageUrl: 'x' }) }),
    )
    const formData = new FormData()

    await apiUpload('/images/posts', formData)

    expect(fetchMock).toHaveBeenCalledWith('/api/images/posts', {
      method: 'POST',
      credentials: 'include',
      body: formData,
    })
  })

  it('returns parsed JSON body on success', async () => {
    fetchMock.mockResolvedValue(
      mockResponse({ ok: true, status: 200, json: vi.fn().mockResolvedValue({ imageUrl: 'https://x' }) }),
    )

    const result = await apiUpload('/images/posts', new FormData())

    expect(result).toEqual({ imageUrl: 'https://x' })
  })

  it('throws ApiError on failure same as apiFetch', async () => {
    fetchMock.mockResolvedValue(
      mockResponse({ ok: false, status: 413, json: vi.fn().mockResolvedValue({ message: 'ファイルが大きすぎます' }) }),
    )

    await expect(apiUpload('/images/posts', new FormData())).rejects.toMatchObject({
      status: 413,
      message: 'ファイルが大きすぎます',
    })
  })
})

describe('ApiError', () => {
  it('exposes status, message, and fieldErrors properties', () => {
    const error = new ApiError(400, 'メッセージ', { username: '必須です' })

    expect(error.status).toBe(400)
    expect(error.message).toBe('メッセージ')
    expect(error.fieldErrors).toEqual({ username: '必須です' })
  })
})
