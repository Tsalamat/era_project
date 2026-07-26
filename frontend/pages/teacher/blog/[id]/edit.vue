<script setup lang="ts">
import type { BlogPost } from '~/types/learning'

const route = useRoute()
const api = useApi()
const id = computed(() => String(route.params.id))
const { data: post, error } = await useAsyncData(`teacher-blog-${id.value}`, () => api<BlogPost>(`/teacher/blog/${id.value}`))
const savedMessage = ref('')

if (!post.value) {
  throw createError({ statusCode: error.value?.statusCode || 404, statusMessage: 'Статья не найдена' })
}

const handleSaved = (saved: BlogPost) => {
  post.value = saved
  savedMessage.value = 'Статья сохранена'
}

usePageSeo({
  title: 'Редактировать статью',
  description: 'Редактирование статьи блога учителя.',
  path: `/teacher/blog/${id.value}/edit`,
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div v-if="post" class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Блог</p>
        <h1 class="mt-2 text-4xl font-black">Редактировать статью</h1>
      </div>
      <NuxtLink v-if="post.status === 'PUBLISHED'" :to="`/blog/${post.slug}`" class="button-secondary">Открыть статью</NuxtLink>
    </header>
    <p v-if="savedMessage" class="status-success">{{ savedMessage }}</p>
    <TeacherBlogForm :post="post" @saved="handleSaved" />
  </div>
</template>
