import { X } from 'lucide-react'

interface FilterChipProps {
  label: string
  onRemove: () => void
}

/** 一个生效中的筛选条件：文字加删除按钮（设计稿的 Tag）。 */
export function FilterChip({ label, onRemove }: FilterChipProps) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-black/6 py-0.5 pr-1 pl-2.5 text-caption text-text-strong">
      {label}
      <button
        type="button"
        onClick={onRemove}
        aria-label={`Remove filter: ${label}`}
        className="inline-flex size-5 cursor-pointer items-center justify-center rounded-full text-text-muted hover:bg-black/8 hover:text-text-strong"
      >
        <X size={12} aria-hidden />
      </button>
    </span>
  )
}
