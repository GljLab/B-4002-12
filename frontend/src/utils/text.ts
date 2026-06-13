export function truncateText(text: string, length: number): string {
  const normalized = text.trim().replace(/\s+/g, ' ')
  if (normalized.length <= length) {
    return normalized
  }
  return `${normalized.slice(0, length)}...`
}
