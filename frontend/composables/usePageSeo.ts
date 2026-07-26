type PageSeoInput = {
  title: string
  description: string
  path: string
  image?: string
  keywords?: string
  robots?: string
  type?: 'website' | 'article'
  publishedTime?: string
  modifiedTime?: string
  schema?: Record<string, unknown>
}

export const usePageSeo = ({
  title,
  description,
  path,
  image,
  keywords,
  robots = 'index,follow',
  type = 'website',
  publishedTime,
  modifiedTime,
  schema
}: PageSeoInput) => {
  const config = useRuntimeConfig()
  const siteUrl = String(config.public.siteUrl).replace(/\/$/, '')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const canonical = `${siteUrl}${normalizedPath}`
  const imageUrl = image
    ? (image.startsWith('http') ? image : `${siteUrl}${image}`)
    : `${siteUrl}/og-cover.svg`
  const meta = [
    { name: 'description', content: description },
    { name: 'robots', content: robots },
    { property: 'og:site_name', content: 'Тест Магистратура' },
    { property: 'og:type', content: type },
    { property: 'og:title', content: title },
    { property: 'og:description', content: description },
    { property: 'og:url', content: canonical },
    { property: 'og:image', content: imageUrl },
    { name: 'twitter:card', content: 'summary_large_image' },
    { name: 'twitter:title', content: title },
    { name: 'twitter:description', content: description },
    { name: 'twitter:url', content: canonical },
    { name: 'twitter:image', content: imageUrl }
  ]

  if (keywords) {
    meta.push({ name: 'keywords', content: keywords })
  }

  if (publishedTime) {
    meta.push({ property: 'article:published_time', content: publishedTime })
  }

  if (modifiedTime) {
    meta.push({ property: 'article:modified_time', content: modifiedTime })
  }

  useHead({
    title,
    meta,
    link: [{ rel: 'canonical', href: canonical }],
    script: schema
      ? [
          {
            type: 'application/ld+json',
            innerHTML: JSON.stringify(schema)
          }
        ]
      : []
  })
}
