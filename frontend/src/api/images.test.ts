import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiUpload } from './client'
import { uploadImage } from './images'

vi.mock('./client', () => ({
  apiUpload: vi.fn(),
}))

describe('images api', () => {
  beforeEach(() => {
    vi.mocked(apiUpload).mockReset()
    vi.mocked(apiUpload).mockResolvedValue({ imageUrl: 'https://example.com/a.png' })
  })

  it('uploadImage builds FormData with image field and calls apiUpload with posts path for post category', async () => {
    const file = new File(['content'], 'a.png', { type: 'image/png' })

    await uploadImage(file, 'post')

    expect(apiUpload).toHaveBeenCalledWith('/images/posts', expect.any(FormData))
    const formData = vi.mocked(apiUpload).mock.calls[0][1] as FormData
    expect(formData.get('image')).toBe(file)
  })

  it('uploadImage calls apiUpload with avatars path for avatar category', async () => {
    const file = new File(['content'], 'avatar.png', { type: 'image/png' })

    await uploadImage(file, 'avatar')

    expect(apiUpload).toHaveBeenCalledWith('/images/avatars', expect.any(FormData))
  })
})
