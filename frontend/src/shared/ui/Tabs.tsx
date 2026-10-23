interface Tab {
  id: string
  label: string
  /** 这个标签页里有几条。没有就不显示 */
  count?: number
}

interface TabsProps {
  /** 读屏软件会先读它，例如 "Posting status" */
  label: string
  tabs: Tab[]
  value: string
  onChange: (id: string) => void
}

/**
 * 标签页（设计稿的 Tabs）：当前页签有下划线，旁边可以带数量。
 * 它只负责"选哪一个"，下面的内容由页面自己根据 value 显示。
 * 当前页签用 aria-pressed 标出，读屏软件会读成"已按下"。
 */
export function Tabs({ label, tabs, value, onChange }: TabsProps) {
  return (
    <div
      role="group"
      aria-label={label}
      className="flex gap-5 border-b border-border"
    >
      {tabs.map((tab) => {
        const active = tab.id === value
        return (
          <button
            key={tab.id}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(tab.id)}
            className={`-mb-px inline-flex cursor-pointer items-center gap-1.5 border-b-2 py-2 text-body-sm ${active ? 'border-text-strong font-semibold text-text-strong' : 'border-transparent text-text-muted hover:text-text'}`}
          >
            {tab.label}
            {tab.count !== undefined && (
              <span className="rounded-full bg-black/5 px-1.5 text-eyebrow font-medium text-text-muted tabular-nums">
                {tab.count}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
