import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'

interface PaginationProps {
  /** 当前页，从 1 开始（和网址里的 ?page= 一致） */
  page: number
  totalPages: number
  onChange: (page: number) => void
}

/** 上一页 / 下一页。只有一页时不显示。 */
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null
  return (
    <nav
      aria-label="Pagination"
      className="flex items-center justify-center gap-3"
    >
      <Button
        variant="utility"
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
      >
        <ChevronLeft size={16} aria-hidden />
        Previous
      </Button>
      <span className="text-caption text-text-muted tabular-nums">
        Page {page} of {totalPages}
      </span>
      <Button
        variant="utility"
        disabled={page >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        Next
        <ChevronRight size={16} aria-hidden />
      </Button>
    </nav>
  )
}
