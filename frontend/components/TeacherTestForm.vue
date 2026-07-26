<script setup lang="ts">
import { Check, Eye, FileText, GripVertical, Plus, Save, Send, Trash2, Upload, X } from 'lucide-vue-next'
import type { Question, QuestionImportResult, QuestionType, TeacherTest } from '~/types/learning'

type EditableOption = { id?: string; text: string; correct: boolean }
type EditableQuestion = {
  id?: string
  clientId: string
  text: string
  type: QuestionType
  explanation: string
  options: EditableOption[]
}

const props = defineProps<{ initialTest?: TeacherTest }>()
const emit = defineEmits<{ saved: [id: string] }>()
const api = useApi()

const blankQuestion = (): EditableQuestion => ({
  clientId: crypto.randomUUID(),
  text: '',
  type: 'SINGLE_CHOICE',
  explanation: '',
  options: [
    { text: '', correct: true },
    { text: '', correct: false },
    { text: '', correct: false },
    { text: '', correct: false }
  ]
})

const toEditableQuestion = (question: Question): EditableQuestion => ({
  id: question.id,
  clientId: question.id || crypto.randomUUID(),
  text: question.questionText,
  type: question.questionType,
  explanation: question.explanation || '',
  options: question.options.map(option => ({ id: option.id, text: option.optionText, correct: option.isCorrect }))
})

const form = reactive({
  title: props.initialTest?.title || '',
  slug: props.initialTest?.slug || '',
  subject: props.initialTest?.subject || 'Логика',
  description: props.initialTest?.description || '',
  timeLimitMinutes: props.initialTest?.timeLimitMinutes || 30,
  status: props.initialTest?.status || 'DRAFT',
  questions: props.initialTest?.questions.map(toEditableQuestion) || [blankQuestion()]
})

const testId = ref(props.initialTest?.id)
const deletedQuestionIds = ref<string[]>([])
const pending = ref(false)
const importing = ref(false)
const preview = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const slugTouched = ref(Boolean(props.initialTest))
const draggedIndex = ref<number | null>(null)
const wordInput = ref<HTMLInputElement | null>(null)

const toSlug = (value: string) => value
  .toLowerCase()
  .trim()
  .replace(/[аә]/g, 'a').replace(/[б]/g, 'b').replace(/[в]/g, 'v').replace(/[гғ]/g, 'g')
  .replace(/[д]/g, 'd').replace(/[еёэ]/g, 'e').replace(/[ж]/g, 'zh').replace(/[з]/g, 'z')
  .replace(/[иі]/g, 'i').replace(/[й]/g, 'y').replace(/[кқ]/g, 'k').replace(/[л]/g, 'l')
  .replace(/[м]/g, 'm').replace(/[нң]/g, 'n').replace(/[оө]/g, 'o').replace(/[п]/g, 'p')
  .replace(/[р]/g, 'r').replace(/[с]/g, 's').replace(/[т]/g, 't').replace(/[уұү]/g, 'u')
  .replace(/[ф]/g, 'f').replace(/[хһ]/g, 'h').replace(/[ц]/g, 'ts').replace(/[ч]/g, 'ch')
  .replace(/[шщ]/g, 'sh').replace(/[ы]/g, 'y').replace(/[ьъ]/g, '')
  .replace(/[^a-z0-9]+/g, '-')
  .replace(/^-|-$/g, '')

watch(() => form.title, (title) => {
  if (!slugTouched.value) form.slug = toSlug(title)
})

const addQuestion = () => form.questions.push(blankQuestion())

const removeQuestion = (index: number) => {
  const [removed] = form.questions.splice(index, 1)
  if (removed?.id) deletedQuestionIds.value.push(removed.id)
}

const setCorrect = (question: EditableQuestion, optionIndex: number) => {
  if (question.type === 'SINGLE_CHOICE') {
    question.options.forEach((option, index) => { option.correct = index === optionIndex })
  } else {
    question.options[optionIndex]!.correct = !question.options[optionIndex]!.correct
  }
}

const normalizeCorrectAnswers = (question: EditableQuestion) => {
  if (question.type === 'SINGLE_CHOICE') {
    const selectedIndex = Math.max(0, question.options.findIndex(option => option.correct))
    question.options.forEach((option, index) => { option.correct = index === selectedIndex })
  }
}

