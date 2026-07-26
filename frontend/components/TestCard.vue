<script setup lang="ts">
import { BarChart3, Clock3, FilePenLine, Send, Trash2, UsersRound } from 'lucide-vue-next'
import type { Test, TestSummary } from '~/types/learning'

type TestCardData = TestSummary & Partial<Pick<Test, 'description'>>

const props = withDefaults(defineProps<{
  test: TestCardData
  teacher?: boolean
}>(), {
  teacher: false
})

const emit = defineEmits<{
  delete: [test: TestCardData]
}>()

const primaryLink = computed(() => props.teacher ? `/teacher/tests/${props.test.id}/edit` : `/tests/${props.test.slug}`)
</script>

<template>
  <article class="surface flex h-full flex-col p-5">
    <div class="flex items-start justify-between gap-3">
      <div>
        <span class="rounded-md bg-gold/15 px-2.5 py-1 text-xs font-black text-gold">{{ test.subject }}</span>
        <h2 class="mt-4 text-xl font-black">{{ test.title }}</h2>
      </div>
      <span
        class="rounded-md px-2.5 py-1 text-xs font-black"
        :class="test.status === 'PUBLISHED' ? 'bg-mint/10 text-mint' : 'bg-coral/10 text-coral'"
      >
        {{ test.status === 'PUBLISHED' ? 'опубликован' : 'черновик' }}
      </span>
    </div>

    <p v-if="test.description" class="mt-3 text-sm leading-6 text-ink/68">{{ test.description }}</p>

    <div class="mt-5 grid grid-cols-3 gap-2 text-sm">
      <div class="rounded-lg border border-line bg-paper p-3">
        <Clock3 class="mb-2 size-4 text-ocean" aria-hidden="true" />
        <strong>{{ test.timeLimitMinutes }}</strong>
        <span class="block text-xs text-ink/58">мин</span>
      </div>
      <div class="rounded-lg border border-line bg-paper p-3">
        <UsersRound class="mb-2 size-4 text-mint" aria-hidden="true" />
        <strong>{{ test.attempts }}</strong>
        <span class="block text-xs text-ink/58">попыток</span>
      </div>
      <div class="rounded-lg border border-line bg-paper p-3">
        <FilePenLine class="mb-2 size-4 text-coral" aria-hidden="true" />
        <strong>{{ test.averageScore }}%</strong>
        <span class="block text-xs text-ink/58">средний</span>
      </div>
    </div>

    <div class="mt-auto flex flex-wrap gap-2 pt-5">
      <NuxtLink :to="primaryLink" class="button-primary flex-1">
        {{ teacher ? 'Редактировать' : 'Открыть тест' }}
      </NuxtLink>
      <NuxtLink v-if="teacher" :to="`/teacher/tests/${test.id}/results`" class="button-secondary px-3" title="Статистика">
        <BarChart3 class="size-4" aria-hidden="true" />
        <span class="hidden sm:inline">Статистика</span>
      </NuxtLink>
      <NuxtLink v-if="teacher" :to="`/teacher/tests/${test.id}/assign`" class="button-secondary px-3" title="Назначить студенту">
        <Send class="size-4" aria-hidden="true" />
        <span>Назначить студенту</span>
      </NuxtLink>
      <button v-if="teacher" type="button" class="button-danger px-3" title="Удалить тест" @click="emit('delete', test)">
        <Trash2 class="size-4" aria-hidden="true" />
        <span class="hidden sm:inline">Удалить</span>
      </button>
    </div>
  </article>
</template>
