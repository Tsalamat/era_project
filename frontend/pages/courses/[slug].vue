<script setup lang="ts">
import { CheckCircle2, Clock3, FileText, PlayCircle, X } from 'lucide-vue-next'
import type { CourseDetails, LessonDetails } from '~/types/learning'

const route = useRoute()
const api = useApi()
const slug = computed(() => String(route.params.slug))
const { data: course, error, refresh } = await useAsyncData(`course-${slug.value}`, () => api<CourseDetails>(`/courses/${slug.value}`))
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')

if (!course.value) {
  throw createError({ statusCode: error.value?.statusCode || 404, statusMessage: 'Курс не найден' })
}

usePageSeo({
  title: course.value.title,
  description: course.value.description,
  path: `/courses/${course.value.slug}`,
  keywords: `${course.value.title}, ${course.value.subject}, курс КТА, подготовка к КТА в магистратуру, уроки КТА онлайн`,
  schema: {
    '@context': 'https://schema.org',
    '@type': 'Course',
    name: course.value.title,
    description: course.value.description,
    url: `${siteUrl}/courses/${course.value.slug}`,
    isAccessibleForFree: true,
    provider: {
      '@type': 'EducationalOrganization',
      name: 'Тест Магистратура',
      url: siteUrl
    }
  }
})

const activeLesson = ref<LessonDetails | null>(null)
const lessonPending = ref(false)
const lessonError = ref('')

const openLesson = async (id: string) => {
  lessonPending.value = true
  lessonError.value = ''
  try {
    activeLesson.value = await api<LessonDetails>(`/lessons/${id}`)
    await nextTick()
    document.querySelector('#lesson-content')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } catch (requestError) {
    lessonError.value = apiErrorMessage(requestError)
  } finally {
    lessonPending.value = false
  }
}

const completeLesson = async () => {
  if (!activeLesson.value || activeLesson.value.completed) return
  lessonPending.value = true
  lessonError.value = ''
  try {
    activeLesson.value = await api<LessonDetails>(`/lessons/${activeLesson.value.id}/complete`, { method: 'POST' })
    await refresh()
    clearNuxtData(['student-learning-summary', 'home-learning-summary', 'home-courses', 'published-courses'])
  } catch (requestError) {
    lessonError.value = apiErrorMessage(requestError)
  } finally {
    lessonPending.value = false
  }
}
</script>

<template>
  <div v-if="course" class="page-shell space-y-6">
    <section class="surface overflow-hidden">
      <div class="grid gap-0 md:grid-cols-[1fr_260px]">
        <div>
          <div class="p-6 md:p-8">
            <p class="eyebrow">{{ course.subject }}</p>
            <h1 class="mt-3 text-4xl font-black">{{ course.title }}</h1>
            <p class="mt-4 max-w-3xl text-lg leading-8 text-ink/68">{{ course.description }}</p>
            <div class="mt-6 flex flex-wrap items-center gap-3">
              <span class="border-2 border-line bg-surface px-4 py-3 text-sm font-black">
                {{ course.completedLessons }} из {{ course.lessonsCount }} уроков
              </span>
              <NuxtLink to="/tests" class="button-secondary">
                <FileText class="size-5" aria-hidden="true" />
                <span>Тесты</span>
              </NuxtLink>
            </div>
          </div>
        </div>
        <div class="border-t-2 border-line bg-gold p-6 text-ink md:border-l-2 md:border-t-0">
          <span class="block text-sm font-black uppercase">Курс</span>
          <strong class="mt-4 block text-6xl font-black leading-none">{{ course.modules.length }}</strong>
          <span class="mt-2 block text-sm font-black">модулей</span>
          <div class="mt-8 h-2 w-full bg-ink/20">
            <div class="h-2 bg-ink" :style="{ width: `${course.progressPercent}%` }" />
          </div>
          <span class="mt-2 block text-sm font-black">{{ course.progressPercent }}% прогресс</span>
        </div>
      </div>
    </section>

    <p v-if="lessonError" class="status-error" role="alert">{{ lessonError }}</p>

    <section class="space-y-5">
      <h2 class="text-2xl font-black">Программа курса</h2>
      <div v-for="module in course.modules" :key="module.id" class="space-y-3">
        <h3 class="text-lg font-black">{{ module.orderNumber }}. {{ module.title }}</h3>
        <article v-for="lesson in module.lessons" :key="lesson.id" class="surface flex flex-wrap items-center justify-between gap-4 p-4">
          <div class="flex items-center gap-4">
            <span class="grid size-11 place-items-center rounded-lg" :class="lesson.completed ? 'bg-mint/10 text-mint' : 'bg-ocean/10 text-ocean'">
              <CheckCircle2 v-if="lesson.completed" class="size-5" aria-hidden="true" />
              <PlayCircle v-else class="size-5" aria-hidden="true" />
            </span>
            <div>
              <h4 class="font-black">{{ lesson.title }}</h4>
              <p class="mt-1 flex items-center gap-1 text-sm text-ink/60">
                <Clock3 class="size-4" aria-hidden="true" />
                <span>{{ lesson.minutes }} минут</span>
              </p>
            </div>
          </div>
          <button class="button-secondary" type="button" :disabled="lessonPending" @click="openLesson(lesson.id)">
            {{ lessonPending ? 'Загрузка...' : 'Открыть урок' }}
          </button>
        </article>
      </div>
    </section>

    <section v-if="activeLesson" id="lesson-content" class="surface scroll-mt-24 p-6 md:p-8">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="eyebrow">{{ activeLesson.moduleTitle }}</p>
          <h2 class="mt-2 text-3xl font-black">{{ activeLesson.title }}</h2>
        </div>
        <button type="button" class="grid size-10 shrink-0 place-items-center rounded-lg border border-line" title="Закрыть урок" @click="activeLesson = null">
          <X class="size-5" aria-hidden="true" />
        </button>
      </div>
      <p class="mt-5 whitespace-pre-line text-lg leading-8 text-ink/75">{{ activeLesson.content }}</p>
      <button type="button" class="button-primary mt-6" :disabled="lessonPending || activeLesson.completed" @click="completeLesson">
        <CheckCircle2 class="size-5" aria-hidden="true" />
        <span>{{ activeLesson.completed ? 'Урок завершен' : lessonPending ? 'Сохраняем...' : 'Завершить урок' }}</span>
      </button>
    </section>
  </div>
</template>
