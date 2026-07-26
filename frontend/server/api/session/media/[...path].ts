export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const rawPath = getRouterParam(event, 'path') || ''
  const url = getRequestURL(event)
  const accessToken = getCookie(event, 'kta_access_token')
  const headers = new Headers()

  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const range = getHeader(event, 'range')
  if (range) headers.set('Range', range)

  const upstream = await fetch(`${String(config.apiBase).replace(/\/$/, '')}/${rawPath.replace(/^\/+/, '')}${url.search}`, {
    headers
  })

  setResponseStatus(event, upstream.status)
  for (const header of ['accept-ranges', 'cache-control', 'content-disposition', 'content-length', 'content-range', 'content-type']) {
    const value = upstream.headers.get(header)
    if (value) setHeader(event, header, value)
  }

  if (!upstream.ok) {
    throw createError({
      statusCode: upstream.status,
      statusMessage: await upstream.text() || upstream.statusText || 'Не удалось открыть медиафайл'
    })
  }
  if (!upstream.body) return ''
  return sendStream(event, upstream.body)
})
