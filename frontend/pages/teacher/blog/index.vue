<script setup lang="ts">
import { FilePlus2, Pencil, Trash2 } from 'lucide-vue-next'
import type { BlogPost } from '~/types/learning'

const api = useApi()
const { data: posts, error, pending, refresh } = await useAsyncData('teacher-blog-posts', () => api<BlogPost[]>('/teacher/blog'))
const actionError = ref('')

const removePost = async (post: BlogPost) => {
  if (!confirm(`Удалить статью "${post.title}"?`)) return
  actionError.value = ''
  try {
    await api(`/teacher/blog/${post.id}`, { method: 'DELETE' })
    await refresh()
  } catch (error) {
    actionError.value = apiErrorMessage(error)
  }
}

usePageSeo({
  title: 'Блог учителя',
  description: 'Создание и редактирование статей для блога Тест Магистратура.',
  path: '/teacher/blog',
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Учитель</p>
        <h1 class="mt-2 text-4xl font-black">Блог</h1>
      </div>
      <NuxtLink to="/teacher/blog/create" class="button-primary">
        <FilePlus2 class="size-5" aria-hidden="true" />
        <span>Создать статью</span>
      </NuxtLink>
    </header>

    <p v-if="pending" class="text-sm font-semibold text-ink/60">Загружаем статьи...</p>
    <p v-if="error" class="status-error">{{ apiErrorMessage(error) }}</p>
    <p v-if="actionError" class="status-error">{{ actionError }}</p>

    <section v-if="posts?.length" class="surface overflow-hidden">
      <div class="divide-y divide-line">
        <article v-for="post in posts" :key="post.id" class="flex flex-wrap items-center justify-between gap-4 p-5">
          <div>
            <span class="rounded-md px-2.5 py-1 text-xs font-black" :class="post.status === 'PUBLISHED' ? 'bg-mint/10 text-mint' : 'bg-coral/10 text-coral'">
              {{ post.status === 'PUBLISHED' ? 'опубликована' : 'черновик' }}
            </span>
            <h2 class="mt-3 text-xl font-black">{{ post.title }}</h2>
            <p class="mt-1 text-sm text-ink/60">{{ post.category }} · {{ post.readMinutes }} мин · {{ post.likesCount }} лайков · {{ post.commentsCount }} комм.</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <NuxtLink :to="`/teacher/blog/${post.id}/edit`" class="button-secondary">
              <Pencil class="size-4" aria-hidden="true" />
              <span>Редактировать</span>
            </NuxtLink>
            <NuxtLink v-if="post.status === 'PUBLISHED'" :to="`/blog/${post.slug}`" class="button-secondary">Открыть</NuxtLink>
            <button type="button" class="grid size-11 place-items-center rounded-lg border border-line text-coral hover:bg-coral/10" title="Удалить" @click="removePost(post)">
              <Trash2 class="size-4" aria-hidden="true" />
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="!pending && !error" class="surface p-8 text-center">
      <h2 class="text-2xl font-black">Статей пока нет</h2>
      <NuxtLink to="/teacher/blog/create" class="button-primary mt-4">Создать первую статью</NuxtLink>
    </section>
  </div>
</template>
