<script setup lang="ts">
import { Bell, Compass, LogIn, LogOut, Moon, Sun, UserRound } from 'lucide-vue-next'
import type { UserNotification } from '~/types/learning'

const auth = useAuthStore()
const { isDark, toggleTheme } = useThemeMode()
const api = useApi()
const unreadNotifications = ref(0)

const loadUnreadNotifications = async () => {
  auth.syncFromCookies()
  if (!auth.isAuthenticated) {
    unreadNotifications.value = 0
    return
  }
  try {
    const notifications = await api<UserNotification[]>('/users/me/notifications')
    unreadNotifications.value = notifications.filter(item => !item.read).length
  } catch {
    unreadNotifications.value = 0
  }
}

onMounted(loadUnreadNotifications)
watch(() => auth.accessToken, loadUnreadNotifications)
</script>

<template>
  <header class="py-4">
    <div class="page-shell flex flex-wrap items-center justify-between gap-3">
      <NuxtLink to="/" class="brand-plate inline-flex items-center gap-3 border-2 border-line bg-surface px-3 py-2" aria-label="Тест Магистратура">
        <Compass class="size-6 text-coral" aria-hidden="true" />
        <span class="leading-tight">
          <span class="block text-base font-black">Тест Магистратура</span>
          <span class="block text-xs font-black uppercase text-ocean">КТА магистратура</span>
        </span>
      </NuxtLink>

      <div class="flex items-center gap-2">
        <NuxtLink
          v-if="auth.isAuthenticated"
          to="/cabinet#notifications"
          class="relative grid size-11 shrink-0 place-items-center border-2 border-line bg-surface transition hover:bg-gold"
          title="Уведомления"
        >
          <Bell class="size-4" aria-hidden="true" />
          <span v-if="unreadNotifications" class="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-coral px-1 text-[10px] font-black text-white">
            {{ unreadNotifications }}
          </span>
        </NuxtLink>

        <button
          type="button"
          role="switch"
          class="relative h-11 w-[78px] shrink-0 border-2 border-line bg-surface p-1 text-ink/70 transition hover:bg-gold"
          :aria-checked="isDark"
          :aria-label="isDark ? 'Включить дневной режим' : 'Включить ночной режим'"
          :title="isDark ? 'Дневной режим' : 'Ночной режим'"
          @click="toggleTheme"
        >
          <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gold">
            <Sun class="size-4" aria-hidden="true" />
          </span>
          <span class="absolute right-3 top-1/2 -translate-y-1/2 text-ocean">
            <Moon class="size-4" aria-hidden="true" />
          </span>
          <span
            class="relative z-10 grid size-8 place-items-center rounded-full bg-white shadow-sm transition-transform duration-200"
            :class="isDark ? 'translate-x-8 bg-ink text-paper' : 'translate-x-0 text-gold'"
          >
            <Moon v-if="isDark" class="size-4" aria-hidden="true" />
            <Sun v-else class="size-4" aria-hidden="true" />
          </span>
        </button>

        <button
          v-if="auth.isAuthenticated"
          type="button"
          class="grid size-11 shrink-0 place-items-center border-2 border-line bg-surface transition hover:bg-coral hover:text-white"
          title="Выйти"
          @click="auth.logout"
        >
          <LogOut class="size-4" aria-hidden="true" />
        </button>
        <NuxtLink v-if="auth.isAuthenticated" to="/cabinet" class="button-secondary min-h-11 px-3">
          <UserRound class="size-4" aria-hidden="true" />
          <span class="hidden sm:inline">Кабинет</span>
        </NuxtLink>
        <NuxtLink v-else to="/login" class="button-primary min-h-11 px-3">
          <LogIn class="size-4" aria-hidden="true" />
          <span>Войти</span>
        </NuxtLink>
      </div>
    </div>
  </header>
</template>
