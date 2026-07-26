<script setup lang="ts">
import { ArrowRight, BookOpenCheck, ClipboardCheck, GraduationCap, Newspaper, ShieldCheck, TrendingUp, UserPlus, UserRound } from 'lucide-vue-next'
import type { Course, LearningSummary, TestSummary } from '~/types/learning'

const auth = useAuthStore()
const api = useApi()
const { data: courses, error: coursesError } = await useAsyncData('home-courses', () => api<Course[]>('/courses'))
const { data: tests, error: testsError } = await useAsyncData('home-published-tests', () => api<TestSummary[]>('/tests'))
const { data: learning } = await useAsyncData('home-learning-summary', () => auth.isAuthenticated
  ? api<LearningSummary>('/users/me/learning-summary')
  : Promise.resolve({ totalLessons: 0, completedLessons: 0, progressPercent: 0 }))
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')

usePageSeo({
  title: 'Подготовка к КТА в магистратуру онлайн',
  description: 'Бесплатная подготовка к КТА в магистратуру: курсы, уроки, пробные тесты КТА, объяснения ошибок и личный прогресс после регистрации.',
  path: '/',
  keywords: 'подготовка к КТА, КТА магистратура, пробные тесты КТА, курсы КТА, поступление в магистратуру Казахстан',
  schema: {
    '@context': 'https://schema.org',
    '@graph': [
      {
        '@type': 'EducationalOrganization',
        name: 'Тест Магистратура',
        url: siteUrl,
        description: 'Бесплатная образовательная платформа для подготовки к КТА в магистратуру.',
        sameAs: [`${siteUrl}/courses`, `${siteUrl}/tests`, `${siteUrl}/blog`]
      },
      {
        '@type': 'WebSite',
        name: 'Тест Магистратура',
        url: siteUrl,
        inLanguage: 'ru'
      }
    ]
  }
})

const publishedTests = computed(() => tests.value || [])
const publishedCourses = computed(() => courses.value || [])
const totalLessons = computed(() => publishedCourses.value.reduce((sum, course) => sum + course.lessonsCount, 0))
</script>

<template>
  <div class="page-shell space-y-8">
    <section class="grid gap-5 lg:grid-cols-[1.05fr_0.95fr]">
      <div class="surface bg-ink p-6 text-paper md:p-10">
        <p class="text-sm font-black uppercase text-gold">Тест Магистратура</p>
        <h1 class="mt-4 max-w-4xl text-5xl font-black leading-none md:text-7xl">КТА без шума</h1>
        <p class="mt-6 max-w-2xl text-lg font-semibold leading-8 text-paper/72">
          Бесплатные уроки, тесты, объяснения ошибок и реальный прогресс после регистрации.
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <NuxtLink to="/tests" class="button-primary border-paper bg-paper text-ink">
            <ClipboardCheck class="size-5" aria-hidden="true" />
            <span>Тесты</span>
          </NuxtLink>
          <NuxtLink to="/courses" class="button-secondary border-paper bg-ink text-paper">
            <GraduationCap class="size-5" aria-hidden="true" />
            <span>Курсы</span>
          </NuxtLink>
        </div>
      </div>

      <aside class="surface overflow-hidden">
        <div class="border-b-2 border-line bg-gold p-5 text-ink">
          <p class="text-sm font-black uppercase">Быстрый маршрут</p>
        </div>
        <div class="grid divide-y-2 divide-line">
          <NuxtLink to="/courses" class="flex items-center justify-between gap-4 p-5 transition hover:bg-gold/35">
            <span class="flex items-center gap-3 font-black"><GraduationCap class="size-5 text-ocean" aria-hidden="true" /> Курсы</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
          <NuxtLink to="/tests" class="flex items-center justify-between gap-4 p-5 transition hover:bg-gold/35">
            <span class="flex items-center gap-3 font-black"><ClipboardCheck class="size-5 text-mint" aria-hidden="true" /> Тесты</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
          <NuxtLink to="/blog" class="flex items-center justify-between gap-4 p-5 transition hover:bg-gold/35">
            <span class="flex items-center gap-3 font-black"><Newspaper class="size-5 text-coral" aria-hidden="true" /> Блог</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
          <NuxtLink v-if="auth.isAuthenticated" to="/cabinet" class="flex items-center justify-between gap-4 p-5 transition hover:bg-gold/35">
            <span class="flex items-center gap-3 font-black"><UserRound class="size-5 text-ocean" aria-hidden="true" /> Кабинет</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
          <NuxtLink v-if="auth.isTeacher" to="/teacher" class="flex items-center justify-between gap-4 p-5 transition hover:bg-gold/35">
            <span class="flex items-center gap-3 font-black"><ShieldCheck class="size-5 text-mint" aria-hidden="true" /> Учитель</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
          <NuxtLink v-if="!auth.isAuthenticated" to="/register" class="flex items-center justify-between gap-4 p-5 transition hover:bg-gold/35">
            <span class="flex items-center gap-3 font-black"><UserPlus class="size-5 text-ocean" aria-hidden="true" /> Регистрация</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
        </div>
      </aside>
    </section>

    <section class="grid gap-5 lg:grid-cols-[360px_1fr]">
      <div class="surface p-6">
        <p class="eyebrow">Личный темп</p>
        <div class="mt-5">
          <ProgressDonut v-if="auth.isAuthenticated" :value="learning?.progressPercent || 0" label="Прогресс по урокам" />
          <div v-else class="space-y-4">
            <UserPlus class="size-7 text-mint" aria-hidden="true" />
            <h2 class="text-xl font-black">Прогресс считается после регистрации</h2>
            <p class="text-sm leading-6 text-ink/62">Уроки, попытки и результаты берутся из backend, без нарисованных счетчиков.</p>
            <NuxtLink to="/register" class="button-primary">Зарегистрироваться</NuxtLink>
          </div>
        </div>
      </div>
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="surface p-6">
          <BookOpenCheck class="mb-5 size-6 text-ocean" aria-hidden="true" />
          <strong class="text-5xl">{{ totalLessons }}</strong>
          <span class="mt-2 block text-sm font-black uppercase text-ink/60">уроков в базе</span>
        </div>
        <div class="surface p-6">
          <TrendingUp class="mb-5 size-6 text-gold" aria-hidden="true" />
          <strong class="text-5xl">{{ publishedTests.length }}</strong>
          <span class="mt-2 block text-sm font-black uppercase text-ink/60">активных тестов</span>
        </div>
      </div>
    </section>

    <p v-if="coursesError || testsError" class="status-error">Не удалось загрузить актуальные материалы. Попробуйте обновить страницу.</p>

    <section class="space-y-4">
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-2xl font-black">Курсы</h2>
        <NuxtLink to="/courses" class="flex items-center gap-2 text-sm font-black text-ocean">
          <span>Все курсы</span>
          <ArrowRight class="size-4" aria-hidden="true" />
        </NuxtLink>
      </div>
      <div v-if="publishedCourses.length" class="grid gap-5 md:grid-cols-3">
        <CourseCard v-for="course in publishedCourses" :key="course.id" :course="course" />
      </div>
      <p v-else-if="!coursesError" class="text-sm text-ink/60">Опубликованных курсов пока нет.</p>
    </section>

    <section v-if="publishedTests.length" class="grid gap-5 lg:grid-cols-3">
      <TestCard v-for="test in publishedTests" :key="test.id" :test="test" />
    </section>
  </div>
</template>
