import { describe, expect, it } from 'vitest'
import { truncateText } from './text'

describe('truncateText', () => {
  it('should return original text when shorter than limit', () => {
    expect(truncateText('hello world', 20)).toBe('hello world')
  })

  it('should truncate text when over limit', () => {
    expect(truncateText('hello world from test', 5)).toBe('hello...')
  })
})
