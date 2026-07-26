export type ThemeMode = 'light' | 'dark'

export const useThemeMode = () => {
  const themeCookie = useCookie<ThemeMode>('kta_theme', {
    path: '/',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 365,
    default: () => 'light'
  })
  const mode = useState<ThemeMode>('kta-theme-mode', () => themeCookie.value === 'dark' ? 'dark' : 'light')
  const isDark = computed(() => mode.value === 'dark')

  const setTheme = (nextMode: ThemeMode) => {
    mode.value = nextMode
    themeCookie.value = nextMode
  }

  const toggleTheme = () => setTheme(isDark.value ? 'light' : 'dark')

  watch(mode, value => {
    themeCookie.value = value
  }, { immediate: true })

  return { mode, isDark, setTheme, toggleTheme }
}
