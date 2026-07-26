<script setup lang="ts">
import { ShieldCheck, UserPlus, UsersRound } from 'lucide-vue-next'
import type { UserProfile } from '~/types/learning'

const api = useApi()
const { data: users, error, pending, refresh } = await useAsyncData('admin-users', () => api<UserProfile[]>('/admin/users'))
const actionUserId = ref<string | null>(null)
const actionError = ref('')
const teacherCount = computed(() => users.value?.filter(user => user.roles.includes('TEACHER')).length || 0)
const adminCount = computed(() => users.value?.filter(user => user.roles.includes('ADMIN')).length || 0)

const assignTeacher = async (user: UserProfile) => {
  actionUserId.value = user.id
  actionError.value = ''
  try {
    await api(`/admin/users/${user.id}/role`, { method: 'PATCH', body: { role: 'TEACHER' } })
    await refresh()
  } catch (requestError) {
    actionError.value = apiErrorMessage(requestError)
  } finally {
    actionUserId.value = null
  }
}

usePageSeo({
  title: 'Админка',
  description: 'Администрирование пользователей и ролей платформы.',
  path: '/admin',
  robots: 'noindex,nofollow'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header>
      <p class="eyebrow">Админ</p>
      <h1 class="mt-2 text-4xl font-black">Панель управления</h1>
    </header>

    <p v-if="error" class="status-error">Не удалось загрузить пользователей.</p>
    <p v-if="actionError" class="status-error" role="alert">{{ actionError }}</p>

    <section class="grid gap-5 md:grid-cols-3">
      <div class="surface p-5">
        <UsersRound class="mb-3 size-6 text-ocean" aria-hidden="true" />
        <strong class="text-3xl">{{ users?.length || 0 }}</strong>
        <span class="block text-sm text-ink/60">пользователей</span>
      </div>
      <div class="surface p-5">
        <UserPlus class="mb-3 size-6 text-mint" aria-hidden="true" />
        <strong class="text-3xl">{{ teacherCount }}</strong>
        <span class="block text-sm text-ink/60">учителей</span>
      </div>
      <div class="surface p-5">
        <ShieldCheck class="mb-3 size-6 text-gold" aria-hidden="true" />
        <strong class="text-3xl">{{ adminCount }}</strong>
        <span class="block text-sm text-ink/60">администраторов</span>
      </div>
    </section>

    <section class="surface overflow-hidden">
      <div class="border-b border-line p-5">
        <h2 class="text-2xl font-black">Пользователи</h2>
      </div>
      <p v-if="pending" class="p-5 text-sm text-ink/60">Загружаем пользователей...</p>
      <div v-else-if="users?.length" class="divide-y divide-line">
        <div v-for="user in users" :key="user.id" class="flex flex-wrap items-center justify-between gap-4 p-5">
          <div>
            <h3 class="font-black">{{ user.fullName }}</h3>
            <p class="text-sm text-ink/60">{{ user.email }}</p>
            <p class="mt-1 text-xs font-bold text-ocean">{{ user.roles.join(', ') }}</p>
          </div>
          <button
            v-if="!user.roles.includes('TEACHER') && !user.roles.includes('ADMIN')"
            type="button"
            class="button-secondary"
            :disabled="actionUserId === user.id"
            @click="assignTeacher(user)"
          >
            <UserPlus class="size-4" aria-hidden="true" />
            <span>{{ actionUserId === user.id ? 'Назначаем...' : 'Назначить учителем' }}</span>
          </button>
        </div>
      </div>
      <p v-else class="p-5 text-sm text-ink/60">Пользователей пока нет.</p>
    </section>
  </div>
</template>
