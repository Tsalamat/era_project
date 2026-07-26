<script setup lang="ts">
import { MailCheck, Route, UserPlus } from 'lucide-vue-next'

const auth = useAuthStore()
const form = reactive({ fullName: '', email: '', password: '' })
const verification = reactive({ code: '' })
const verificationSent = ref(false)
const pending = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

usePageSeo({
  title: 'Регистрация',
  description: 'Бесплатная регистрация на платформе подготовки к КТА.',
  path: '/register',
  robots: 'noindex,nofollow'
})

const submit = async () => {
  pending.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (!verificationSent.value) {
      const response = await auth.requestRegistrationCode(form)
      verificationSent.value = true
      successMessage.value = response.message || 'Код подтверждения отправлен на email'
      return
    }
    await auth.verifyRegistrationCode({ email: form.email, code: verification.code })
    await navigateTo('/cabinet')
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}

const resendCode = async () => {
  pending.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const response = await auth.requestRegistrationCode(form)
    verification.code = ''
    successMessage.value = response.message || 'Новый код отправлен на email'
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="page-shell grid min-h-[70vh] items-center py-10">
    <section class="surface grid overflow-hidden lg:grid-cols-[1.1fr_0.9fr]">
      <div class="p-6 md:p-8 lg:p-10">
        <p class="eyebrow">Бесплатный доступ</p>
        <h1 class="mt-2 text-3xl font-black">Регистрация</h1>

        <form class="mt-6 space-y-4" @submit.prevent="submit">
          <label class="block space-y-2">
            <span class="text-sm font-black">Имя и фамилия</span>
            <input v-model.trim="form.fullName" autocomplete="name" required class="input-field" :disabled="verificationSent" />
          </label>
          <label class="block space-y-2">
            <span class="text-sm font-black">Email</span>
            <input v-model.trim="form.email" type="email" autocomplete="email" required class="input-field" :disabled="verificationSent" />
          </label>
          <label class="block space-y-2">
            <span class="text-sm font-black">Пароль</span>
            <input v-model="form.password" type="password" minlength="8" autocomplete="new-password" required class="input-field" :disabled="verificationSent" />
          </label>

          <label v-if="verificationSent" class="block space-y-2">
            <span class="text-sm font-black">Код из письма</span>
            <input v-model.trim="verification.code" inputmode="numeric" autocomplete="one-time-code" required class="input-field" placeholder="Например, 123456" />
          </label>

          <p v-if="successMessage" class="rounded-lg bg-mint/10 p-4 text-sm font-bold text-mint" role="status">{{ successMessage }}</p>
          <p v-if="errorMessage" class="status-error" role="alert">{{ errorMessage }}</p>

          <button type="submit" class="button-primary w-full" :disabled="pending">
            <MailCheck v-if="verificationSent" class="size-4" aria-hidden="true" />
            <UserPlus v-else class="size-4" aria-hidden="true" />
            <span>{{ pending ? 'Проверяем...' : verificationSent ? 'Подтвердить код' : 'Получить код' }}</span>
          </button>

          <button v-if="verificationSent" type="button" class="button-secondary w-full" :disabled="pending" @click="resendCode">
            <span>Отправить код ещё раз</span>
          </button>
        </form>

        <p class="mt-5 text-center text-sm text-ink/65">
          Уже зарегистрированы? <NuxtLink to="/login" class="font-black text-ocean">Войти</NuxtLink>
        </p>
      </div>

      <aside class="border-t border-line bg-ink p-7 text-paper lg:border-l lg:border-t-0 lg:p-10">
        <Route class="size-9 text-gold" aria-hidden="true" />
        <p class="mt-6 text-sm font-black uppercase text-mint">Тест Магистратура</p>
        <h2 class="mt-3 text-4xl font-black leading-tight">Старт без оплаты и лишних входов</h2>
        <p class="mt-4 leading-7 text-paper/72">После подтверждения email backend начнет считать реальный прогресс: уроки, попытки, результаты и назначенные тесты.</p>
      </aside>
    </section>
  </div>
</template>
