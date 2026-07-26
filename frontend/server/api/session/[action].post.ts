import type { Role } from '~/types/learning'

type AuthResponse = {
  accessToken: string
  refreshToken: string
  role: Role
  fullName: string
  email: string
}

const cookieOptions = {
  path: '/',
  sameSite: 'lax' as const,
  maxAge: 60 * 60 * 24 * 30,
  httpOnly: false,
  secure: false
}

export default defineEventHandler(async (event) => {
  const action = getRouterParam(event, 'action')
  const cookieNames = ['kta_access_token', 'kta_refresh_token', 'kta_role', 'kta_full_name', 'kta_email']
  const forwardedProto = getHeader(event, 'x-forwarded-proto')
  const secureCookieOptions = {
    ...cookieOptions,
    secure: forwardedProto === 'https' || getRequestURL(event).protocol === 'https:'
  }

  if (action === 'logout') {
    cookieNames.forEach(name => deleteCookie(event, name, { path: '/' }))
    return { message: 'Сессия завершена' }
  }

  if (action !== 'login' && action !== 'register' && action !== 'refresh' && action !== 'register-request-code' && action !== 'register-verify') {
    throw createError({ statusCode: 404, statusMessage: 'Неизвестное действие' })
  }

  try {
    const config = useRuntimeConfig(event)
    const body = await readBody<Record<string, string> | null>(event) || {}
    const payload = action === 'refresh'
      ? { refreshToken: body.refreshToken || getCookie(event, 'kta_refresh_token') || '' }
      : body
    const authPath = action === 'register-request-code'
      ? '/auth/register/request-code'
      : action === 'register-verify'
        ? '/auth/register/verify'
        : `/auth/${action}`
    const response = await $fetch<AuthResponse | { status: string; message: string }>(authPath, {
      baseURL: String(config.apiBase),
      method: 'POST',
      body: payload
    })

    if (action === 'register-request-code' || action === 'register') {
      return response
    }
    const session = response as AuthResponse
    setCookie(event, 'kta_access_token', session.accessToken, secureCookieOptions)
    setCookie(event, 'kta_refresh_token', session.refreshToken, secureCookieOptions)
    setCookie(event, 'kta_role', session.role, secureCookieOptions)
    setCookie(event, 'kta_full_name', session.fullName, secureCookieOptions)
    setCookie(event, 'kta_email', session.email, secureCookieOptions)
    return session
  } catch (error) {
    const requestError = error as { statusCode?: number; statusMessage?: string; data?: { detail?: string; message?: string } }
    const fallbackMessage = action === 'register-request-code' || action === 'register'
      ? 'Не удалось отправить код подтверждения'
      : action === 'register-verify'
        ? 'Не удалось подтвердить код'
        : 'Не удалось выполнить вход'
    throw createError({
      statusCode: requestError.statusCode || 500,
      statusMessage: requestError.data?.detail || requestError.data?.message || requestError.statusMessage || fallbackMessage
    })
  }
})
