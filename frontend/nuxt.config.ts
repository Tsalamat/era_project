declare const process: { env: Record<string, string | undefined> }

export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',
  ssr: true,
  modules: ['@pinia/nuxt', '@nuxtjs/tailwindcss'],
  css: ['~/assets/css/main.css'],
  app: {
    head: {
      htmlAttrs: { lang: 'ru' },
      titleTemplate: '%s | Тест Магистратура',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'theme-color', content: '#f7f9fb' },
        { property: 'og:type', content: 'website' },
        { name: 'twitter:card', content: 'summary_large_image' }
      ],
      link: [{ rel: 'icon', href: '/favicon.svg' }]
    }
  },
  runtimeConfig: {
    apiBase: process.env.NUXT_API_BASE || 'http://localhost:8080/api',
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080/api',
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL || 'https://invest-gold.asia'
    }
  },
  typescript: {
    strict: true,
    typeCheck: false
  }
})