const dropQuestion = (targetIndex: number) => {
  if (draggedIndex.value === null || draggedIndex.value === targetIndex) return
  const [moved] = form.questions.splice(draggedIndex.value, 1)
  if (moved) form.questions.splice(targetIndex, 0, moved)
  draggedIndex.value = null
}

const validate = () => {
  if (!form.title.trim() || !form.slug.trim()) return 'Заполните название и slug'
  if (!form.questions.length) return 'Добавьте хотя бы один вопрос'
  for (const [index, question] of form.questions.entries()) {
    if (!question.text.trim()) return `Заполните текст вопроса ${index + 1}`
    if (question.options.some(option => !option.text.trim())) return `Заполните все варианты ответа у вопроса ${index + 1}`
    const correctCount = question.options.filter(option => option.correct).length
    if (correctCount === 0 || (question.type === 'SINGLE_CHOICE' && correctCount !== 1)) return `Проверьте правильный ответ у вопроса ${index + 1}`
  }
  return ''
}

const metadataPayload = () => ({
  title: form.title.trim(),
  slug: form.slug.trim(),
  description: form.description.trim(),
  subject: form.subject,
  timeLimitMinutes: form.timeLimitMinutes
})

const saveMetadata = async () => {
  if (!form.title.trim() || !form.slug.trim()) {
    throw new Error('Перед импортом заполните название и slug теста')
  }
  const savedTest = testId.value
    ? await api<TeacherTest>(`/teacher/tests/${testId.value}`, { method: 'PATCH', body: metadataPayload() })
    : await api<TeacherTest>('/teacher/tests', { method: 'POST', body: metadataPayload() })
  testId.value = savedTest.id
  form.status = savedTest.status
  return savedTest
}

const hasOnlyBlankQuestion = () => form.questions.length === 1
  && !form.questions[0]!.id
  && !form.questions[0]!.text.trim()
  && !form.questions[0]!.explanation.trim()
  && form.questions[0]!.options.every(option => !option.text.trim())

const openWordImport = () => {
  if (!pending.value && !importing.value) wordInput.value?.click()
}

const importQuestionsFromWord = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.docx')) {
    errorMessage.value = 'Загрузите файл Word в формате .docx'
    return
  }

  importing.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const savedTest = await saveMetadata()
    const body = new FormData()
    body.append('file', file)
    const result = await api<QuestionImportResult>(`/teacher/tests/${savedTest.id}/questions/import-docx`, {
      method: 'POST',
      body
    })
    const imported = result.questions.map(toEditableQuestion)
    if (hasOnlyBlankQuestion()) {
      form.questions.splice(0, form.questions.length, ...imported)
    } else {
      form.questions.push(...imported)
    }
    successMessage.value = `Импортировано вопросов: ${result.importedCount}`
    emit('saved', savedTest.id)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    importing.value = false
  }
}

