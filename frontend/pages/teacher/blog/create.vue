<script setup lang="ts">
import type { BlogPost } from '~/types/learning'

const handleSaved = (post: BlogPost) => navigateTo(`/teacher/blog/${post.id}/edit`)

usePageSeo({
  title: 'Создать статью',
  description: 'Новая статья блога учителя.',
  path: '/teacher/blog/create',
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header>
      <p class="eyebrow">Блог</p>
      <h1 class="mt-2 text-4xl font-black">Создать статью</h1>
    </header>
    <TeacherBlogForm @saved="handleSaved" />
  </div>
</template>
