// 匹配度。三档：>=70 高、40-69 中、<40 低。颜色只用在圆点和圆环上，数字和文字始终显示。
const tone = (percent: number) =>
  percent >= 70 ? 'match-high' : percent >= 40 ? 'match-mid' : 'match-low'

const dotClass = {
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
 * `size="sm"`：圆点加文字 "85% match"（职位卡片）。
 * `size="lg"`：圆环，中间是百分数（职位详情）。
 */
export function MatchScore({
  percent,
  size = 'sm',
}: {
  /** 0–100 的整数 */
  percent: number
  size?: 'sm' | 'lg'
}) {
  const level = tone(percent)
  if (size === 'sm') {
    return (
      <span className="inline-flex items-center gap-1.5 text-caption font-semibold text-text-strong tabular-nums">
        <span
          aria-hidden
          className={`size-2 rounded-full ${dotClass[level]}`}
        />
        {percent}% match
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
