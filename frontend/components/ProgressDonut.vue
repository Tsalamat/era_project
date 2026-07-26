<script setup lang="ts">
const props = defineProps<{
  value: number
  label: string
}>()

const style = computed(() => ({
  background: `conic-gradient(rgb(var(--color-mint)) ${props.value * 3.6}deg, rgb(var(--color-line)) 0deg)`
}))
</script>

<template>
  <div class="flex items-center gap-4">
    <div class="grid size-24 shrink-0 place-items-center rounded-full" :style="style">
      <div class="grid size-[74px] place-items-center rounded-full bg-white text-xl font-black">{{ value }}%</div>
    </div>
    <div>
      <p class="font-black">{{ label }}</p>
      <p class="mt-1 text-sm leading-6 text-ink/62">прогресс сохранен</p>
    </div>
  </div>
</template>
