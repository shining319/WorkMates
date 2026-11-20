import { formatDateTime } from '../../../shared/lib/format'
import { Card } from '../../../shared/ui/Card'
import { StatusBadge } from '../../../shared/ui/StatusBadge'
import type { ActorType, HistoryStep } from '../types'

/**
 * 状态历史卡片（设计 09、14），学生和雇主共用。新的在上，每步：状态、谁改的、什么时候（都柏林时间）。
 * `actorName` 由页面决定怎么称呼三种操作方，例如学生页面把 STUDENT 叫 "you"。
 */
export function StatusHistory({
  history,
  actorName,
}: {
  /** 后端给的顺序：旧到新 */
  history: HistoryStep[]
  actorName: Record<ActorType, string>
}) {
  const newestFirst = [...history].reverse()
  return (
    <Card className="p-5">
      <h2 className="mb-3 text-title">Status history</h2>
      <ol className="flex flex-col">
        {newestFirst.map((step, i) => (
          <li
            key={`${step.toStatus}-${step.createdAt}`}
            className="relative flex gap-3 pb-4 last:pb-0"
          >
            {/* 左边的圆点和竖线只是装饰；最新一步是实心圆点 */}
            <span aria-hidden className="relative flex w-3 justify-center">
              <span
                className={`mt-1.5 size-2.5 rounded-full border-[1.5px] border-text-strong ${i === 0 ? 'bg-text-strong' : 'bg-surface'}`}
              />
              {i < newestFirst.length - 1 && (
                <span className="absolute top-4.5 -bottom-1.5 w-px bg-border" />
              )}
            </span>
            <div className="flex min-w-0 flex-1 flex-wrap items-start justify-between gap-x-3 gap-y-1">
              <div className="flex flex-col items-start gap-1">
                <StatusBadge status={step.toStatus} reason={step.reason} />
                <span className="text-caption text-text-muted">
                  by {actorName[step.actorType]}
                </span>
              </div>
              <time
                dateTime={step.createdAt}
                className="text-caption text-text-muted tabular-nums"
              >
                {formatDateTime(step.createdAt)}
              </time>
            </div>
          </li>
        ))}
      </ol>
    </Card>
  )
}
