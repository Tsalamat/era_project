<script setup lang="ts">
import { AlertTriangle, BarChart3, CheckCircle2, Clock3, Download, ListChecks, Play, Send, TimerReset, XCircle } from 'lucide-vue-next'
import type { StudentTest, TestAttempt, TestResult } from '~/types/learning'

const route = useRoute()
const auth = useAuthStore()
const api = useApi()
const apiFile = useApiFile()
const slug = computed(() => String(route.params.slug))
const { data, error } = await useAsyncData(`test-${slug.value}`, () => api<StudentTest>(`/tests/${slug.value}`))
const test = computed(() => data.value)
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')

if (!test.value) {
  throw createError({ statusCode: error.value?.statusCode || 404, statusMessage: 'Тест не найден' })
}

usePageSeo({
  title: test.value.title,
  description: test.value.description,
  path: `/tests/${test.value.slug}`,
  keywords: `${test.value.title}, ${test.value.subject}, пробный тест КТА, КТА магистратура онлайн, подготовка к КТА`,
  schema: {
    '@context': 'https://schema.org',
    '@type': 'Quiz',
    name: test.value.title,
    description: test.value.description,
    url: `${siteUrl}/tests/${test.value.slug}`,
    educationalLevel: 'graduate',
    assesses: test.value.subject,
    timeRequired: `PT${test.value.timeLimitMinutes}M`,
    isAccessibleForFree: true
  }
})

const selected = ref<Record<string, string[]>>({})
const attempt = ref<TestAttempt | null>(null)
const result = ref<TestResult | null>(null)
const secondsLeft = ref(test.value.timeLimitMinutes * 60)
const pending = ref(false)
const downloadingReport = ref(false)
const errorMessage = ref('')
let timer: ReturnType<typeof setInterval> | undefined

const formattedTime = computed(() => `${Math.floor(secondsLeft.value / 60).toString().padStart(2, '0')}:${(secondsLeft.value % 60).toString().padStart(2, '0')}`)
const answeredCount = computed(() => Object.values(selected.value).filter(answer => answer.length > 0).length)
const resultLevelClass = computed(() => {
  const score = result.value?.scorePercent || 0
  if (score >= 70) return 'text-mint'
  if (score >= 50) return 'text-gold'
  return 'text-coral'
})

