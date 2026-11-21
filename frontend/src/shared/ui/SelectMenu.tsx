import { Check, ChevronDown } from 'lucide-react'
import {
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent,
  type Ref,
} from 'react'

export interface SelectMenuOption {
  value: string
  label: string
  /** 选项右边的灰色小字，例如数量 */
  hint?: string
}

interface SelectMenuProps {
  options: SelectMenuOption[]
  value: string
  onChange: (value: string) => void
  /** field：和 Input 一样的 4px 边框方框（表单里用）；pill：白色药丸加轻阴影（工具栏里用） */
  variant?: 'field' | 'pill'
  size?: 'sm' | 'md'
  /** 没选时按钮上显示的文字 */
  placeholder?: string
  /** 当前值前面的灰字，例如 "Sort" */
  prefix?: string
  /** 菜单和按钮的哪一边对齐。按钮靠页面右边时用 end，菜单就不会超出屏幕 */
  align?: 'start' | 'end'
  /** Field 传进来的 id、aria-invalid、aria-describedby，交给按钮 */
  id?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
  'aria-label'?: string
  /** react-hook-form 的 Controller 传进来的 ref 和 onBlur：校验失败时能聚焦到按钮 */
  ref?: Ref<HTMLButtonElement>
  onBlur?: () => void
  /** 加在最外层，用来控制宽度 */
  className?: string
}

const triggerClass = {
  field:
    'flex w-full justify-between rounded-xs border bg-surface text-body-sm leading-5.5 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/25 aria-invalid:border-danger',
  pill: 'inline-flex rounded-full bg-surface font-medium shadow-card hover:bg-surface-muted active:scale-96 focus-visible:ring-2 focus-visible:ring-primary/25',
}
const triggerSize = {
  field: { sm: 'px-2 py-0.5', md: 'py-1.5 pr-2 pl-2.5' },
  pill: {
    sm: 'py-1 pr-2.5 pl-3.5 text-body-sm',
    md: 'py-2 pr-3.5 pl-4.5 text-base',
  },
}

/**
 * 自己画菜单的下拉框（新版设计系统的 SelectMenu）。和原生 <select> 比，菜单的样子各浏览器一致，
 * 选项右边能放数量，选中的打勾。
 *
 * 键盘：按钮上 ↑ / ↓ / Enter / 空格打开；菜单里 ↑ / ↓、Home / End 移动，Enter / 空格选择，
 * Esc 或 Tab 关闭并回到按钮，直接输入字符跳到以它开头的选项（例如输入 "14" 跳到 14:00）。
 * 读屏软件：按钮是 combobox，菜单是 listbox，当前高亮的选项用 aria-activedescendant 告诉它。
 *
 * 菜单用 absolute 定位在按钮下面（下方放不下时在上面），所以外层不能有 overflow-hidden。
 */
