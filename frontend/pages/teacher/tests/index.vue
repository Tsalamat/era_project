<script setup lang="ts">
import { FilePlus2 } from 'lucide-vue-next'
import type { TestSummary } from '~/types/learning'

const api = useApi()
const { data: tests, error, pending, refresh } = await useAsyncData('teacher-tests', () => api<TestSummary[]>('/teacher/tests'))
const actionError = ref('')
const actionMessage = ref('')
const deletingId = ref('')

const deleteTest = async (test: TestSummary) => {
  if (!import.meta.client || deletingId.value) return
  const confirmed = window.confirm(`Удалить тест "${test.title}"? Результаты и вопросы этого теста тоже будут удалены.`)
  if (!confirmed) return

  deletingId.value = test.id
  actionError.value = ''
  actionMessage.value = ''
  try {
    await api(`/teacher/tests/${test.id}`, { method: 'DELETE' })
    await refresh()
    actionMessage.value = 'Тест удалён'
  } catch (error) {
    actionError.value = apiErrorMessage(error)
  } finally {
    deletingId.value = ''
  }
}

usePageSeo({
  title: 'Тесты учителя',
  description: 'Список тестов учителя с черновиками, публикацией и статистикой.',
  path: '/teacher/tests',
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Управление</p>
        <h1 class="mt-2 text-4xl font-black">Тесты</h1>
      </div>
      <NuxtLink to="/teacher/tests/create" class="button-primary">
        <FilePlus2 class="size-5" aria-hidden="true" />
        <span>Создать тест</span>
      </NuxtLink>
    </header>

    <p v-if="pending" class="text-sm font-semibold text-ink/60">Загружаем тесты...</p>
    <p v-if="error" class="status-error">{{ apiErrorMessage(error) }}</p>
    <p v-if="actionMessage" class="status-success">{{ actionMessage }}</p>
    <p v-if="actionError" class="status-error">{{ actionError }}</p>

    <div v-if="tests?.length" class="grid gap-5 md:grid-cols-2">
      <TestCard v-for="test in tests" :key="test.id" :test="test" teacher @delete="deleteTest" />
    </div>
    <section v-else-if="!pending && !error" class="surface p-8 text-center">
      <h2 class="text-2xl font-black">Тестов пока нет</h2>
      <NuxtLink to="/teacher/tests/create" class="button-primary mt-4">Создать первый тест</NuxtLink>
    </section>
  </div>
</template>
