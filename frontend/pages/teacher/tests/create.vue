<script setup lang="ts">
usePageSeo({
  title: 'Создание теста',
  description: 'Создание вопросов, вариантов ответа и объяснений для теста КТА.',
  path: '/teacher/tests/create',
  robots: 'noindex,nofollow'
})

const handleSaved = (id: string) => navigateTo(`/teacher/tests/${id}/edit`)
</script>

<template>
  <div class="page-shell space-y-6">
    <header>
      <p class="eyebrow">Редактор</p>
      <h1 class="mt-2 text-4xl font-black">Новый тест</h1>
    </header>
    <TeacherTestForm @saved="handleSaved" />
  </div>
</template>