export function SelectMenu({
  options,
  value,
  onChange,
  variant = 'field',
  size = 'md',
  placeholder = 'Select…',
  prefix,
  align = 'start',
  id,
  ref,
  onBlur,
  className = '',
  ...aria
}: SelectMenuProps) {
  const [open, setOpen] = useState(false)
  // 高亮的选项（鼠标移上去或键盘移动到的），不一定是已选中的
  const [active, setActive] = useState(-1)
  const [above, setAbove] = useState(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const listRef = useRef<HTMLDivElement>(null)
  const typed = useRef({ text: '', timer: 0 })
  const baseId = useId()
  const buttonId = id ?? `${baseId}-button`
  const listId = `${baseId}-list`
  const selectedIndex = options.findIndex((o) => o.value === value)
  const selected = options[selectedIndex]

  // 打开后把焦点交给菜单，方便键盘操作；点到组件外面就关闭
  useEffect(() => {
    if (!open) return
    listRef.current?.focus({ preventScroll: true })
    function onMouseDown(event: MouseEvent) {
      if (!wrapperRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [open])

  // 高亮的选项滚进菜单的可见区域（选项多时，例如 48 个时间）
  useEffect(() => {
    if (!open || active < 0) return
    listRef.current
      ?.querySelector(`[data-index="${active}"]`)
      ?.scrollIntoView({ block: 'nearest' })
  }, [open, active])

  function openMenu() {
    // 按钮下方不到 200px 且上方更宽敞时，菜单向上弹出
    const rect = buttonRef.current?.getBoundingClientRect()
    if (rect) {
      const below = window.innerHeight - rect.bottom
      setAbove(below < 200 && rect.top > below)
    }
    setActive(Math.max(selectedIndex, 0))
    setOpen(true)
  }

  function close() {
    setOpen(false)
    buttonRef.current?.focus()
  }

  function choose(index: number) {
    const option = options[index]
    if (option) onChange(option.value)
    close()
  }

  /** 输入字符跳到以它开头的选项；0.5 秒内连续输入的字符拼在一起匹配 */
  function jumpTo(char: string) {
    const t = typed.current
    window.clearTimeout(t.timer)
    t.text += char.toLowerCase()
    t.timer = window.setTimeout(() => (t.text = ''), 500)
    // 只输入一个字符时从下一项找起，连续按同一个字母可以轮流跳
    const start = t.text.length > 1 ? Math.max(active, 0) : active + 1
    for (let k = 0; k < options.length; k++) {
      const i = (start + k) % options.length
      if (options[i].label.toLowerCase().startsWith(t.text)) {
        setActive(i)
        return
      }
    }
  }

  function onButtonKeyDown(event: KeyboardEvent) {
    if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) {
      event.preventDefault()
      openMenu()
    }
  }

  function onListKeyDown(event: KeyboardEvent) {
    const last = options.length - 1
    const keys: Record<string, () => void> = {
      ArrowDown: () => setActive((a) => Math.min(a + 1, last)),
      ArrowUp: () => setActive((a) => Math.max(a - 1, 0)),
      Home: () => setActive(0),
      End: () => setActive(last),
      Enter: () => choose(active),
      ' ': () => choose(active),
      Escape: close,
      Tab: close,
    }
    const action = keys[event.key]
    if (action) {
      event.preventDefault()
      action()
    } else if (event.key.length === 1) {
      jumpTo(event.key)
    }
  }

  return (
    <div
      ref={wrapperRef}
      className={`relative ${variant === 'pill' ? 'inline-block' : 'block w-full'} ${className}`}
    >
      <button
        ref={(node) => {
          buttonRef.current = node
          if (typeof ref === 'function') ref(node)
          else if (ref) ref.current = node
        }}
        id={buttonId}
        type="button"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        {...aria}
        onClick={() => (open ? close() : openMenu())}
        onKeyDown={onButtonKeyDown}
        onBlur={() => {
          if (!open) onBlur?.()
        }}
        className={`cursor-pointer items-center gap-2 text-left whitespace-nowrap text-text-strong transition focus-visible:outline-none ${triggerClass[variant]} ${triggerSize[variant][size]} ${
          variant === 'field'
            ? open
              ? 'border-primary shadow-card'
              : 'border-border-input'
            : ''
        }`}
      >
        <span className="flex min-w-0 items-center gap-2 overflow-hidden">
          {prefix && (
            <span className="font-normal text-text-muted">{prefix}</span>
          )}
          {/* 设计里占位文字是 text-faint，对比度太低，这里用 text-muted */}
          <span className={`truncate ${selected ? '' : 'text-text-muted'}`}>
            {selected ? selected.label : placeholder}
          </span>
        </span>
        <ChevronDown
          size={16}
          aria-hidden
          className="shrink-0 text-text-muted opacity-70"
        />
      </button>

      {open && (
        <div
          ref={listRef}
          id={listId}
          role="listbox"
          tabIndex={-1}
          aria-labelledby={aria['aria-label'] ? undefined : buttonId}
          aria-label={aria['aria-label']}
          aria-activedescendant={
            active >= 0 ? `${baseId}-option-${active}` : undefined
          }
          onKeyDown={onListKeyDown}
          onBlur={onBlur}
          // starting:* 是打开时的起始样式：从 95% 大小、透明，过渡到正常
          className={`absolute z-20 max-h-72 min-w-[max(100%,8rem)] overflow-y-auto rounded-md border border-border bg-surface p-1 shadow-md transition-[opacity,scale] duration-150 outline-none starting:scale-95 starting:opacity-0 ${
            above
              ? 'bottom-full mb-1 origin-bottom'
              : 'top-full mt-1 origin-top'
          } ${align === 'end' ? 'right-0' : 'left-0'}`}
        >
          {options.map((option, i) => {
            const isSelected = i === selectedIndex
            return (
              // 键盘由外层 listbox 统一处理（aria-activedescendant 模式），焦点一直在 listbox 上，
              // 选项本身不需要能获得焦点，也不需要自己的键盘事件
              // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
              <div
                key={option.value}
                id={`${baseId}-option-${i}`}
                data-index={i}
                role="option"
                aria-selected={isSelected}
                onMouseMove={() => setActive(i)}
                onClick={() => choose(i)}
                className={`relative flex cursor-pointer items-center gap-2 rounded-sm py-1.5 pr-8 pl-2 text-body-sm whitespace-nowrap text-text-strong select-none ${i === active ? 'bg-black/5' : ''}`}
              >
                <span className="flex-1">{option.label}</span>
                {option.hint && (
                  <span className="text-caption text-text-faint tabular-nums">
                    {option.hint}
                  </span>
                )}
                {isSelected && (
                  <Check
                    size={16}
                    aria-hidden
                    className="absolute right-2 text-text-strong"
                  />
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
