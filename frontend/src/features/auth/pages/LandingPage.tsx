import {
  Bell,
  CalendarCheck,
  ClipboardList,
  GraduationCap,
  Store,
  type LucideIcon,
} from 'lucide-react'
import { Fragment } from 'react'
import { Link, Navigate } from 'react-router'
import { useAppSelector } from '../../../app/store'
import { ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { useGetMeQuery } from '../api'

const HERO_TITLE =
  'Stop scrolling through 37 WhatsApp groups to find a part-time job.'

const features: { icon: LucideIcon; title: string; text: string }[] = [
  {
    icon: ClipboardList,
    title: 'Every job in one place',
    text: 'Hourly rate, hours, and location, spelled out.',
  },
  {
    icon: GraduationCap,
    title: 'Campus email sign-up',
    text: 'Real students and real employers only.',
  },
  {
    icon: CalendarCheck,
    title: 'Matched to your free time',
    text: "Jobs that don't clash with your timetable come first.",
  },
  {
    icon: Bell,
    title: 'Live application updates',
    text: 'No more waiting in the dark.',
  },
]

const footerColumns = [
  {
    title: 'Students',
    links: [
      { label: 'Find jobs', to: '/jobs' },
      { label: 'Create account', to: '/register' },
    ],
  },
  {
    title: 'Employers',
    links: [
      { label: 'Post a job', to: '/postings' },
      { label: 'Log in', to: '/login' },
    ],
  },
]

// 首页（设计 01，US 1.1 的入口）。已登录的用户打开 / 时，直接送到各自的首页。
export function LandingPage() {
  const token = useAppSelector((state) => state.auth.token)
  const user = useAppSelector((state) => state.auth.user)

  // 刷新页面后有令牌但还没有 user，要向后端取一次，做法和 RequireRole 一样。
  const { isError } = useGetMeQuery(undefined, { skip: !token || !!user })

  if (user) {
    return (
      <Navigate to={user.role === 'STUDENT' ? '/jobs' : '/postings'} replace />
    )
  }
  if (token && !isError) {
    return (
      <p role="status" className="p-4 text-text-muted">
        Loading…
      </p>
    )
  }

  return (
    <div className="bg-surface">
      <Hero />
      <section className="mx-auto max-w-300 px-4 py-16 md:px-8 md:py-20">
        <h2 className="mb-10 text-center text-h2 md:text-h1">
          What WorkMates does about it
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {features.map(({ icon: Icon, title, text }) => (
            <Card key={title} className="flex flex-col gap-2.5 p-6">
              <span className="inline-flex size-10 items-center justify-center rounded-md bg-surface-muted text-text-strong">
                <Icon size={20} aria-hidden />
              </span>
              <h3 className="text-title">{title}</h3>
              <p className="text-body-sm text-text-muted">{text}</p>
            </Card>
          ))}
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Audience icon={GraduationCap} title="For students">
            Sign up with your @student.tus.ie email, add your free time once,
            and see jobs ranked by how well they fit.
          </Audience>
          <Audience icon={Store} title="For employers">
            Post a job with pay, hours and shifts in a few minutes. Move
            applicants through a clear status flow.
          </Audience>
        </div>
      </section>
      <Footer />
    </div>
  )
}

// 深靛蓝横幅：整个网站唯一的深色区域。
function Hero() {
  return (
    <section className="bg-night px-4 pt-16 pb-14 text-white md:px-8 md:pt-24 md:pb-22">
      <div className="mx-auto flex max-w-300 flex-col items-center gap-5 text-center">
        <span className="rounded-full bg-white/12 px-2.5 py-1 text-eyebrow">
          For TUS students
        </span>
        <h1 className="max-w-205 text-h1 text-balance text-white md:text-display">
          <DominoText text={HERO_TITLE} />
        </h1>
        <p className="max-w-150 text-title font-normal text-pretty text-white/78">
          WorkMates is a part-time and full-time job community for students at
          TUS.
        </p>
        <div className="mt-2 flex flex-wrap justify-center gap-3">
          <ButtonLink to="/register" size="lg">
            Create an account
          </ButtonLink>
          <ButtonLink to="/login" size="lg" variant="secondary">
            Log in
          </ButtonLink>
        </div>
      </div>
    </section>
  )
}

// 标题逐字"翻倒再站起来"的动画。每个字用同一个动画，只是延迟递增（0.045 秒一个字），
// 看起来像多米诺骨牌依次倒下。
// - 一个词里的字放进同一个 inline-block + nowrap，这样换行时不会把一个词从中间断开。
// - 读屏软件读 sr-only 里的整句话；拆开的单个字母用 aria-hidden 藏起来，免得逐字母朗读。
// - motion-reduce:animate-none：系统设置了"减少动态效果"的用户看不到动画。
function DominoText({ text }: { text: string }) {
  const words = text.split(' ')
  let index = 0
  return (
    <>
      <span className="sr-only">{text}</span>
      <span aria-hidden>
        {words.map((word, wordIndex) => (
          <Fragment key={wordIndex}>
            <span className="inline-block whitespace-nowrap">
              {[...word].map((char, charIndex) => (
                <span
                  key={charIndex}
                  className="inline-block origin-bottom-right animate-domino motion-reduce:animate-none"
                  style={{ animationDelay: `${index++ * 0.045}s` }}
                >
                  {char}
                </span>
              ))}
            </span>
            {wordIndex < words.length - 1 && ' '}
          </Fragment>
        ))}
      </span>
    </>
  )
}

function Audience({
  icon: Icon,
  title,
  children,
}: {
  icon: LucideIcon
  title: string
  children: string
}) {
  return (
    <div className="flex flex-col gap-2 rounded-lg bg-surface-muted p-7">
      <h3 className="flex items-center gap-2 text-h3">
        <Icon size={18} aria-hidden />
        {title}
      </h3>
      <p className="text-text">{children}</p>
    </div>
  )
}

function Footer() {
  return (
    <footer className="border-t border-border bg-surface-muted px-4 pt-12 pb-6 md:px-8">
      <div className="mx-auto max-w-300">
        <div className="grid gap-8 md:grid-cols-[2fr_1fr_1fr]">
          <div className="flex flex-col gap-2">
            <span className="text-[17px] leading-tight font-bold tracking-[-0.3px] text-text-strong">
              WorkMates
            </span>
            <span className="max-w-70 text-caption text-text-muted">
              A job community for students at TUS. Athlone · Limerick · Remote.
            </span>
          </div>
          {footerColumns.map((column) => (
            <div key={column.title} className="flex flex-col gap-2">
              <span className="text-caption leading-5 font-semibold text-text-strong">
                {column.title}
              </span>
              {column.links.map((link) => (
                <Link
                  key={link.label}
                  to={link.to}
                  className="text-caption text-text hover:underline"
                >
                  {link.label}
                </Link>
              ))}
            </div>
          ))}
        </div>
        <div className="mt-10 flex flex-wrap justify-between gap-x-6 gap-y-2 border-t border-border pt-5 text-caption text-text-faint">
          <span>© {new Date().getFullYear()} WorkMates</span>
          {/* Privacy 和 Terms 还没有页面，先只显示文字 */}
          <span className="flex gap-5">
            <span>Privacy</span>
            <span>Terms</span>
          </span>
        </div>
      </div>
    </footer>
  )
}
