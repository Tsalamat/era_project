<script setup lang="ts">
import { Eye, Save } from 'lucide-vue-next'
import type { BlogPost } from '~/types/learning'

const props = defineProps<{
  post?: BlogPost | null
}>()

const emit = defineEmits<{
  saved: [post: BlogPost]
}>()

const api = useApi()
const pending = ref(false)
const errorMessage = ref('')
const slugTouched = ref(Boolean(props.post?.slug))
const form = reactive({
  title: props.post?.title || '',
  slug: props.post?.slug || '',
  category: props.post?.category || 'Подготовка',
  excerpt: props.post?.excerpt || '',
  content: props.post?.content || '',
  readMinutes: props.post?.readMinutes || 5,
  status: props.post?.status || 'DRAFT'
})

const slugify = (value: string) => value
  .toLowerCase()
  .trim()
  .replace(/[^a-z0-9а-яё\s-]/gi, '')
  .replace(/[а-яё]/gi, char => ({
    а: 'a', б: 'b', в: 'v', г: 'g', д: 'd', е: 'e', ё: 'e', ж: 'zh', з: 'z', и: 'i', й: 'i', к: 'k', л: 'l', м: 'm', н: 'n', о: 'o',
    п: 'p', р: 'r', с: 's', т: 't', у: 'u', ф: 'f', х: 'h', ц: 'c', ч: 'ch', ш: 'sh', щ: 'sh', ъ: '', ы: 'y', ь: '', э: 'e', ю: 'yu', я: 'ya'
  }[char.toLowerCase()] || char))
  .replace(/\s+/g, '-')
  .replace(/-+/g, '-')
  .replace(/^-|-$/g, '')

watch(() => form.title, title => {
  if (!slugTouched.value) form.slug = slugify(title)
})

const submit = async () => {
  pending.value = true
  errorMessage.value = ''
  try {
    const payload = { ...form, readMinutes: Number(form.readMinutes) || 1 }
    const saved = props.post?.id
      ? await api<BlogPost>(`/teacher/blog/${props.post.id}`, { method: 'PATCH', body: payload })
      : await api<BlogPost>('/teacher/blog', { method: 'POST', body: payload })
    emit('saved', saved)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <form class="space-y-5" @submit.prevent="submit">
    <p v-if="errorMessage" class="status-error">{{ errorMessage }}</p>

    <section class="surface grid gap-4 p-5 md:grid-cols-2">
      <label class="block space-y-2 md:col-span-2">
        <span class="text-sm font-black">Название</span>
        <input v-model.trim="form.title" required class="input-field" />
      </label>
      <label class="block space-y-2">
        <span class="text-sm font-black">Slug</span>
        <input v-model.trim="form.slug" required pattern="[a-z0-9-]+" class="input-field" @input="slugTouched = true" />
      </label>
      <label class="block space-y-2">
        <span class="text-sm font-black">Категория</span>
        <input v-model.trim="form.category" required class="input-field" />
      </label>
      <label class="block space-y-2">
        <span class="text-sm font-black">Минут чтения</span>
        <input v-model.number="form.readMinutes" type="number" min="1" required class="input-field" />
      </label>
      <label class="block space-y-2">
        <span class="text-sm font-black">Статус</span>
        <select v-model="form.status" class="input-field">
          <option value="DRAFT">Черновик</option>
          <option value="PUBLISHED">Опубликовать</option>
        </select>
      </label>
      <label class="block space-y-2 md:col-span-2">
        <span class="text-sm font-black">Краткое описание</span>
        <textarea v-model.trim="form.excerpt" class="input-field min-h-24" />
      </label>
      <label class="block space-y-2 md:col-span-2">
        <span class="text-sm font-black">Текст статьи</span>
        <textarea v-model="form.content" required class="input-field min-h-72" />
      </label>
    </section>

    <section class="surface p-5">
      <div class="flex items-center gap-2">
        <Eye class="size-5 text-ocean" aria-hidden="true" />
        <h2 class="text-xl font-black">Предпросмотр</h2>
      </div>
      <div class="mt-4 rounded-lg border border-line bg-paper p-4">
        <p class="eyebrow">{{ form.category || 'Категория' }}</p>
        <h3 class="mt-2 text-2xl font-black">{{ form.title || 'Название статьи' }}</h3>
        <p class="mt-3 text-sm leading-6 text-ink/68">{{ form.excerpt || 'Краткое описание статьи.' }}</p>
      </div>
    </section>

    <button type="submit" class="button-primary" :disabled="pending">
      <Save class="size-4" aria-hidden="true" />
      <span>{{ pending ? 'Сохраняем...' : 'Сохранить статью' }}</span>
    </button>
  </form>
</template>
