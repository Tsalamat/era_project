<script setup lang="ts">
import { BarChart3, Send, Trash2 } from 'lucide-vue-next'
import type { TeacherTest } from '~/types/learning'

const route = useRoute()
const api = useApi()
const id = String(route.params.id)
const { data, error, refresh } = await useAsyncData(`teacher-test-${id}`, () => api<TeacherTest>(`/teacher/tests/${id}`))
const actionError = ref('')
const deleting = ref(false)

const deleteTest = async () => {
  if (!data.value || !import.meta.client || deleting.value) return
  const confirmed = window.confirm(`Удалить тест "${data.value.title}"? Результаты и вопросы этого теста тоже будут удалены.`)
  if (!confirmed) return

  deleting.value = true
  actionError.value = ''
  try {
    await api(`/teacher/tests/${id}`, { method: 'DELETE' })
    await navigateTo('/teacher/tests')
  } catch (error) {
    actionError.value = apiErrorMessage(error)
  } finally {
    deleting.value = false
  }
}

usePageSeo({
  title: 'Редактирование теста',
  description: 'Редактирование теста, вопросов, ответов и объяснений.',
  path: `/teacher/tests/${id}/edit`,
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Редактор</p>
        <h1 class="mt-2 text-4xl font-black">Редактирование теста</h1>
      </div>
      <div v-if="data" class="flex flex-wrap gap-2">
        <NuxtLink :to="`/teacher/tests/${id}/assign`" class="button-secondary">
          <Send class="size-4" aria-hidden="true" />
          <span>Назначить студенту</span>
        </NuxtLink>
        <NuxtLink :to="`/teacher/tests/${id}/results`" class="button-secondary">
          <BarChart3 class="size-4" aria-hidden="true" />
          <span>Статистика</span>
        </NuxtLink>
        <button type="button" class="button-danger" :disabled="deleting" @click="deleteTest">
          <Trash2 class="size-4" aria-hidden="true" />
          <span>{{ deleting ? 'Удаляем...' : 'Удалить тест' }}</span>
        </button>
      </div>
    </header>
    <p v-if="error" class="status-error">{{ apiErrorMessage(error) }}</p>
    <p v-if="actionError" class="status-error">{{ actionError }}</p>
    <TeacherTestForm v-else-if="data" :initial-test="data" @saved="() => refresh()" />
  </div>
</template>