const startTest = async () => {
  if (!auth.isAuthenticated) {
    await navigateTo(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  pending.value = true
  errorMessage.value = ''
  try {
    attempt.value = await api<TestAttempt>(`/tests/${test.value!.id}/start`, { method: 'POST' })
    timer = setInterval(() => {
      secondsLeft.value = Math.max(0, secondsLeft.value - 1)
      if (secondsLeft.value === 0) {
        clearInterval(timer)
        void submitTest()
      }
    }, 1000)
  } catch (requestError) {
    errorMessage.value = apiErrorMessage(requestError)
  } finally {
    pending.value = false
  }
}

const choose = (questionId: string, optionId: string, multiple: boolean) => {
  if (result.value) return
  if (!multiple) {
    selected.value[questionId] = [optionId]
    return
  }
  const current = selected.value[questionId] || []
  selected.value[questionId] = current.includes(optionId)
    ? current.filter(id => id !== optionId)
    : [...current, optionId]
}

const submitTest = async () => {
  if (!attempt.value || result.value || pending.value) return
  pending.value = true
  errorMessage.value = ''
  if (timer) clearInterval(timer)
  try {
    result.value = await api<TestResult>(`/tests/${test.value!.id}/submit`, {
      method: 'POST',
      body: {
        attemptId: attempt.value.id,
        answers: test.value!.questions.map(question => ({
          questionId: question.id,
          answerOptionIds: selected.value[question.id] || []
        }))
      }
    })
  } catch (requestError) {
    errorMessage.value = apiErrorMessage(requestError)
  } finally {
    pending.value = false
  }
}

const downloadStudentReport = async () => {
  if (!test.value || downloadingReport.value) return
  downloadingReport.value = true
  errorMessage.value = ''
  try {
    await apiFile.download(`/tests/${test.value.id}/result/docx`, `analytics-${test.value.slug}.docx`)
  } catch (requestError) {
    errorMessage.value = apiErrorMessage(requestError)
  } finally {
    downloadingReport.value = false
  }
}

const questionResult = (questionId: string) => result.value?.questions.find(item => item.questionId === questionId)

const optionState = (questionId: string, optionId: string) => {
  const current = questionResult(questionId)
  if (!current) return selected.value[questionId]?.includes(optionId) ? 'selected' : 'idle'
  if (current.correctOptionIds.includes(optionId)) return 'correct'
  if (current.selectedOptionIds.includes(optionId)) return 'wrong'
  return 'idle'
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div v-if="test" class="page-shell space-y-6">
    <section class="surface p-6 md:p-8">
      <div class="grid gap-5 md:grid-cols-[1fr_auto]">
        <div>
          <p class="eyebrow">{{ test.subject }}</p>
          <h1 class="mt-2 max-w-3xl text-4xl font-black leading-tight">{{ test.title }}</h1>
          <p class="mt-3 max-w-2xl text-lg leading-8 text-ink/68">{{ test.description }}</p>
        </div>
        <div class="grid gap-3 sm:min-w-64">
          <div class="rounded-lg border border-line bg-paper px-4 py-3">
            <Clock3 class="mb-2 size-5 text-ocean" aria-hidden="true" />
            <strong class="text-2xl">{{ attempt && !result ? formattedTime : `${test.timeLimitMinutes} мин` }}</strong>
            <span class="block text-xs font-bold text-ink/55">время теста</span>
          </div>
          <div class="rounded-lg border border-line bg-paper px-4 py-3">
            <ListChecks class="mb-2 size-5 text-mint" aria-hidden="true" />
            <strong class="text-2xl">{{ answeredCount }}/{{ test.questions.length }}</strong>
            <span class="block text-xs font-bold text-ink/55">ответов выбрано</span>
          </div>
        </div>
      </div>
    </section>

    <p v-if="errorMessage" class="status-error" role="alert">{{ errorMessage }}</p>

    <section v-if="!attempt" class="surface p-6 md:p-8">
      <div class="mx-auto max-w-2xl text-center">
        <Play class="mx-auto size-9 text-ocean" aria-hidden="true" />
        <h2 class="mt-3 text-2xl font-black">{{ test.questions.length }} вопросов</h2>
        <p class="mt-2 text-ink/65">Обычный учебный режим: только вопросы, таймер и результат. После завершения вы получите аналитику и объяснения ошибок.</p>
        <button type="button" class="button-primary mt-5" :disabled="pending || !test.questions.length" @click="startTest">
          <Play class="size-4" aria-hidden="true" />
          <span>{{ pending ? 'Запускаем...' : 'Начать тест' }}</span>
        </button>
      </div>
    </section>

    <template v-else>
      <section v-if="result" class="surface p-6 md:p-8">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p class="eyebrow">Аналитика результата</p>
            <div class="mt-2 flex flex-wrap items-end gap-4">
              <strong class="text-5xl font-black" :class="resultLevelClass">{{ result.scorePercent }}%</strong>
              <span class="pb-1 text-lg font-bold text-ink/65">{{ result.score }} из {{ result.maxScore }}</span>
            </div>
            <p class="mt-3 text-xl font-black">{{ result.analytics.level }}</p>
            <p class="mt-2 max-w-3xl text-sm font-semibold leading-6 text-ink/65">{{ result.analytics.recommendation }}</p>
          </div>
          <button type="button" class="button-secondary" :disabled="downloadingReport" @click="downloadStudentReport">
            <Download class="size-4" aria-hidden="true" />
            <span>{{ downloadingReport ? 'Готовим...' : 'Скачать Word' }}</span>
          </button>
        </div>

        <div class="mt-6 grid gap-3 md:grid-cols-5">
          <div class="rounded-lg border border-line bg-paper p-4">
            <BarChart3 class="mb-2 size-5 text-ocean" aria-hidden="true" />
            <strong class="text-2xl">{{ result.analytics.accuracyPercent }}%</strong>
            <span class="block text-xs font-bold text-ink/55">точность</span>
          </div>
          <div class="rounded-lg border border-line bg-paper p-4">
            <ListChecks class="mb-2 size-5 text-mint" aria-hidden="true" />
            <strong class="text-2xl">{{ result.analytics.answeredQuestions }}</strong>
            <span class="block text-xs font-bold text-ink/55">ответил</span>
          </div>
          <div class="rounded-lg border border-line bg-paper p-4">
            <AlertTriangle class="mb-2 size-5 text-gold" aria-hidden="true" />
            <strong class="text-2xl">{{ result.analytics.skippedQuestions }}</strong>
            <span class="block text-xs font-bold text-ink/55">не ответил</span>
          </div>
          <div class="rounded-lg border border-line bg-paper p-4">
            <CheckCircle2 class="mb-2 size-5 text-mint" aria-hidden="true" />
            <strong class="text-2xl">{{ result.analytics.correctAnswers }}</strong>
            <span class="block text-xs font-bold text-ink/55">правильно</span>
          </div>
          <div class="rounded-lg border border-line bg-paper p-4">
            <TimerReset class="mb-2 size-5 text-coral" aria-hidden="true" />
            <strong class="text-2xl">{{ result.analytics.durationMinutes }}</strong>
            <span class="block text-xs font-bold text-ink/55">минут</span>
          </div>
        </div>
      </section>

      <section class="space-y-4">
        <article v-for="question in test.questions" :key="question.id" class="surface p-5">
          <div class="flex items-start justify-between gap-3">
            <h2 class="text-xl font-black">{{ question.orderNumber }}. {{ question.questionText }}</h2>
            <CheckCircle2 v-if="questionResult(question.id)?.correct" class="size-6 shrink-0 text-mint" aria-hidden="true" />
            <XCircle v-else-if="result" class="size-6 shrink-0 text-coral" aria-hidden="true" />
          </div>
          <div class="mt-4 grid gap-3 md:grid-cols-2">
            <button
              v-for="option in question.options"
              :key="option.id"
              type="button"
              class="flex min-h-14 items-center justify-between rounded-lg border p-4 text-left font-semibold transition"
              :class="{
                'border-ocean bg-ocean/10': optionState(question.id, option.id) === 'selected',
                'border-mint bg-mint/10': optionState(question.id, option.id) === 'correct',
                'border-coral bg-coral/10': optionState(question.id, option.id) === 'wrong',
                'border-line bg-surface hover:border-ocean': optionState(question.id, option.id) === 'idle' && !result
              }"
              @click="choose(question.id, option.id, question.questionType === 'MULTIPLE_CHOICE')"
            >
              <span>{{ option.optionText }}</span>
              <CheckCircle2 v-if="selected[question.id]?.includes(option.id)" class="size-5 shrink-0 text-ocean" aria-hidden="true" />
            </button>
          </div>
          <p v-if="result" class="mt-4 rounded-lg bg-paper p-4 text-sm font-semibold leading-6 text-ink/72">
            {{ questionResult(question.id)?.explanation }}
          </p>
        </article>
      </section>

      <div class="flex flex-wrap items-center gap-3">
        <button v-if="!result" class="button-primary" type="button" :disabled="pending" @click="submitTest">
          <Send class="size-4" aria-hidden="true" />
          <span>{{ pending ? 'Сохраняем...' : `Завершить · ${answeredCount}/${test.questions.length}` }}</span>
        </button>
        <NuxtLink v-else to="/cabinet" class="button-primary">Мои результаты</NuxtLink>
        <NuxtLink v-if="result" to="/tests" class="button-secondary">Другие тесты</NuxtLink>
      </div>
    </template>
  </div>
</template>
