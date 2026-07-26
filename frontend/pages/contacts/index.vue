<script setup lang="ts">
import { Mail, MessageCircle, Send } from 'lucide-vue-next'

usePageSeo({
  title: 'Контакты',
  description: 'Контакты платформы Тест Магистратура для вопросов о подготовке к КТА.',
  path: '/contacts'
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header>
      <p class="eyebrow">Связь</p>
      <h1 class="mt-2 text-4xl font-black">Контакты</h1>
    </header>

    <section class="grid gap-5 md:grid-cols-3">
      <a href="mailto:hello@kta-study.kz" class="surface p-5">
        <Mail class="mb-3 size-6 text-ocean" aria-hidden="true" />
        <h2 class="text-xl font-black">Email</h2>
        <p class="mt-2 text-sm text-ink/62">hello@kta-study.kz</p>
      </a>
      <a href="https://t.me/" class="surface p-5">
        <Send class="mb-3 size-6 text-mint" aria-hidden="true" />
        <h2 class="text-xl font-black">Telegram</h2>
        <p class="mt-2 text-sm text-ink/62">@kta-study</p>
      </a>
      <div class="surface p-5">
        <MessageCircle class="mb-3 size-6 text-gold" aria-hidden="true" />
        <h2 class="text-xl font-black">Заявка</h2>
        <p class="mt-2 text-sm text-ink/62">Ответ в течение рабочего дня.</p>
      </div>
    </section>
  </div>
</template>
