<script setup lang="ts">
import type { Course } from '~/types/learning'

const api = useApi()
const { data: courses, error, pending } = await useAsyncData('published-courses', () => api<Course[]>('/courses'))
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')

usePageSeo({
  title: 'Бесплатные курсы КТА для магистратуры',
  description: 'Бесплатные онлайн-курсы для подготовки к КТА в магистратуру: уроки по логике, английскому, профильным предметам и стратегии поступления.',
  path: '/courses',
  keywords: 'бесплатные курсы КТА, курсы КТА магистратура, уроки КТА онлайн, подготовка к магистратуре',
  schema: courses.value?.length
    ? {
        '@context': 'https://schema.org',
        '@type': 'ItemList',
        itemListElement: courses.value.map((course, index) => ({
          '@type': 'ListItem',
          position: index + 1,
          url: `${siteUrl}/courses/${course.slug}`,
          item: {
            '@type': 'Course',
            name: course.title,
            description: course.description,
            url: `${siteUrl}/courses/${course.slug}`,
            provider: {
              '@type': 'EducationalOrganization',
              name: 'Тест Магистратура',
              url: siteUrl
            }
          }
        }))
      }
    : undefined
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="surface flex flex-wrap items-end justify-between gap-4 p-6 md:p-8">
      <div>
        <p class="eyebrow">Каталог</p>
        <h1 class="mt-2 text-4xl font-black">Бесплатные курсы</h1>
        <p class="mt-3 max-w-2xl text-ink/65">Структурированные материалы по КТА: уроки открываются бесплатно, а прогресс считается после входа.</p>
      </div>
      <NuxtLink to="/tests" class="button-secondary">Перейти к тестам</NuxtLink>
    </header>

    <p v-if="pending" class="text-sm font-semibold text-ink/60">Загружаем курсы...</p>
    <p v-else-if="error" class="status-error">Не удалось загрузить курсы. Попробуйте обновить страницу.</p>
    <div v-else-if="courses?.length" class="grid gap-5 lg:grid-cols-2">
      <CourseCard v-for="course in courses" :key="course.id" :course="course" />
    </div>
    <p v-else class="text-sm text-ink/60">Опубликованных курсов пока нет.</p>
  </div>
</template>
