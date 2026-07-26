<script setup lang="ts">
import { MessageCircle, Send, X } from 'lucide-vue-next'
import type { ChatMessage } from '~/types/learning'

const api = useApi()
const auth = useAuthStore()

const open = ref(false)
const pending = ref(false)
const sending = ref(false)
const connected = ref(false)
const errorMessage = ref('')
const draft = ref('')
const messages = ref<ChatMessage[]>([])
const socket = shallowRef<WebSocket | null>(null)
const reconnectTimer = ref<ReturnType<typeof setTimeout> | null>(null)

type ChatSocketPayload =
  | { type: 'history'; messages: ChatMessage[] }
  | { type: 'message'; message: ChatMessage }
  | { type: 'error'; message: string }

const roleLabel = (role: ChatMessage['senderRole']) => {
  if (role === 'ADMIN') return 'Админ'
  if (role === 'TEACHER') return 'Учитель'
  return 'Студент'
}

const loadMessages = async () => {
  if (!auth.isAuthenticated) return
  pending.value = true
  errorMessage.value = ''
  try {
    messages.value = await api<ChatMessage[]>('/chat/messages')
    await nextTick()
    const list = document.querySelector('[data-chat-list]')
    list?.scrollTo({ top: list.scrollHeight })
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    pending.value = false
  }
}

const socketUrl = () => {
  const config = useRuntimeConfig()
  const apiBase = String(config.public.apiBase || '')
  if (apiBase.startsWith('http')) {
    const url = new URL(apiBase)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    url.pathname = '/ws/chat'
    url.search = ''
    return url.toString()
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/chat`
}

const scrollToBottom = async (smooth = false) => {
  await nextTick()
  const list = document.querySelector('[data-chat-list]')
  list?.scrollTo({ top: list.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
}

const connectSocket = () => {
  if (!import.meta.client || !auth.isAuthenticated) return
  if (socket.value?.readyState === WebSocket.OPEN || socket.value?.readyState === WebSocket.CONNECTING) return

  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
    reconnectTimer.value = null
  }

  const nextSocket = new WebSocket(socketUrl())
  socket.value = nextSocket

  nextSocket.onopen = () => {
    connected.value = true
    errorMessage.value = ''
  }

  nextSocket.onmessage = async (event) => {
    const payload = JSON.parse(String(event.data)) as ChatSocketPayload
    if (payload.type === 'history') {
      messages.value = payload.messages
      await scrollToBottom()
    }
    if (payload.type === 'message') {
      messages.value = [...messages.value.filter(message => message.id !== payload.message.id), payload.message]
      await scrollToBottom(true)
    }
    if (payload.type === 'error') {
      errorMessage.value = payload.message
    }
  }

  nextSocket.onerror = () => {
    errorMessage.value = 'WebSocket недоступен, пробую обычную загрузку'
    void loadMessages()
  }

  nextSocket.onclose = () => {
    connected.value = false
    if (open.value && auth.isAuthenticated) {
      reconnectTimer.value = setTimeout(connectSocket, 3000)
    }
  }
}

const toggle = async () => {
  open.value = !open.value
  if (open.value) {
    auth.syncFromCookies()
    connectSocket()
    await loadMessages()
  }
}

const sendMessage = async () => {
  const text = draft.value.trim()
  if (!text || sending.value || !auth.isAuthenticated) return

  sending.value = true
  errorMessage.value = ''
  try {
    if (socket.value?.readyState === WebSocket.OPEN) {
      socket.value.send(JSON.stringify({ message: text }))
    } else {
      const message = await api<ChatMessage>('/chat/messages', { method: 'POST', body: { message: text } })
      messages.value = [...messages.value, message]
    }
    draft.value = ''
    await scrollToBottom(true)
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    sending.value = false
  }
}

watch(open, (value) => {
  if (value) {
    connectSocket()
  } else {
    socket.value?.close()
    socket.value = null
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
  }
})

onBeforeUnmount(() => {
  socket.value?.close()
  if (reconnectTimer.value) clearTimeout(reconnectTimer.value)
})
</script>

<template>
  <div class="fixed bottom-5 right-5 z-50">
    <section
      v-if="open"
      class="mb-3 flex h-[min(560px,calc(100vh-112px))] w-[min(380px,calc(100vw-32px))] flex-col overflow-hidden rounded-lg border border-line bg-surface shadow-2xl"
      aria-label="Мини-чат"
    >
      <header class="flex items-center justify-between border-b border-line px-4 py-3">
        <div>
          <p class="text-sm font-black">Чат</p>
          <p v-if="auth.isAuthenticated" class="text-xs text-ink/60">
            {{ auth.fullName }} · {{ auth.role }} · {{ connected ? 'онлайн' : 'подключение' }}
          </p>
        </div>
        <button type="button" class="grid size-9 place-items-center rounded-md border border-line" title="Закрыть" @click="open = false">
          <X class="size-4" aria-hidden="true" />
        </button>
      </header>

      <div v-if="!auth.isAuthenticated" class="grid flex-1 place-items-center p-5 text-center">
        <div class="space-y-3">
          <p class="font-black">Войдите, чтобы писать в чат</p>
          <NuxtLink to="/login" class="button-primary">Войти</NuxtLink>
        </div>
      </div>

      <template v-else>
        <div data-chat-list class="flex-1 space-y-3 overflow-y-auto bg-paper p-4">
          <div v-if="pending && !messages.length" class="text-sm font-bold text-ink/60">Загрузка...</div>
          <div v-else-if="!messages.length" class="text-sm font-bold text-ink/60">Сообщений пока нет</div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="max-w-[86%] rounded-lg border p-3"
            :class="message.mine ? 'ml-auto border-ocean/20 bg-ocean text-white' : 'border-line bg-surface text-ink'"
          >
            <div class="mb-1 flex items-center gap-2 text-[11px] font-black" :class="message.mine ? 'text-white/80' : 'text-ink/55'">
              <span class="truncate">{{ message.senderName }}</span>
              <span class="rounded px-1.5 py-0.5" :class="message.mine ? 'bg-white/15' : 'bg-paper'">{{ roleLabel(message.senderRole) }}</span>
            </div>
            <p class="whitespace-pre-wrap break-words text-sm leading-5">{{ message.message }}</p>
          </article>
        </div>

        <p v-if="errorMessage" class="mx-4 mt-3 rounded-md bg-coral/10 px-3 py-2 text-xs font-bold text-coral">{{ errorMessage }}</p>

        <form class="flex gap-2 border-t border-line p-3" @submit.prevent="sendMessage">
          <textarea
            v-model="draft"
            maxlength="1000"
            rows="1"
            class="input-field max-h-28 min-h-11 resize-none"
            placeholder="Написать..."
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button type="submit" class="grid size-11 shrink-0 place-items-center rounded-md bg-ocean text-white disabled:opacity-50" :disabled="sending || !draft.trim()" title="Отправить">
            <Send class="size-4" aria-hidden="true" />
          </button>
        </form>
      </template>
    </section>

    <button
      type="button"
      class="ml-auto grid size-14 place-items-center rounded-full bg-ink text-paper shadow-xl transition hover:-translate-y-0.5 hover:bg-mint"
      :aria-expanded="open"
      aria-label="Открыть чат"
      @click="toggle"
    >
      <MessageCircle class="size-6" aria-hidden="true" />
    </button>
  </div>
</template>
