import { apiUpload } from './client'

export type ImageCategory = 'post' | 'avatar'

export function uploadImage(file: File, category: ImageCategory): Promise<{ imageUrl: string }> {
  const formData = new FormData()
  formData.append('image', file)
  return apiUpload(`/images/${category}s`, formData)
}
