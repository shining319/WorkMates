import { useEffect, useState } from 'react'

const DELAY_MS = 300

/**
 * 输入框的文字和网址参数同步：用户打字时先改输入框，停顿 300ms 再调用 `commit` 写进网址；
 * 网址被别的方式改了（后退、清除筛选、粘贴链接）时，输入框跟着变。
 * 返回值和 useState 一样：[文字, 设置文字]。
 */
export function useUrlText(
  urlValue: string,
  commit: (value: string) => void,
): [string, (value: string) => void] {
  const [text, setText] = useState(urlValue)
  // synced：输入框上一次和网址对齐的值。网址和它不一样，说明网址是被别处改的
  const [synced, setSynced] = useState(urlValue)

  // 在渲染时直接调整 state 是 React 推荐的写法（比 useEffect 少闪一次）
  if (urlValue !== synced) {
    setSynced(urlValue)
    setText(urlValue)
  }

  useEffect(() => {
    if (text === synced) return
    const timer = setTimeout(() => {
      setSynced(text)
      commit(text)
    }, DELAY_MS)
    return () => clearTimeout(timer)
  }, [text, synced, commit])

  return [text, setText]
}
