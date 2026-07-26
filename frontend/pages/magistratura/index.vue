<script setup lang="ts">
const config = useRuntimeConfig()
const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')

usePageSeo({
  title: 'Магистратура и КТА: подготовка к поступлению',
  description: 'Что нужно знать для поступления в магистратуру через КТА: предметы, стратегия подготовки, пробные тесты, баллы и план занятий.',
  path: '/magistratura',
  keywords: 'магистратура КТА, поступление в магистратуру Казахстан, подготовка к комплексному тестированию, КТА баллы',
  schema: {
    '@context': 'https://schema.org',
    '@type': 'EducationalOccupationalProgram',
    name: 'Подготовка к КТА в магистратуру',
    provider: {
      '@type': 'EducationalOrganization',
      name: 'Тест Магистратура',
      url: siteUrl
    },
    url: `${siteUrl}/magistratura`
  }
})
</script>

<template>
  <div class="page-shell space-y-6">
    <header class="surface p-6 md:p-8">
      <p class="eyebrow">Поступление</p>
      <h1 class="mt-3 text-4xl font-black">Магистратура и КТА</h1>
      <p class="mt-4 max-w-3xl text-lg leading-8 text-ink/68">
        Раздел собирает ключевые материалы о структуре КТА, предметах, проходных баллах и планировании подготовки.
      </p>
    </header>

    <section class="grid gap-5 md:grid-cols-3">
      <NuxtLink to="/blog/podgotovka-k-kta-magistratura-plan" class="surface p-5">
        <h2 class="text-xl font-black">План подготовки</h2>
        <p class="mt-2 text-sm leading-6 text-ink/62">Диагностика, расписание и разбор ошибок.</p>
      </NuxtLink>
      <NuxtLink to="/tests/kta-mini-diagnostic" class="surface p-5">
        <h2 class="text-xl font-black">Пробный КТА</h2>
        <p class="mt-2 text-sm leading-6 text-ink/62">Онлайн-тест с результатом и объяснениями.</p>
      </NuxtLink>
      <NuxtLink to="/courses" class="surface p-5">
        <h2 class="text-xl font-black">Курсы</h2>
        <p class="mt-2 text-sm leading-6 text-ink/62">Открытые уроки по основным предметам.</p>
      </NuxtLink>
    </section>
  </div>
</template>
