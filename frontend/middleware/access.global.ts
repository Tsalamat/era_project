export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore()
  auth.syncFromCookies()
  const protectedRoute = to.path.startsWith('/teacher') || to.path.startsWith('/admin') || to.path.startsWith('/cabinet')

  if (protectedRoute && !auth.isAuthenticated) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
  if (to.path.startsWith('/teacher') && !auth.isTeacher) {
    return navigateTo('/cabinet')
  }
  if (to.path.startsWith('/admin') && !auth.isAdmin) {
    return navigateTo('/cabinet')
  }
})
