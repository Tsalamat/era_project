const methodsWithBody = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const rawPath = getRouterParam(event, 'path') || ''
  const url = getRequestURL(event)
  const method = getMethod(event)
  const headers = new Headers()
  const accessToken = getCookie(event, 'kta_access_token')

  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  const contentType = getHeader(event, 'content-type')
  if (contentType) {
    headers.set('Content-Type', contentType)
  }
  const isMultipart = contentType?.includes('multipart/form-data')

  try {
    const response = await $fetch.raw(`/${rawPath.replace(/^\/+/, '')}${url.search}`, {
      baseURL: String(config.apiBase),
      method,
      headers,
      body: methodsWithBody.has(method) ? isMultipart ? await readRawBody(event, false) : await readBody(event) : undefined
    })
    setResponseStatus(event, response.status)
    return response._data
  } catch (error) {
    const requestError = error as {
      statusCode?: number
      response?: { status?: number; statusText?: string }
      data?: { detail?: string; message?: string; error?: string }
      message?: string
    }
    throw createError({
      statusCode: requestError.response?.status || requestError.statusCode || 500,
      statusMessage: requestError.data?.detail || requestError.data?.message || requestError.data?.error || requestError.response?.statusText || requestError.message || 'Не удалось выполнить запрос'
    })
  }
})