const saveTest = async (publish: boolean) => {
  const validationError = validate()
  if (validationError) {
    errorMessage.value = validationError
    return
  }

  pending.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const savedTest = await saveMetadata()
    testId.value = savedTest.id

    for (const id of deletedQuestionIds.value) {
      await api(`/teacher/questions/${id}`, { method: 'DELETE' })
    }
    deletedQuestionIds.value = []

    for (const [index, question] of form.questions.entries()) {
      const payload = {
        questionText: question.text.trim(),
        questionType: question.type,
        explanation: question.explanation.trim(),
        orderNumber: index + 1,
        options: question.options.map((option, optionIndex) => ({
          id: option.id,
          optionText: option.text.trim(),
          isCorrect: option.correct,
          orderNumber: optionIndex + 1
        }))
      }
      const savedQuestion = question.id
        ? await api<{ id: string }>(`/teacher/questions/${question.id}`, { method: 'PATCH', body: payload })
        : await api<{ id: string }>(`/teacher/tests/${testId.value}/questions`, { method: 'POST', body: payload })
      question.id = savedQuestion.id
    }

    if (publish) {
      await api(`/teacher/tests/${testId.value}/publish`, { method: 'POST' })
      form.status = 'PUBLISHED'
      successMessage.value = 'Тест опубликован и доступен студентам'
    } else {
      successMessage.value = 'Черновик сохранен'
    }
    emit('saved', testId.value)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <form class="space-y-6" @submit.prevent="saveTest(false)">
    <section class="surface p-5">
      <div class="mb-5 flex items-center justify-between gap-3">
        <h2 class="text-xl font-black">Параметры теста</h2>
        <span class="rounded-md px-2.5 py-1 text-xs font-black" :class="form.status === 'PUBLISHED' ? 'bg-mint/10 text-mint' : 'bg-coral/10 text-coral'">
          {{ form.status === 'PUBLISHED' ? 'опубликован' : 'черновик' }}
        </span>
      </div>
      <div class="grid gap-4 md:grid-cols-2">
        <label class="space-y-2">
          <span class="text-sm font-black">Название</span>
          <input v-model="form.title" required class="input-field" />
        </label>
        <label class="space-y-2">
          <span class="text-sm font-black">Slug</span>
          <input v-model="form.slug" required pattern="[a-z0-9-]+" class="input-field" @input="slugTouched = true" />
        </label>
        <label class="space-y-2">
          <span class="text-sm font-black">Предмет</span>
          <select v-model="form.subject" class="input-field">
            <option>Логика</option>
            <option>Английский</option>
            <option>Профильный предмет</option>
            <option>Комплексный</option>
          </select>
        </label>
        <label class="space-y-2">
          <span class="text-sm font-black">Лимит времени, минут</span>
          <input v-model.number="form.timeLimitMinutes" type="number" min="1" required class="input-field" />
        </label>
        <label class="space-y-2 md:col-span-2">
          <span class="text-sm font-black">Описание</span>
          <textarea v-model="form.description" class="input-field min-h-24" />
        </label>
      </div>
    </section>

    <section class="surface p-5">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="max-w-2xl">
          <div class="flex items-center gap-3">
            <FileText class="size-5 text-ocean" aria-hidden="true" />
            <h2 class="text-xl font-black">Импорт вопросов из Word</h2>
          </div>
          <p class="mt-2 text-sm font-semibold text-ink/60">
            Один файл .docx может содержать сразу все вопросы теста. После загрузки вопросы добавятся ниже и их можно будет редактировать.
          </p>
        </div>
        <input
          ref="wordInput"
          type="file"
          accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          class="hidden"
          @change="importQuestionsFromWord"
        />
        <button type="button" class="button-primary" :disabled="pending || importing" @click="openWordImport">
          <Upload class="size-4" aria-hidden="true" />
          <span>{{ importing ? 'Импортируем...' : 'Загрузить .docx' }}</span>
        </button>
      </div>

      <div class="mt-5 grid gap-4 lg:grid-cols-[1fr_320px]">
        <pre class="overflow-x-auto rounded-lg border border-line bg-slate-950 p-4 text-sm font-semibold leading-6 text-white"><code>1. Какой раздел проверяет критическое мышление?
A) Математическая грамотность
B) Оқу-аналитикалық сауаттылық
C) Профильный предмет
D) Иностранный язык
Ответ: B
Объяснение: В КТА этот блок проверяет работу с текстом и выводами.

