import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
  content: [
    './app.vue',
    './components/**/*.{vue,js,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './stores/**/*.ts',
    './composables/**/*.ts'
  ],
  theme: {
    extend: {
      colors: {
        ink: 'rgb(var(--color-ink) / <alpha-value>)',
        paper: 'rgb(var(--color-paper) / <alpha-value>)',
        line: 'rgb(var(--color-line) / <alpha-value>)',
        ocean: 'rgb(var(--color-ocean) / <alpha-value>)',
        mint: 'rgb(var(--color-mint) / <alpha-value>)',
        coral: 'rgb(var(--color-coral) / <alpha-value>)',
        gold: 'rgb(var(--color-gold) / <alpha-value>)',
        surface: 'rgb(var(--color-surface) / <alpha-value>)'
      },
      boxShadow: {
        soft: 'var(--shadow-soft)'
      }
    }
  }
}
