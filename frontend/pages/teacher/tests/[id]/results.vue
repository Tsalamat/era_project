<script setup lang="ts">
import { AlertTriangle, Download, Timer, TrendingUp, UsersRound } from 'lucide-vue-next'
import type { TeacherTestStats } from '~/types/learning'

const route = useRoute()
const api = useApi()
const apiFile = useApiFile()
const id = String(route.params.id)
const { data: stats, error, pending } = await useAsyncData(`teacher-test-stats-${id}`, () => api<TeacherTestStats>(`/teacher/tests/${id}/results`))
const downloadingStats = ref(false)
const actionError = ref('')

usePageSeo({
  title: 'Статистика теста',
  description: 'Статистика прохождений, средний балл, время, сложные вопросы и результаты студентов.',
  path: `/teacher/tests/${id}/results`,
  robots: 'noindex,nofollow'
})

const formatDate = (value: string) => new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

const downloadStatsReport = async () => {
  downloadingStats.value = true
  actionError.value = ''
  try {
    await apiFile.download(`/teacher/tests/${id}/results/docx`, `teacher-test-analytics-${id}.docx`)
  } catch (requestError) {
    actionError.value = apiErrorMessage(requestError)
  } finally {
    downloadingStats.value = false
  }
}
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Аналитика</p>
        <h1 class="mt-2 text-4xl font-black">Статистика теста</h1>
      </div>
      <button type="button" class="button-secondary" :disabled="downloadingStats" @click="downloadStatsReport">
        <Download class="size-4" aria-hidden="true" />
        <span>{{ downloadingStats ? 'Готовим...' : 'Скачать Word-аналитику' }}</span>
      </button>
    </header>

    <p v-if="pending" class="text-sm font-semibold text-ink/60">Собираем статистику...</p>
    <p v-if="error" class="status-error">{{ apiErrorMessage(error) }}</p>
    <p v-if="actionError" class="status-error">{{ actionError }}</p>

    <template v-if="stats">
      <section class="grid gap-5 md:grid-cols-4">
        <div class="surface p-5">
          <UsersRound class="mb-3 size-6 text-ocean" aria-hidden="true" />
          <strong class="text-3xl">{{ stats.attemptsCount }}</strong>
          <span class="block text-sm text-ink/60">прохождений</span>
        </div>
        <div class="surface p-5">
          <TrendingUp class="mb-3 size-6 text-mint" aria-hidden="true" />
          <strong class="text-3xl">{{ stats.averageScorePercent }}%</strong>
          <span class="block text-sm text-ink/60">средний балл</span>
        </div>
        <div class="surface p-5">
          <Timer class="mb-3 size-6 text-gold" aria-hidden="true" />
          <strong class="text-3xl">{{ stats.averageTimeMinutes }}</strong>
          <span class="block text-sm text-ink/60">среднее время, мин</span>
        </div>
        <div class="surface p-5">
          <AlertTriangle class="mb-3 size-6 text-coral" aria-hidden="true" />
          <strong class="text-3xl">{{ stats.hardestQuestions.filter(item => item.wrongPercent >= 50).length }}</strong>
          <span class="block text-sm text-ink/60">сложных вопросов</span>
        </div>
      </section>

      <section class="surface overflow-hidden">
        <div class="border-b border-line p-5">
          <h2 class="text-2xl font-black">Самые сложные вопросы</h2>
        </div>
        <div v-if="stats.hardestQuestions.length" class="divide-y divide-line">
          <div v-for="question in stats.hardestQuestions" :key="question.questionId" class="grid gap-3 p-5 md:grid-cols-[1fr_120px]">
            <strong>{{ question.questionText }}</strong>
            <span class="font-black text-coral">{{ question.wrongPercent }}% ошибок</span>
          </div>
        </div>
        <p v-else class="p-5 text-sm text-ink/60">Данные появятся после первых прохождений.</p>
      </section>

      <section class="surface overflow-hidden">
        <div class="border-b border-line p-5">
          <h2 class="text-2xl font-black">Результаты студентов</h2>
        </div>
        <div v-if="stats.students.length" class="divide-y divide-line">
          <div v-for="student in stats.students" :key="student.attemptId" class="grid gap-3 p-5 md:grid-cols-[1fr_110px_120px_120px_110px_190px]">
            <div>
              <strong>{{ student.studentName }}</strong>
              <span class="block text-sm text-ink/55">{{ student.email }}</span>
            </div>
            <span class="font-black">{{ student.scorePercent }}%</span>
            <span class="font-bold text-mint">Ответил: {{ student.answeredQuestions }}</span>
            <span class="font-bold text-coral">Не ответил: {{ student.skippedQuestions }}</span>
            <span class="font-bold text-ink/65">{{ student.durationMinutes }} мин</span>
            <span class="text-ink/60">{{ formatDate(student.completedAt) }}</span>
          </div>
        </div>
        <p v-else class="p-5 text-sm text-ink/60">Тест еще никто не завершил.</p>
      </section>
    </template>
  </div>
</template>
