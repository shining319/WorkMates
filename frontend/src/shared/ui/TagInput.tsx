import { X } from 'lucide-react'
import { useId, useState, type KeyboardEvent } from 'react'

const MAX_TAG_LENGTH = 50

interface TagInputProps {
  /** 由 Field 传进来，让标签和输入框对上 */
  id?: string
  'aria-describedby'?: string
  'aria-invalid'?: boolean
  /** 已选的标签（都是小写） */
  value: string[]
  onChange: (tags: string[]) => void
  /** 最多几个标签。学生是 20；不传表示不限（职位的技能标签） */
  max?: number
  /** 候选词。由外面按输入内容去请求，这里只负责显示 */
  suggestions: string[]
  /** 输入内容变了就通知外面（清空时传空字符串） */
  onQueryChange: (query: string) => void
}

/**
 * 标签输入框：输入文字，回车添加；下面的候选词可以用上下键选择。
 * 标签和候选词都按"去空格、转小写"处理，大小写不同的重复标签会被忽略（和后端一致）。
 *
 * 键盘：Enter 添加（有选中的候选词就加候选词）、↑↓ 在候选词里移动、
 * Esc 收起候选词、输入框为空时 Backspace 删掉最后一个标签。
 * 读屏软件：输入框是 combobox，候选词是 listbox，aria-activedescendant 指出当前选中项。
 */
export function TagInput({
  value,
  onChange,
  max,
  suggestions,
  onQueryChange,
  ...controlProps
}: TagInputProps) {
  const listId = useId()
  const [text, setText] = useState('')
  // -1 表示没有选中任何候选词
  const [active, setActive] = useState(-1)
  const [open, setOpen] = useState(false)

  const typed = text.trim().toLowerCase()
  const atLimit = max !== undefined && value.length >= max

  // 候选词里去掉已选的；如果输入的内容不在候选词里，最后加一项"新建这个标签"。
  const options = suggestions.filter((name) => !value.includes(name))
  if (typed && !value.includes(typed) && !options.includes(typed)) {
    options.push(typed)
  }
  const showList = open && !atLimit && typed !== '' && options.length > 0

  function reset() {
    setText('')
    setActive(-1)
    onQueryChange('')
  }

  function add(raw: string) {
    const name = raw.trim().toLowerCase()
    // 已经有的标签直接忽略；到上限后不再添加
    if (name && !value.includes(name) && !atLimit) {
      onChange([...value, name])
    }
    reset()
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      // 输入框在表单里，不拦住的话回车会提交整个表单
      event.preventDefault()
      add(showList && active >= 0 ? options[active] : text)
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      if (options.length === 0) return
      event.preventDefault()
      setOpen(true)
      const step = event.key === 'ArrowDown' ? 1 : -1
      setActive((active + step + options.length) % options.length)
    } else if (event.key === 'Escape') {
      setOpen(false)
      setActive(-1)
    } else if (event.key === 'Backspace' && text === '' && value.length > 0) {
      onChange(value.slice(0, -1))
    }
  }

  return (
    <div className="relative">
      <div
        className={`flex min-h-9 flex-wrap items-center gap-1.5 rounded-xs border bg-surface px-2.5 py-1.5 transition-colors focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/25 ${controlProps['aria-invalid'] ? 'border-danger' : 'border-border-input'}`}
      >
        {value.map((name) => (
          <span
            key={name}
            className="inline-flex items-center gap-1 rounded-xs bg-black/5 px-1.5 py-px text-caption leading-5 text-text"
          >
            {name}
            <button
              type="button"
              aria-label={`Remove ${name}`}
              onClick={() => onChange(value.filter((tag) => tag !== name))}
              className="inline-flex cursor-pointer rounded-xs opacity-60 hover:opacity-100"
            >
              <X size={12} aria-hidden />
            </button>
          </span>
        ))}
        <input
          {...controlProps}
          role="combobox"
          aria-expanded={showList}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={
            showList && active >= 0 ? `${listId}-${active}` : undefined
          }
          autoComplete="off"
          maxLength={MAX_TAG_LENGTH}
          value={text}
          placeholder={value.length === 0 ? 'e.g. java, english' : undefined}
          onChange={(event) => {
            setText(event.target.value)
            setActive(-1)
            setOpen(true)
            onQueryChange(event.target.value)
          }}
          onKeyDown={onKeyDown}
          onBlur={() => setOpen(false)}
          className="min-w-24 flex-1 bg-transparent text-body-sm leading-5.5 text-text-strong placeholder:text-text-faint focus-visible:outline-none"
        />
      </div>

      {showList && (
        <ul
          id={listId}
          role="listbox"
          className="absolute top-full left-0 z-10 mt-1 w-full max-w-65 rounded-md border border-border bg-surface p-1 shadow-card"
        >
          {options.map((name, index) => {
            const isNew = !suggestions.includes(name)
            return (
              // 这是 combobox：键盘操作都在输入框上处理（↑↓ 选、Enter 加），候选词本身不需要键盘监听
              // eslint-disable-next-line jsx-a11y/click-events-have-key-events
              <li
                key={name}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={index === active}
                // mousedown 默认会让输入框失焦、候选词收起，点击就丢了，所以先拦住
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => add(name)}
                className={`cursor-pointer rounded-sm px-2.5 py-1.5 hover:bg-black/4 ${index === active ? 'bg-black/4' : ''} ${isNew ? 'text-caption text-text-muted' : 'text-body-sm text-text-strong'}`}
              >
                {isNew ? `Add “${name}” as a new tag` : name}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
