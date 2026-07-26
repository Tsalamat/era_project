<script setup lang="ts">
const { mode, isDark } = useThemeMode()

useHead(() => ({
  htmlAttrs: {
    class: isDark.value ? 'theme-dark' : '',
    'data-theme': mode.value
  },
  meta: [
    { name: 'theme-color', content: isDark.value ? '#10151b' : '#f2f6f8' }
  ]
}))
</script>

<template>
  <div>
    <AppHeader />
    <main class="pb-14 pt-6">
      <slot />
    </main>
    <MiniChat />
  </div>
</template>
