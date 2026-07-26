<script setup lang="ts">
import { Award, Bell, BookOpenCheck, CheckCircle2, ClipboardList, Layers3 } from 'lucide-vue-next'
import type { LearningSummary, StudentAssignedTest, TestResult, UserNotification } from '~/types/learning'

const auth = useAuthStore()
const api = useApi()
const { data: results, error: resultsError } = await useAsyncData('student-results', () => api<TestResult[]>('/users/me/results'))
const { data: learning, error: learningError } = await useAsyncData('student-learning-summary', () => api<LearningSummary>('/users/me/learning-summary'))
const { data: assignments, error: assignmentsError } = await useAsyncData('student-assignments', () => api<StudentAssignedTest[]>('/users/me/assignments'))
const { data: notifications, error: notificationsError, refresh: refreshNotifications } = await useAsyncData('student-notifications', () => api<UserNotification[]>('/users/me/notifications'))
const average = computed(() => results.value?.length
  ? Math.round(results.value.reduce((sum, result) => sum + result.scorePercent, 0) / results.value.length)
  : 0)
const unreadNotifications = computed(() => notifications.value?.filter(item => !item.read).length || 0)
const formatDate = (value?: string) => value ? new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium' }).format(new Date(value)) : ''

const markRead = async (notification: UserNotification) => {
  if (notification.read) return
  await api(`/users/me/notifications/${notification.id}/read`, { method: 'POST' })
  await refreshNotifications()
}

usePageSeo({
  title: 'Личный кабинет студента',
  description: 'Курсы, прогресс и результаты тестов студента.',
  path: '/cabinet',
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header>
      <p class="eyebrow">Личный кабинет</p>
      <h1 class="mt-2 text-4xl font-black">{{ auth.fullName || 'Студент' }}</h1>
      <p class="mt-2 text-ink/60">{{ auth.email }}</p>
    </header>

    <p v-if="resultsError || learningError || assignmentsError || notificationsError" class="status-error">Не удалось загрузить актуальные данные. Попробуйте обновить страницу.</p>

    <section id="notifications" class="surface overflow-hidden">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line p-5">
        <h2 class="flex items-center gap-2 text-2xl font-black">
          <Bell class="size-5 text-ocean" aria-hidden="true" />
          Уведомления
        </h2>
        <span v-if="unreadNotifications" class="rounded-md bg-coral/10 px-2.5 py-1 text-xs font-black text-coral">{{ unreadNotifications }} новых</span>
      </div>
      <div v-if="notifications?.length" class="divide-y divide-line">
        <article v-for="notification in notifications.slice(0, 5)" :key="notification.id" class="flex flex-wrap items-center justify-between gap-4 p-5" :class="notification.read ? 'opacity-70' : ''">
          <div>
            <h3 class="font-black">{{ notification.title }}</h3>
            <p class="mt-1 text-sm text-ink/65">{{ notification.message }}</p>
            <p class="mt-1 text-xs font-bold text-ink/50">{{ formatDate(notification.createdAt) }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <NuxtLink v-if="notification.link" :to="notification.link" class="button-primary" @click="markRead(notification)">Открыть</NuxtLink>
            <button v-if="!notification.read" type="button" class="button-secondary" @click="markRead(notification)">Прочитано</button>
          </div>
        </article>
      </div>
      <p v-else class="p-5 text-sm text-ink/60">Уведомлений пока нет.</p>
    </section>

    <section class="surface overflow-hidden">
      <div class="border-b border-line p-5">
        <h2 class="text-2xl font-black">Назначенные тесты</h2>
      </div>
      <div v-if="assignments?.length" class="divide-y divide-line">
        <article v-for="assignment in assignments" :key="assignment.assignmentId" class="flex flex-wrap items-center justify-between gap-4 p-5">
          <div>
            <span class="rounded-md px-2.5 py-1 text-xs font-black" :class="assignment.status === 'COMPLETED' ? 'bg-mint/10 text-mint' : 'bg-gold/15 text-gold'">
              {{ assignment.status === 'COMPLETED' ? 'пройден' : 'назначен' }}
            </span>
            <h3 class="mt-3 font-black">{{ assignment.title }}</h3>
            <p class="text-sm text-ink/60">{{ assignment.subject }} · {{ assignment.timeLimitMinutes }} мин · назначен {{ formatDate(assignment.assignedAt) }}</p>
          </div>
          <NuxtLink :to="`/tests/${assignment.slug}`" class="button-primary">
            <CheckCircle2 v-if="assignment.status === 'COMPLETED'" class="size-4" aria-hidden="true" />
            <ClipboardList v-else class="size-4" aria-hidden="true" />
            <span>{{ assignment.status === 'COMPLETED' ? 'Посмотреть' : 'Пройти' }}</span>
          </NuxtLink>
        </article>
      </div>
      <p v-else class="p-5 text-sm text-ink/60">Назначенные тесты появятся здесь.</p>
    </section>

    <section class="grid gap-5 md:grid-cols-4">
      <div class="surface p-5">
        <BookOpenCheck class="mb-3 size-6 text-ocean" aria-hidden="true" />
        <strong class="text-3xl">{{ learning?.completedLessons || 0 }}</strong>
        <span class="block text-sm text-ink/60">уроков пройдено</span>
      </div>
      <div class="surface p-5">
        <ClipboardList class="mb-3 size-6 text-mint" aria-hidden="true" />
        <strong class="text-3xl">{{ results?.length || 0 }}</strong>
        <span class="block text-sm text-ink/60">попыток завершено</span>
      </div>
      <div class="surface p-5">
        <Award class="mb-3 size-6 text-gold" aria-hidden="true" />
        <strong class="text-3xl">{{ average }}%</strong>
        <span class="block text-sm text-ink/60">средний результат</span>
      </div>
      <div class="surface p-5">
        <Layers3 class="mb-3 size-6 text-coral" aria-hidden="true" />
        <strong class="text-3xl">{{ learning?.totalLessons || 0 }}</strong>
        <span class="block text-sm text-ink/60">уроков доступно</span>
      </div>
    </section>

    <section class="grid gap-5 lg:grid-cols-[0.8fr_1.2fr]">
      <div class="surface p-5">
        <ProgressDonut :value="learning?.progressPercent || 0" label="Прогресс по урокам" />
        <NuxtLink to="/courses" class="button-secondary mt-5 w-full">Перейти к курсам</NuxtLink>
      </div>

      <div class="surface overflow-hidden">
        <div class="border-b border-line p-5">
          <h2 class="text-2xl font-black">Результаты тестов</h2>
        </div>
        <div v-if="results?.length" class="divide-y divide-line">
          <div v-for="result in results" :key="result.attemptId" class="flex flex-wrap items-center justify-between gap-3 p-5">
            <div>
              <h3 class="font-black">{{ result.testTitle }}</h3>
              <p class="text-sm text-ink/60">{{ formatDate(result.completedAt) }}</p>
            </div>
            <div class="text-right">
              <strong class="block text-xl">{{ result.score }} / {{ result.maxScore }}</strong>
              <span class="text-sm font-bold text-ink/60">{{ result.scorePercent }}%</span>
            </div>
          </div>
        </div>
        <p v-else class="p-5 text-sm text-ink/60">Завершенные попытки появятся здесь после прохождения теста.</p>
      </div>
    </section>
  </div>
</template>
