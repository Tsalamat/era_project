import { ofetch } from 'ofetch'
import type { FetchOptions } from 'ofetch'

export const useApi = () => {
  const config = useRuntimeConfig()
  const auth = useAuthStore()
  const baseURL = import.meta.server ? String(config.apiBase) : '/api/session/proxy'

  const request = <T>(path: string, options: FetchOptions<'json'> = {}) => {
    auth.syncFromCookies()
    const headers = new Headers(options.headers as HeadersInit | undefined)
    const cookieToken = useCookie<string | null>('kta_access_token').value
    const token = cookieToken || auth.accessToken
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return ofetch<T>(path, { ...options, baseURL, credentials: 'include', headers })
  }

  return async <T>(path: string, options: FetchOptions<'json'> = {}) => {
    try {
      return await request<T>(path, options)
    } catch (error) {
      const requestError = error as { statusCode?: number; response?: { status?: number } }
      const status = requestError.statusCode || requestError.response?.status
      const hasRefreshToken = Boolean(useCookie<string | null>('kta_refresh_token').value || auth.refreshToken)
      if ((status === 401 || status === 403) && hasRefreshToken) {
        await auth.refreshSession()
        return request<T>(path, options)
      }
      throw error
    }
  }
}

export const apiErrorMessage = (error: unknown) => {
  const value = error as { data?: { detail?: string; message?: string; statusMessage?: string }; statusMessage?: string; message?: string }
  return value.data?.detail || value.data?.message || value.data?.statusMessage || value.statusMessage || value.message || 'Не удалось выполнить запрос'
}

export const useApiFile = () => {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  const urlFor = (path: string) => {
    const base = String(config.public.apiBase || '/api').replace(/\/$/, '')
    return `${base}${path.startsWith('/') ? path : `/${path}`}`
  }

  const fetchBlob = async (path: string) => {
    auth.syncFromCookies()
    const headers = new Headers()
    const token = useCookie<string | null>('kta_access_token').value || auth.accessToken
    if (token) headers.set('Authorization', `Bearer ${token}`)
    const response = await fetch(urlFor(path), { headers, credentials: 'include' })
    if (!response.ok) throw new Error(await response.text() || 'Не удалось скачать файл')
    return {
      blob: await response.blob(),
      filename: response.headers.get('content-disposition')?.match(/filename="?([^";]+)"?/)?.[1]
    }
  }

  const download = async (path: string, fallbackName: string) => {
    const { blob, filename } = await fetchBlob(path)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename || fallbackName
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  return { fetchBlob, download }
}
