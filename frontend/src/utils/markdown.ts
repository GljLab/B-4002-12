import MarkdownIt from 'markdown-it'
import markdownItTaskLists from 'markdown-it-task-lists'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'

export interface TocItem {
  level: number
  text: string
  id: string
  children: TocItem[]
}

function slugify(text: string): string {
  return text
    .toLowerCase()
    .trim()
    .replace(/[\s]+/g, '-')
    .replace(/[^\w\-]/g, '')
    .replace(/\-+/g, '-')
    .replace(/^-+|-+$/g, '')
}

function buildTocTree(items: Omit<TocItem, 'children'>[]): TocItem[] {
  const root: TocItem[] = []
  const stack: TocItem[] = []

  items.forEach((item) => {
    const newItem: TocItem = { ...item, children: [] }

    while (stack.length > 0 && stack[stack.length - 1].level >= newItem.level) {
      stack.pop()
    }

    if (stack.length === 0) {
      root.push(newItem)
    } else {
      stack[stack.length - 1].children.push(newItem)
    }

    stack.push(newItem)
  })

  return root
}

export function createMarkdownRenderer() {
  const tocItems: Omit<TocItem, 'children'>[] = []
  let headingCounter = 0

  const md = new MarkdownIt({
    html: false,
    linkify: true,
    typographer: true,
    highlight(str: string, lang: string): string {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return `<pre class="hljs"><code class="language-${lang}">${
            hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
          }</code></pre>`
        } catch {
          // fall through
        }
      }
      return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
    },
  })

  md.use(markdownItTaskLists, { label: true, labelAfter: true })

  const defaultRender = md.renderer.rules.image!
  md.renderer.rules.image = (tokens: any[], idx: number, options: any, env: any, self: any): string => {
    const token = tokens[idx]
    const src = token.attrGet('src') || ''
    const alt = token.content || ''
    return `<img src="${src}" alt="${alt}" loading="lazy" class="md-image" data-preview="true" />`
  }

  const defaultLinkRender: any = md.renderer.rules.link_open! || function(
    tokens: any[],
    idx: number,
    options: any,
    env: any,
    self: any,
  ): string {
    return self.renderToken(tokens, idx, options)
  }
  md.renderer.rules.link_open = (tokens: any[], idx: number, options: any, env: any, self: any): string => {
    const token = tokens[idx]
    const href = token.attrGet('href') || ''
    if (href && !href.startsWith('#')) {
      token.attrSet('target', '_blank')
      token.attrSet('rel', 'noopener noreferrer')
    }
    return defaultLinkRender(tokens, idx, options, env, self)
  }

  md.renderer.rules.heading_open = (tokens: any[], idx: number, options: any, env: any, self: any): string => {
    const token = tokens[idx]
    const level = parseInt(token.tag.replace('h', ''), 10)
    const nextToken = tokens[idx + 1]
    const text = nextToken?.content || ''
    headingCounter++
    const id = `heading-${headingCounter}-${slugify(text)}`
    token.attrSet('id', id)
    token.attrSet('class', 'md-heading')
    tocItems.push({ level, text, id })
    return self.renderToken(tokens, idx, options)
  }

  function render(content: string): { html: string; toc: TocItem[] } {
    tocItems.length = 0
    headingCounter = 0
    const rawHtml = md.render(content)
    const cleanHtml = DOMPurify.sanitize(rawHtml, {
      ADD_ATTR: ['target', 'rel', 'loading', 'data-preview', 'class', 'id'],
    })
    const toc = buildTocTree([...tocItems])
    return { html: cleanHtml, toc }
  }

  function renderToHtml(content: string): string {
    return render(content).html
  }

  function getToc(content: string): TocItem[] {
    return render(content).toc
  }

  return {
    render,
    renderToHtml,
    getToc,
  }
}

export const markdownRenderer = createMarkdownRenderer()

export function countWords(text: string): number {
  if (!text) return 0
  const chineseChars = (text.match(/[\u4e00-\u9fa5]/g) || []).length
  const englishWords = text
    .replace(/[\u4e00-\u9fa5]/g, ' ')
    .split(/\s+/)
    .filter((w) => w.length > 0).length
  return chineseChars + englishWords
}

export function countCharacters(text: string): number {
  if (!text) return 0
  return text.length
}

export const MAX_IMAGE_SIZE = 5 * 1024 * 1024

export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']

export function validateImageFile(file: File): { valid: boolean; message?: string } {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return {
      valid: false,
      message: `不支持的文件类型 "${file.type}"，仅支持 JPG、PNG、GIF、WEBP、SVG 格式`,
    }
  }
  if (file.size > MAX_IMAGE_SIZE) {
    return {
      valid: false,
      message: `图片大小超过限制 (${(file.size / 1024 / 1024).toFixed(2)}MB)，最大支持 5MB`,
    }
  }
  return { valid: true }
}

export function getMarkdownImageSyntax(url: string, alt: string = 'image'): string {
  return `![${alt}](${url})`
}

export function extractImageFilesFromClipboard(event: ClipboardEvent): File[] {
  const items = event.clipboardData?.items
  if (!items) return []

  const files: File[] = []
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        files.push(file)
      }
    }
  }
  return files
}

export function extractImageFilesFromDataTransfer(dataTransfer: DataTransfer): File[] {
  const files: File[] = []
  if (dataTransfer.files) {
    for (let i = 0; i < dataTransfer.files.length; i++) {
      const file = dataTransfer.files[i]
      if (file.type.startsWith('image/')) {
        files.push(file)
      }
    }
  }
  return files
}
