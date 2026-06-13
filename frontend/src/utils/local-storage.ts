const FAVORITES_KEY = 'local_favorites'
const HISTORY_KEY = 'local_read_history'
const PROGRESS_KEY = 'local_reading_progress'

interface LocalFavorite {
  postId: number
  note: string | null
  createdAt: string
}

interface LocalHistory {
  postId: number
  readAt: string
  durationSeconds: number
  scrollPosition: number
}

interface LocalProgress {
  postId: number
  durationSeconds: number
  scrollPosition: number
}

function load<T>(key: string): T[] {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function save<T>(key: string, items: T[]) {
  localStorage.setItem(key, JSON.stringify(items))
}

export const localFavorites = {
  getAll: (): LocalFavorite[] => load<LocalFavorite>(FAVORITES_KEY),
  add: (postId: number, note?: string) => {
    const items = load<LocalFavorite>(FAVORITES_KEY)
    if (!items.find(i => i.postId === postId)) {
      items.push({ postId, note: note || null, createdAt: new Date().toISOString() })
      save(FAVORITES_KEY, items)
    }
  },
  remove: (postId: number) => {
    save(FAVORITES_KEY, load<LocalFavorite>(FAVORITES_KEY).filter(i => i.postId !== postId))
  },
  has: (postId: number): boolean => load<LocalFavorite>(FAVORITES_KEY).some(i => i.postId === postId),
  clear: () => save(FAVORITES_KEY, []),
  getAllForSync: () => load<LocalFavorite>(FAVORITES_KEY),
}

export const localHistory = {
  getAll: (): LocalHistory[] => load<LocalHistory>(HISTORY_KEY),
  record: (postId: number) => {
    const items = load<LocalHistory>(HISTORY_KEY)
    const existing = items.find(i => i.postId === postId)
    if (existing) {
      existing.readAt = new Date().toISOString()
    } else {
      items.push({ postId, readAt: new Date().toISOString(), durationSeconds: 0, scrollPosition: 0 })
    }
    save(HISTORY_KEY, items)
  },
  updateProgress: (postId: number, durationSeconds: number, scrollPosition: number) => {
    const items = load<LocalHistory>(HISTORY_KEY)
    const existing = items.find(i => i.postId === postId)
    if (existing) {
      existing.durationSeconds = Math.max(existing.durationSeconds, durationSeconds)
      existing.scrollPosition = scrollPosition
      save(HISTORY_KEY, items)
    }
  },
  getProgress: (postId: number): LocalProgress | null => {
    const item = load<LocalHistory>(HISTORY_KEY).find(i => i.postId === postId)
    return item ? { postId: item.postId, durationSeconds: item.durationSeconds, scrollPosition: item.scrollPosition } : null
  },
  clear: () => save(HISTORY_KEY, []),
  getAllForSync: () => load<LocalHistory>(HISTORY_KEY),
}

export async function syncLocalDataToServer(recordRead: (postId: number) => Promise<void>, toggleFavorite: (postId: number) => Promise<void>) {
  const favorites = localFavorites.getAllForSync()
  const history = localHistory.getAllForSync()

  for (const fav of favorites) {
    try { await toggleFavorite(fav.postId) } catch { /* ignore */ }
  }

  for (const h of history) {
    try { await recordRead(h.postId) } catch { /* ignore */ }
  }

  localFavorites.clear()
  localHistory.clear()
}
