<script setup lang="ts">
import { KeyRound, LogIn, ShieldCheck } from 'lucide-vue-next'

const auth = useAuthStore()
const route = useRoute()
const form = reactive({ email: '', password: '' })
const pending = ref(false)
const errorMessage = ref('')
const requestedRedirect = computed(() => typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
  ? route.query.redirect
  : '/cabinet')

usePageSeo({
  title: 'Вход',
  description: 'Вход в личный кабинет платформы подготовки к КТА.',
  path: '/login',
  robots: 'noindex,nofollow'
})

const submit = async () => {
  pending.value = true
  errorMessage.value = ''
  try {
    const session = await auth.login(form)
    await navigateTo(requestedRedirect.value === '/cabinet'
      ? (session.role === 'ADMIN' ? '/admin' : session.role === 'TEACHER' ? '/teacher' : '/cabinet')
      : requestedRedirect.value)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="page-shell grid min-h-[70vh] items-center py-10">
    <section class="surface grid overflow-hidden lg:grid-cols-[0.9fr_1.1fr]">
      <aside class="border-b border-line bg-ink p-7 text-paper lg:border-b-0 lg:border-r lg:p-10">
        <ShieldCheck class="size-9 text-gold" aria-hidden="true" />
        <p class="mt-6 text-sm font-black uppercase text-mint">Тест Магистратура</p>
        <h1 class="mt-3 text-4xl font-black leading-tight">Вход в учебный кабинет</h1>
        <p class="mt-4 max-w-md leading-7 text-paper/72">Курсы, тесты, назначенные задания и результаты открываются после обычного входа по email и паролю.</p>
        <div class="mt-8 rounded-lg border border-paper/15 bg-paper/8 p-4">
          <KeyRound class="mb-3 size-5 text-gold" aria-hidden="true" />
          <p class="text-sm font-bold leading-6 text-paper/78">Вход через внешние сервисы отключен. Аккаунт платформы работает напрямую через backend.</p>
        </div>
      </aside>

      <div class="p-6 md:p-8 lg:p-10">
        <p class="eyebrow">Личный кабинет</p>
        <h2 class="mt-2 text-3xl font-black">Войти</h2>

        <form class="mt-6 space-y-4" @submit.prevent="submit">
          <label class="block space-y-2">
            <span class="text-sm font-black">Email</span>
            <input v-model.trim="form.email" type="email" autocomplete="email" required class="input-field" />
          </label>
          <label class="block space-y-2">
            <span class="text-sm font-black">Пароль</span>
            <input v-model="form.password" type="password" autocomplete="current-password" required class="input-field" />
          </label>

          <p v-if="errorMessage" class="status-error" role="alert">{{ errorMessage }}</p>

          <button type="submit" class="button-primary w-full" :disabled="pending">
            <LogIn class="size-4" aria-hidden="true" />
            <span>{{ pending ? 'Входим...' : 'Войти' }}</span>
          </button>
        </form>

        <p class="mt-5 text-center text-sm text-ink/65">
          Нет аккаунта? <NuxtLink to="/register" class="font-black text-ocean">Зарегистрироваться</NuxtLink>
        </p>
      </div>
    </section>
  </div>
</template>
