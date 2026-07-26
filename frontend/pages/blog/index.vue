<script setup lang="ts">
import { ArrowRight, Heart, MessageCircle } from 'lucide-vue-next'
import type { BlogPost } from '~/types/learning'

const api = useApi()
const { data: posts, error, pending } = await useAsyncData('blog-posts', () => api<BlogPost[]>('/blog'))
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')
const publishedPosts = computed(() => posts.value || [])

usePageSeo({
  title: 'Блог о КТА и поступлении в магистратуру',
  description: 'Полезные статьи о подготовке к КТА в магистратуру: логика, английский, профильные предметы, баллы, пробные тесты и разбор ошибок.',
  path: '/blog',
  keywords: 'блог КТА, подготовка к КТА магистратура, логическое мышление КТА, английский КТА, пробные тесты КТА',
  schema: {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    name: 'Блог о КТА',
    itemListElement: publishedPosts.value.map((post, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      url: `${siteUrl}/blog/${post.slug}`,
      name: post.title
    }))
  }
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="surface flex flex-wrap items-end justify-between gap-4 p-6 md:p-8">
      <div>
        <p class="eyebrow">Материалы</p>
        <h1 class="mt-2 text-4xl font-black">Блог</h1>
        <p class="mt-3 max-w-2xl text-ink/65">Разборы подготовки, поступления и стратегии прохождения тестов от команды платформы.</p>
      </div>
      <NuxtLink to="/tests" class="button-secondary">Открыть тесты</NuxtLink>
    </header>

    <p v-if="pending" class="text-sm font-semibold text-ink/60">Загружаем статьи...</p>
    <p v-else-if="error" class="status-error">Не удалось загрузить статьи. Попробуйте обновить страницу.</p>
    <section v-else-if="posts?.length" class="grid gap-5 md:grid-cols-3">
      <article v-for="post in posts" :key="post.id" class="surface flex flex-col p-5">
        <span class="w-fit rounded-md bg-ocean/10 px-2.5 py-1 text-xs font-black text-ocean">{{ post.category }}</span>
        <h2 class="mt-4 text-xl font-black">{{ post.title }}</h2>
        <p class="mt-3 flex-1 text-sm leading-6 text-ink/68">{{ post.excerpt }}</p>
        <div class="mt-5 flex items-center justify-between gap-3">
          <div class="flex items-center gap-3 text-xs font-black text-ink/55">
            <span>{{ post.readMinutes }} мин</span>
            <span class="flex items-center gap-1"><Heart class="size-4" aria-hidden="true" /> {{ post.likesCount }}</span>
            <span class="flex items-center gap-1"><MessageCircle class="size-4" aria-hidden="true" /> {{ post.commentsCount }}</span>
          </div>
          <NuxtLink :to="`/blog/${post.slug}`" class="flex items-center gap-2 font-black text-ocean">
            <span>Читать</span>
            <ArrowRight class="size-4" aria-hidden="true" />
          </NuxtLink>
        </div>
      </article>
    </section>
    <p v-else class="text-sm text-ink/60">Опубликованных статей пока нет.</p>
  </div>
</template>
