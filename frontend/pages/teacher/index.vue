<script setup lang="ts">
import { BarChart3, FilePlus2, ListChecks, Newspaper } from 'lucide-vue-next'
import type { TestSummary } from '~/types/learning'

const api = useApi()
const { data: teacherTests, error } = await useAsyncData('teacher-dashboard-tests', () => api<TestSummary[]>('/teacher/tests'))
const totalAttempts = computed(() => teacherTests.value?.reduce((sum, test) => sum + test.attempts, 0) || 0)
const drafts = computed(() => teacherTests.value?.filter(test => test.status === 'DRAFT').length || 0)

usePageSeo({
  title: 'Кабинет учителя',
  description: 'Создание тестов КТА, вопросы, варианты ответов, публикация и статистика.',
  path: '/teacher',
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Учитель</p>
        <h1 class="mt-2 text-4xl font-black">Кабинет учителя</h1>
      </div>
      <NuxtLink to="/teacher/tests/create" class="button-primary">
        <FilePlus2 class="size-5" aria-hidden="true" />
        <span>Создать тест</span>
      </NuxtLink>
      <NuxtLink to="/teacher/blog" class="button-secondary">
        <Newspaper class="size-5" aria-hidden="true" />
        <span>Блог</span>
      </NuxtLink>
    </header>

    <p v-if="error" class="status-error">{{ apiErrorMessage(error) }}</p>

    <section class="grid gap-5 md:grid-cols-3">
      <div class="surface p-5">
        <ListChecks class="mb-3 size-6 text-ocean" aria-hidden="true" />
        <strong class="text-3xl">{{ teacherTests?.length || 0 }}</strong>
        <span class="block text-sm text-ink/60">тестов</span>
      </div>
      <div class="surface p-5">
        <BarChart3 class="mb-3 size-6 text-mint" aria-hidden="true" />
        <strong class="text-3xl">{{ totalAttempts }}</strong>
        <span class="block text-sm text-ink/60">прохождений</span>
      </div>
      <div class="surface p-5">
        <FilePlus2 class="mb-3 size-6 text-gold" aria-hidden="true" />
        <strong class="text-3xl">{{ drafts }}</strong>
        <span class="block text-sm text-ink/60">черновиков</span>
      </div>
    </section>

    <section class="surface overflow-hidden">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line p-5">
        <h2 class="text-2xl font-black">Последние тесты</h2>
        <NuxtLink to="/teacher/tests" class="button-secondary">Открыть список</NuxtLink>
      </div>
      <div v-if="teacherTests?.length" class="divide-y divide-line">
        <div v-for="test in teacherTests" :key="test.id" class="flex flex-wrap items-center justify-between gap-4 p-5">
          <div>
            <h3 class="font-black">{{ test.title }}</h3>
            <p class="text-sm text-ink/60">{{ test.subject }} · {{ test.timeLimitMinutes }} минут</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <NuxtLink :to="`/teacher/tests/${test.id}/edit`" class="button-secondary">Редактировать</NuxtLink>
            <NuxtLink :to="`/teacher/tests/${test.id}/results`" class="button-secondary">Статистика</NuxtLink>
          </div>
        </div>
      </div>
      <p v-else class="p-5 text-sm font-semibold text-ink/60">Созданные тесты появятся здесь.</p>
    </section>
  </div>
</template>
