export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string> | null

  constructor(status: number, message: string, fieldErrors: Record<string, string> | null) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) {
    return undefined as T
  }

  const data = await res.json().catch(() => null)

  if (!res.ok) {
    throw new ApiError(res.status, data?.message ?? 'エラーが発生しました', data?.fieldErrors ?? null)
  }

  return data as T
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api${path}`, {
    ...options,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  return handleResponse<T>(res)
}

export async function apiUpload<T>(path: string, formData: FormData): Promise<T> {
  const res = await fetch(`/api${path}`, {
    method: 'POST',
    credentials: 'include',
    body: formData,
  })
  return handleResponse<T>(res)
}