2. Какие инструменты помогают учиться по результатам теста?
A) Аналитика результата
B) Объяснение ошибок
C) Скачивание Word-отчета
D) Цвет темы сайта
Ответ: A,B,C
Объяснение: Несколько букв автоматически создают вопрос с несколькими ответами.</code></pre>

        <div class="rounded-lg border border-line bg-paper p-4 text-sm font-semibold text-ink/70">
          <p class="font-black text-ink">Формат файла</p>
          <p class="mt-3">Каждый вопрос начинается с номера: <span class="font-black">1.</span>, <span class="font-black">2.</span>, <span class="font-black">3.</span></p>
          <p class="mt-2">Варианты пишутся строго как <span class="font-black">A)</span>, <span class="font-black">B)</span>, <span class="font-black">C)</span>, <span class="font-black">D)</span>.</p>
          <p class="mt-2">Правильный ответ: <span class="font-black">Ответ: A</span> или <span class="font-black">Ответ: A,C</span>.</p>
          <p class="mt-2">Объяснение можно не писать, но если нужно: <span class="font-black">Объяснение:</span>.</p>
          <p class="mt-2">Можно использовать кириллицу: <span class="font-black">А)</span>, <span class="font-black">Б)</span>, <span class="font-black">В)</span>, <span class="font-black">Г)</span>.</p>
        </div>
      </div>
    </section>

    <section class="space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h2 class="text-2xl font-black">Вопросы</h2>
        <button type="button" class="button-secondary" @click="addQuestion">
          <Plus class="size-4" aria-hidden="true" />
          <span>Добавить вопрос</span>
        </button>
      </div>

      <article
        v-for="(question, questionIndex) in form.questions"
        :key="question.clientId"
        draggable="true"
        class="surface p-5"
        @dragstart="draggedIndex = questionIndex"
        @dragover.prevent
        @drop="dropQuestion(questionIndex)"
      >
        <div class="flex items-start gap-3">
          <button type="button" class="mt-9 grid size-9 shrink-0 place-items-center rounded-md border border-line" title="Перетащить вопрос">
            <GripVertical class="size-4 text-ink/50" aria-hidden="true" />
          </button>
          <div class="grid min-w-0 flex-1 gap-4 md:grid-cols-[1fr_220px]">
            <label class="space-y-2">
              <span class="text-sm font-black">Вопрос {{ questionIndex + 1 }}</span>
              <textarea v-model="question.text" required class="input-field min-h-24" />
            </label>
            <label class="space-y-2">
              <span class="text-sm font-black">Тип</span>
              <select v-model="question.type" class="input-field" @change="normalizeCorrectAnswers(question)">
                <option value="SINGLE_CHOICE">Один ответ</option>
                <option value="MULTIPLE_CHOICE">Несколько ответов</option>
              </select>
            </label>
          </div>
          <button type="button" class="mt-9 grid size-9 shrink-0 place-items-center rounded-md border border-line text-coral" title="Удалить вопрос" @click="removeQuestion(questionIndex)">
            <Trash2 class="size-4" aria-hidden="true" />
          </button>
        </div>

        <div class="mt-5 grid gap-3 md:grid-cols-2">
          <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="flex items-center gap-3 rounded-lg border border-line bg-white p-3">
            <button
              type="button"
              class="grid size-7 shrink-0 place-items-center rounded-md border"
              :class="option.correct ? 'border-mint bg-mint text-white' : 'border-line bg-paper text-transparent'"
              :title="option.correct ? 'Правильный ответ' : 'Отметить правильным'"
              @click="setCorrect(question, optionIndex)"
            >
              <Check class="size-4" aria-hidden="true" />
            </button>
            <input v-model="option.text" required class="min-w-0 flex-1 border-0 bg-transparent text-sm font-semibold outline-none" :placeholder="`Вариант ${optionIndex + 1}`" />
          </div>
        </div>

        <label class="mt-5 block space-y-2">
          <span class="text-sm font-black">Объяснение</span>
          <textarea v-model="question.explanation" class="input-field min-h-20" />
        </label>
      </article>
    </section>

    <p v-if="successMessage" class="status-success" role="status">{{ successMessage }}</p>
    <p v-if="errorMessage" class="status-error" role="alert">{{ errorMessage }}</p>

    <section v-if="preview" class="surface p-5">
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-2xl font-black">Предпросмотр</h2>
        <button type="button" class="grid size-9 place-items-center rounded-md border border-line" title="Закрыть предпросмотр" @click="preview = false">
          <X class="size-4" aria-hidden="true" />
        </button>
      </div>
      <div class="mt-5 space-y-5">
        <div v-for="(question, index) in form.questions" :key="question.clientId">
          <h3 class="font-black">{{ index + 1 }}. {{ question.text || 'Без текста' }}</h3>
          <div class="mt-2 grid gap-2 md:grid-cols-2">
            <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="rounded-lg border border-line bg-paper p-3 text-sm font-semibold">
              {{ option.text || `Вариант ${optionIndex + 1}` }}
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="flex flex-wrap gap-3">
      <button type="button" class="button-secondary" @click="preview = !preview">
        <Eye class="size-4" aria-hidden="true" />
        <span>Предпросмотр</span>
      </button>
      <button type="submit" class="button-secondary" :disabled="pending">
        <Save class="size-4" aria-hidden="true" />
        <span>{{ pending ? 'Сохраняем...' : 'Сохранить' }}</span>
      </button>
      <button type="button" class="button-primary" :disabled="pending" @click="saveTest(true)">
        <Send class="size-4" aria-hidden="true" />
        <span>{{ pending ? 'Публикуем...' : 'Опубликовать' }}</span>
      </button>
    </div>
  </form>
</template>
