import { useEffect, useState } from 'react'
import { useGetTagsQuery } from './api'

/**
 * 给 `TagInput` 提供候选词：输入停顿 250ms 后才去请求，避免每敲一个字母就发一次请求。
 * 返回 `[候选词, 通知输入内容变化的函数]`，分别传给 TagInput 的 `suggestions` 和 `onQueryChange`。
 */
export function useTagSuggestions(): [string[], (query: string) => void] {
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')

  useEffect(() => {
    const timer = setTimeout(
      () => setDebounced(query.trim().toLowerCase()),
      250,
    )
    // 输入又变了，就取消上一次还没到时间的计时
    return () => clearTimeout(timer)
  }, [query])

  // 输入为空时不请求
  const { data } = useGetTagsQuery(debounced, { skip: debounced === '' })
  const suggestions = query.trim() === '' ? [] : (data ?? []).map((t) => t.name)

  return [suggestions, setQuery]
}
