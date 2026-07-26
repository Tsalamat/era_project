<script setup lang="ts">
import { ArrowRight, BookOpenCheck, CheckCircle2 } from 'lucide-vue-next'
import type { Course } from '~/types/learning'

const props = defineProps<{
  course: Course
}>()

const auth = useAuthStore()
const progressWidth = computed(() => `${props.course.progressPercent}%`)
</script>

<template>
  <article class="surface grid h-full overflow-hidden md:grid-cols-[116px_1fr]">
    <div class="flex flex-col justify-between border-b-2 border-line bg-gold p-4 text-ink md:border-b-0 md:border-r-2">
      <BookOpenCheck class="size-7" aria-hidden="true" />
      <span class="mt-10 text-4xl font-black leading-none">{{ course.lessonsCount }}</span>
    </div>
    <div class="flex min-h-64 flex-col p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span class="border-2 border-line bg-paper px-2.5 py-1 text-xs font-black text-ocean">{{ course.subject }}</span>
        <span class="flex items-center gap-1 text-xs font-black text-mint">
          <CheckCircle2 class="size-4" aria-hidden="true" />
          open
        </span>
      </div>
      <h2 class="mt-4 text-xl font-black">{{ course.title }}</h2>
      <p class="mt-2 flex-1 text-sm leading-6 text-ink/68">{{ course.description }}</p>
      <div v-if="auth.isAuthenticated" class="mt-4 h-2 rounded-full bg-line">
        <div class="h-2 rounded-full bg-mint" :style="{ width: progressWidth }" />
      </div>
      <div class="mt-5 flex items-center justify-between gap-3 border-t-2 border-line pt-4">
        <span class="text-sm font-bold text-ink/62">
          {{ auth.isAuthenticated ? `${course.completedLessons} из ${course.lessonsCount}` : `${course.lessonsCount} уроков` }}
        </span>
        <NuxtLink :to="`/courses/${course.slug}`" class="button-secondary">
          <span>Открыть</span>
          <ArrowRight class="size-4" aria-hidden="true" />
        </NuxtLink>
      </div>
    </div>
  </article>
</template>
