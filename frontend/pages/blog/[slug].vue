<script setup lang="ts">
import { Copy, Heart, MessageCircle, Send, Share2, Trash2 } from 'lucide-vue-next'
import type { BlogComment, BlogPost, BlogReaction } from '~/types/learning'

const route = useRoute()
const api = useApi()
const auth = useAuthStore()
const config = useRuntimeConfig()
const slug = computed(() => String(route.params.slug))
const { data: post, error } = await useAsyncData(`blog-${slug.value}`, () => api<BlogPost>(`/blog/${slug.value}`))
const { data: comments, refresh: refreshComments } = await useAsyncData(`blog-comments-${slug.value}`, () => api<BlogComment[]>(`/blog/${slug.value}/comments`))
const commentText = ref('')
const commentPending = ref(false)
const commentError = ref('')
const shareMessage = ref('')

if (!post.value) {
  throw createError({ statusCode: error.value?.statusCode || 404, statusMessage: 'Статья не найдена' })
}

const shareUrl = computed(() => {
  if (import.meta.client) return window.location.href
  return `${String(config.public.siteUrl).replace(/\/$/, '')}/blog/${post.value?.slug || slug.value}`
})
const shareTitle = computed(() => post.value?.title || 'Тест Магистратура')
const encodedShareUrl = computed(() => encodeURIComponent(shareUrl.value))
const encodedShareText = computed(() => encodeURIComponent(shareTitle.value))
const telegramShareUrl = computed(() => `https://t.me/share/url?url=${encodedShareUrl.value}&text=${encodedShareText.value}`)
const whatsappShareUrl = computed(() => `https://wa.me/?text=${encodedShareText.value}%20${encodedShareUrl.value}`)

const loginRedirect = computed(() => `/login?redirect=${encodeURIComponent(route.fullPath)}`)
const formatDate = (value: string) => new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

const toggleLike = async () => {
  if (!auth.isAuthenticated) {
    await navigateTo(loginRedirect.value)
    return
  }
  if (!post.value) return
  const reaction = await api<BlogReaction>(`/blog/${post.value.slug}/like`, { method: 'POST' })
  post.value.likesCount = reaction.likesCount
  post.value.likedByMe = reaction.likedByMe
}

const submitComment = async () => {
  if (!auth.isAuthenticated) {
    await navigateTo(loginRedirect.value)
    return
  }
  if (!commentText.value.trim()) return
  commentPending.value = true
  commentError.value = ''
  try {
    await api<BlogComment>(`/blog/${slug.value}/comments`, { method: 'POST', body: { content: commentText.value } })
    commentText.value = ''
    await refreshComments()
    if (post.value) post.value.commentsCount = comments.value?.length || post.value.commentsCount
  } catch (error) {
    commentError.value = apiErrorMessage(error)
  } finally {
    commentPending.value = false
  }
}

const removeComment = async (comment: BlogComment) => {
  commentError.value = ''
  try {
    await api(`/blog/comments/${comment.id}`, { method: 'DELETE' })
    await refreshComments()
    if (post.value) post.value.commentsCount = comments.value?.length || 0
  } catch (error) {
    commentError.value = apiErrorMessage(error)
  }
}

const shareNative = async () => {
  shareMessage.value = ''
  if (import.meta.client && navigator.share) {
    await navigator.share({ title: shareTitle.value, url: shareUrl.value })
    return
  }
  if (import.meta.client && navigator.clipboard) {
    await navigator.clipboard.writeText(shareUrl.value)
    shareMessage.value = 'Ссылка скопирована'
  }
}

const canDeleteComment = (comment: BlogComment) => auth.email === comment.authorEmail || auth.isTeacher || auth.isAdmin

usePageSeo({
  title: post.value.title,
  description: post.value.excerpt,
  path: `/blog/${post.value.slug}`,
  type: 'article',
  keywords: `${post.value.title}, подготовка к КТА, КТА магистратура, ${post.value.category}`,
  publishedTime: post.value.createdAt,
  modifiedTime: post.value.createdAt,
  schema: {
    '@context': 'https://schema.org',
    '@type': 'Article',
    mainEntityOfPage: `${String(config.public.siteUrl).replace(/\/$/, '')}/blog/${post.value.slug}`,
    headline: post.value.title,
    description: post.value.excerpt,
    datePublished: post.value.createdAt,
    dateModified: post.value.createdAt,
    inLanguage: 'ru',
    author: {
      '@type': 'Person',
      name: post.value.authorName || 'Тест Магистратура'
    },
    publisher: {
      '@type': 'Organization',
      name: 'Тест Магистратура',
      url: String(config.public.siteUrl).replace(/\/$/, '')
    }
  }
})
</script>

