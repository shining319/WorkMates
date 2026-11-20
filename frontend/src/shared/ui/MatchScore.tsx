// 匹配度。三档：>=70 高、40-69 中、<40 低。颜色只用在进度条和圆环上，数字始终显示。
const tone = (percent: number) =>
  percent >= 70 ? 'match-high' : percent >= 40 ? 'match-mid' : 'match-low'

const barClass = {
  'match-high': 'bg-match-high',
  'match-mid': 'bg-match-mid',
  'match-low': 'bg-match-low',
}
const ringClass = {
  'match-high': 'text-match-high',
  'match-mid': 'text-match-mid',
  'match-low': 'text-match-low',
}

// 圆环：半径 34，周长 2πr。stroke-dashoffset 控制画多少。
const RADIUS = 34
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/**
 * `size="sm"`：40px 的小进度条加 "85%" 和 `label`（设计系统 MatchScore，职位卡片、申请人列表）。
 * `size="lg"`：圆环，中间是百分数（职位详情）。
 */
export function MatchScore({
  percent,
  size = 'sm',
  label = 'match',
}: {
  /** 0–100 的整数 */
  percent: number
  size?: 'sm' | 'lg'
  /** 百分数后面的小字；申请人列表的表头已经写了 Match，传空字符串 */
  label?: string
}) {
  const level = tone(percent)
  if (size === 'sm') {
    return (
      <span className="inline-flex items-center gap-2 tabular-nums">
        {/* 进度条只是装饰，数字已经写出来了 */}
        <span
          aria-hidden
          className="h-1 w-10 overflow-hidden rounded-xs bg-black/8"
        >
          <span
            className={`block h-full rounded-xs ${barClass[level]}`}
            style={{ width: `${percent}%` }}
          />
        </span>
        <span className="text-body-sm font-semibold text-text-strong">
          {percent}%
        </span>
        {label && <span className="text-caption text-text-muted">{label}</span>}
        {/* 不显示 label 时，给读屏软件补上 "match" */}
        {!label && <span className="sr-only">match</span>}
      </span>
    )
  }
  return (
    <div
      role="img"
      aria-label={`${percent}% match`}
      className="relative size-21 shrink-0"
    >
      <svg viewBox="0 0 84 84" aria-hidden className="size-full -rotate-90">
        <circle
          cx="42"
          cy="42"
          r={RADIUS}
          fill="none"
          stroke="currentColor"
          strokeWidth="6"
          className="text-black/6"
        />
        <circle
          cx="42"
          cy="42"
          r={RADIUS}
          fill="none"
          stroke="currentColor"
          strokeWidth="6"
          strokeLinecap="round"
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={CIRCUMFERENCE * (1 - percent / 100)}
          className={ringClass[level]}
        />
      </svg>
      <span
        aria-hidden
        className="absolute inset-0 flex flex-col items-center justify-center text-text-strong tabular-nums"
      >
        <span className="text-title leading-none">{percent}%</span>
        <span className="text-eyebrow text-text-muted">match</span>
      </span>
    </div>
  )
}
