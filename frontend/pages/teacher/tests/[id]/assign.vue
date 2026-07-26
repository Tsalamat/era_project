<script setup lang="ts">
import { BellPlus, CheckCircle2, UsersRound } from 'lucide-vue-next'
import type { TeacherTest, TestAssignment, UserProfile } from '~/types/learning'

const route = useRoute()
const api = useApi()
const id = computed(() => String(route.params.id))
const { data: test, error: testError } = await useAsyncData(`assign-test-${id.value}`, () => api<TeacherTest>(`/teacher/tests/${id.value}`))
const { data: students, error: studentsError } = await useAsyncData('teacher-students', () => api<UserProfile[]>('/teacher/students'))
const { data: assignments, error: assignmentsError, refresh } = await useAsyncData(`test-assignments-${id.value}`, () => api<TestAssignment[]>(`/teacher/tests/${id.value}/assignments`))
const assignmentMode = ref<'single' | 'multiple'>('single')
const selected = ref<string[]>([])
const pending = ref(false)
const message = ref('')
const actionError = ref('')

const assignedStudentEmails = computed(() => new Set((assignments.value || []).map(item => item.studentEmail)))
const selectedCount = computed(() => selected.value.length)
const assignButtonText = computed(() => {
  if (pending.value) return 'Назначаем...'
  if (assignmentMode.value === 'single') return selectedCount.value ? 'Назначить студенту' : 'Выберите студента'
  return selectedCount.value ? `Назначить выбранным (${selectedCount.value})` : 'Выберите студентов'
})
const formatDate = (value?: string) => value ? new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : ''

const setAssignmentMode = (mode: 'single' | 'multiple') => {
  assignmentMode.value = mode
  if (mode === 'single' && selected.value.length > 1) {
    selected.value = selected.value.slice(0, 1)
  }
}

const chooseSingleStudent = (studentId: string) => {
  selected.value = [studentId]
}

const assign = async () => {
  if (!selected.value.length) return
  pending.value = true
  message.value = ''
  actionError.value = ''
  try {
    await api<TestAssignment[]>(`/teacher/tests/${id.value}/assignments`, {
      method: 'POST',
      body: { studentIds: selected.value }
    })
    selected.value = []
    await refresh()
    message.value = assignmentMode.value === 'single' ? 'Тест назначен одному студенту' : 'Тест назначен выбранным студентам'
  } catch (error) {
    actionError.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}

usePageSeo({
  title: 'Назначить тест',
  description: 'Назначение теста выбранным студентам.',
  path: `/teacher/tests/${id.value}/assign`,
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Назначение</p>
        <h1 class="mt-2 text-4xl font-black">{{ test?.title || 'Тест' }}</h1>
        <p v-if="test" class="mt-2 text-sm font-bold text-ink/60">{{ test.subject }} · {{ test.timeLimitMinutes }} мин · {{ test.status === 'PUBLISHED' ? 'опубликован' : 'черновик' }}</p>
      </div>
      <NuxtLink :to="`/teacher/tests/${id}/edit`" class="button-secondary">К редактированию</NuxtLink>
    </header>

    <p v-if="testError || studentsError || assignmentsError" class="status-error">{{ apiErrorMessage(testError || studentsError || assignmentsError) }}</p>
    <p v-if="message" class="status-success">{{ message }}</p>
    <p v-if="actionError" class="status-error">{{ actionError }}</p>

    <section class="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
      <div class="surface overflow-hidden">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line p-5">
          <div>
            <h2 class="text-2xl font-black">Студенты</h2>
            <p class="mt-1 text-sm font-semibold text-ink/60">Выберите одного студента или сразу группу.</p>
          </div>
          <div class="flex flex-wrap items-center gap-2">
            <div class="inline-flex rounded-lg border border-line bg-paper p-1">
              <button
                type="button"
                class="rounded-md px-3 py-2 text-sm font-black transition"
                :class="assignmentMode === 'single' ? 'bg-ocean text-white' : 'text-ink/62 hover:text-ink'"
                @click="setAssignmentMode('single')"
              >
                Один студент
              </button>
              <button
                type="button"
                class="rounded-md px-3 py-2 text-sm font-black transition"
                :class="assignmentMode === 'multiple' ? 'bg-ocean text-white' : 'text-ink/62 hover:text-ink'"
                @click="setAssignmentMode('multiple')"
              >
                Несколько
              </button>
            </div>
            <button type="button" class="button-primary" :disabled="pending || !selectedCount" @click="assign">
              <BellPlus class="size-4" aria-hidden="true" />
              <span>{{ assignButtonText }}</span>
            </button>
          </div>
        </div>
        <div v-if="students?.length" class="divide-y divide-line">
          <label v-for="student in students" :key="student.id" class="flex cursor-pointer items-center justify-between gap-4 p-5">
            <span>
              <strong class="block">{{ student.fullName }}</strong>
              <span class="text-sm text-ink/60">{{ student.email }}</span>
            </span>
            <span class="flex items-center gap-3">
              <span v-if="assignedStudentEmails.has(student.email)" class="flex items-center gap-1 text-xs font-black text-mint">
                <CheckCircle2 class="size-4" aria-hidden="true" />
                назначен
              </span>
              <input
                v-if="assignmentMode === 'single'"
                type="radio"
                name="single-assigned-student"
                :checked="selected.includes(student.id)"
                class="size-5 accent-[rgb(var(--color-ocean))]"
                @change="chooseSingleStudent(student.id)"
              />
              <input v-else v-model="selected" type="checkbox" :value="student.id" class="size-5 accent-[rgb(var(--color-ocean))]" />
            </span>
          </label>
        </div>
        <p v-else class="p-5 text-sm text-ink/60">Студентов пока нет.</p>
      </div>

      <div class="surface overflow-hidden">
        <div class="border-b border-line p-5">
          <h2 class="flex items-center gap-2 text-2xl font-black">
            <UsersRound class="size-5 text-ocean" aria-hidden="true" />
            Назначения
          </h2>
        </div>
        <div v-if="assignments?.length" class="divide-y divide-line">
          <article v-for="assignment in assignments" :key="assignment.id" class="p-5">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h3 class="font-black">{{ assignment.studentName }}</h3>
                <p class="text-sm text-ink/60">{{ assignment.studentEmail }}</p>
              </div>
              <span class="rounded-md px-2.5 py-1 text-xs font-black" :class="assignment.status === 'COMPLETED' ? 'bg-mint/10 text-mint' : 'bg-gold/15 text-gold'">
                {{ assignment.status === 'COMPLETED' ? 'пройден' : 'назначен' }}
              </span>
            </div>
            <p class="mt-3 text-xs font-bold text-ink/55">Назначен: {{ formatDate(assignment.assignedAt) }}</p>
            <p v-if="assignment.completedAt" class="mt-1 text-xs font-bold text-ink/55">Завершён: {{ formatDate(assignment.completedAt) }}</p>
          </article>
        </div>
        <p v-else class="p-5 text-sm text-ink/60">Назначений пока нет.</p>
      </div>
    </section>
  </div>
</template>
