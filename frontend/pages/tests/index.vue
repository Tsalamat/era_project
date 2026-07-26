<script setup lang="ts">
import type { TestSummary } from '~/types/learning'

const api = useApi()
const { data, error, pending } = await useAsyncData('published-tests', () => api<TestSummary[]>('/tests'))
const publishedTests = computed(() => data.value || [])
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')

usePageSeo({
  title: 'Пробные тесты КТА онлайн',
  description: 'Бесплатные пробные тесты КТА для магистратуры с таймером, результатом, аналитикой, объяснением ошибок и сохранением прогресса.',
  path: '/tests',
  keywords: 'пробные тесты КТА, тесты КТА онлайн, КТА магистратура тест, логическое мышление КТА, английский КТА',
  schema: {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    itemListElement: publishedTests.value.map((test, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: test.title,
      url: `${siteUrl}/tests/${test.slug}`
    }))
  }
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="surface flex flex-wrap items-end justify-between gap-4 p-6 md:p-8">
      <div>
        <p class="eyebrow">Практика</p>
        <h1 class="mt-2 text-4xl font-black">Тесты КТА</h1>
        <p class="mt-3 max-w-2xl text-ink/65">Пробные тесты с таймером, результатом, объяснениями и скачиваемой аналитикой.</p>
      </div>
      <NuxtLink to="/courses" class="button-secondary">Курсы</NuxtLink>
    </header>

    <p v-if="pending" class="text-sm font-semibold text-ink/60">Загружаем тесты...</p>
    <p v-else-if="error" class="status-error">Не удалось загрузить тесты. Попробуйте обновить страницу.</p>

    <div v-if="publishedTests.length" class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
      <TestCard v-for="test in publishedTests" :key="test.id" :test="test" />
    </div>
    <p v-else-if="!pending && !error" class="text-sm text-ink/60">Опубликованных тестов пока нет.</p>
  </div>
</template>
