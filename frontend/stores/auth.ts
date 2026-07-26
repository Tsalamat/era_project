import { defineStore } from 'pinia'
import type { Role } from '~/types/learning'

type AuthResponse = {
  accessToken: string
  refreshToken: string
  role: Role
  fullName: string
  email: string
}

type ApiMessage = {
  status: string
  message: string
}

export const useAuthStore = defineStore('auth', () => {
  const cookieOptions = { path: '/', sameSite: 'lax' as const, maxAge: 60 * 60 * 24 * 30 }
  const accessCookie = useCookie<string | null>('kta_access_token', cookieOptions)
  const refreshCookie = useCookie<string | null>('kta_refresh_token', cookieOptions)
  const roleCookie = useCookie<Role | null>('kta_role', cookieOptions)
  const nameCookie = useCookie<string | null>('kta_full_name', cookieOptions)
  const emailCookie = useCookie<string | null>('kta_email', cookieOptions)
  const accessToken = ref(accessCookie.value)
  const refreshToken = ref(refreshCookie.value)
  const role = ref(roleCookie.value)
  const fullName = ref(nameCookie.value)
  const email = ref(emailCookie.value)

  const isAuthenticated = computed(() => Boolean(accessToken.value))
  const isTeacher = computed(() => role.value === 'TEACHER' || role.value === 'ADMIN')
  const isAdmin = computed(() => role.value === 'ADMIN')

  const syncFromCookies = () => {
    accessToken.value = accessCookie.value
    refreshToken.value = refreshCookie.value
    role.value = roleCookie.value
    fullName.value = nameCookie.value
    email.value = emailCookie.value
  }

  const saveSession = (response: AuthResponse) => {
    accessToken.value = response.accessToken
    refreshToken.value = response.refreshToken
    role.value = response.role
    fullName.value = response.fullName
    email.value = response.email
    accessCookie.value = response.accessToken
    refreshCookie.value = response.refreshToken
    roleCookie.value = response.role
    nameCookie.value = response.fullName
    emailCookie.value = response.email
  }

  const authRequest = async (path: '/auth/login', body: Record<string, string>) => {
    const action = path.endsWith('login') ? 'login' : 'register'
    const response = await $fetch<AuthResponse>(`/api/session/${action}`, { method: 'POST', body })
    saveSession(response)
    return response
  }

  const login = (credentials: { email: string; password: string }) => authRequest('/auth/login', credentials)
  const requestRegistrationCode = (data: { fullName: string; email: string; password: string }) =>
    $fetch<ApiMessage>('/api/session/register-request-code', { method: 'POST', body: data })
  const verifyRegistrationCode = async (data: { email: string; code: string }) => {
    const response = await $fetch<AuthResponse>('/api/session/register-verify', { method: 'POST', body: data })
    saveSession(response)
    return response
  }
  const refreshSession = async () => {
    const cookieRefreshToken = refreshCookie.value
    const response = await $fetch<AuthResponse>('/api/session/refresh', {
      method: 'POST',
      body: { refreshToken: cookieRefreshToken || refreshToken.value || '' }
    })
    saveSession(response)
    return response
  }

  const logout = async () => {
    await $fetch('/api/session/logout', { method: 'POST' })
    accessToken.value = null
    refreshToken.value = null
    role.value = null
    fullName.value = null
    email.value = null
    accessCookie.value = null
    refreshCookie.value = null
    roleCookie.value = null
    nameCookie.value = null
    emailCookie.value = null
    return navigateTo('/')
  }

  return {
    accessToken,
    refreshToken,
    role,
    fullName,
    email,
    isAuthenticated,
    isTeacher,
    isAdmin,
    syncFromCookies,
    login,
    requestRegistrationCode,
    verifyRegistrationCode,
    refreshSession,
    logout
  }
})