<template>
  <article v-if="post" class="page-shell max-w-3xl space-y-6">
    <header class="surface p-6 md:p-8">
      <p class="eyebrow">{{ post.category }}</p>
      <h1 class="mt-3 text-4xl font-black leading-tight">{{ post.title }}</h1>
      <p class="mt-4 text-lg leading-8 text-ink/68">{{ post.excerpt }}</p>
      <div class="mt-4 flex flex-wrap items-center gap-3 text-sm font-bold text-ink/55">
        <span>{{ post.readMinutes }} мин чтения</span>
        <span v-if="post.authorName">Автор: {{ post.authorName }}</span>
        <span class="flex items-center gap-1"><Heart class="size-4" aria-hidden="true" /> {{ post.likesCount }}</span>
        <span class="flex items-center gap-1"><MessageCircle class="size-4" aria-hidden="true" /> {{ post.commentsCount }}</span>
      </div>
    </header>

    <div class="surface whitespace-pre-line p-6 text-lg leading-8 text-ink/76 md:p-8">{{ post.content }}</div>

    <section class="surface p-5">
      <div class="flex flex-wrap items-center gap-2">
        <button type="button" class="button-secondary" :class="post.likedByMe ? 'border-coral text-coral' : ''" @click="toggleLike">
          <Heart class="size-4" :fill="post.likedByMe ? 'currentColor' : 'none'" aria-hidden="true" />
          <span>{{ post.likedByMe ? 'Лайк стоит' : 'Лайк' }}</span>
        </button>
        <button type="button" class="button-secondary" @click="shareNative">
          <Share2 class="size-4" aria-hidden="true" />
          <span>Поделиться</span>
        </button>
        <a :href="telegramShareUrl" target="_blank" rel="noopener" class="button-secondary">
          <Send class="size-4" aria-hidden="true" />
          <span>Telegram</span>
        </a>
        <a :href="whatsappShareUrl" target="_blank" rel="noopener" class="button-secondary">
          <Copy class="size-4" aria-hidden="true" />
          <span>WhatsApp</span>
        </a>
      </div>
      <p v-if="shareMessage" class="mt-3 text-sm font-bold text-mint">{{ shareMessage }}</p>
    </section>

    <section class="surface p-5">
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-2xl font-black">Комментарии</h2>
        <span class="text-sm font-bold text-ink/60">{{ comments?.length || 0 }}</span>
      </div>

      <form class="mt-5 space-y-3" @submit.prevent="submitComment">
        <textarea v-model="commentText" class="input-field min-h-24" :placeholder="auth.isAuthenticated ? 'Написать комментарий' : 'Войдите, чтобы оставить комментарий'" :disabled="!auth.isAuthenticated" />
        <p v-if="commentError" class="status-error">{{ commentError }}</p>
        <div class="flex flex-wrap items-center gap-3">
          <button type="submit" class="button-primary" :disabled="commentPending || !auth.isAuthenticated || !commentText.trim()">
            <MessageCircle class="size-4" aria-hidden="true" />
            <span>{{ commentPending ? 'Отправляем...' : 'Отправить' }}</span>
          </button>
          <NuxtLink v-if="!auth.isAuthenticated" :to="loginRedirect" class="button-secondary">Войти</NuxtLink>
        </div>
      </form>

      <div v-if="comments?.length" class="mt-6 divide-y divide-line">
        <article v-for="comment in comments" :key="comment.id" class="py-4">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="font-black">{{ comment.authorName }}</h3>
              <p class="text-xs font-bold text-ink/55">{{ comment.authorRole }} · {{ formatDate(comment.createdAt) }}</p>
            </div>
            <button v-if="canDeleteComment(comment)" type="button" class="grid size-9 place-items-center rounded-md border border-line text-coral" title="Удалить" @click="removeComment(comment)">
              <Trash2 class="size-4" aria-hidden="true" />
            </button>
          </div>
          <p class="mt-3 whitespace-pre-line text-sm leading-6 text-ink/72">{{ comment.content }}</p>
        </article>
      </div>
      <p v-else class="mt-5 text-sm font-semibold text-ink/60">Комментариев пока нет.</p>
    </section>
  </article>
</template>
