import { zodResolver } from '@hookform/resolvers/zod'
import { CircleCheck, Circle } from 'lucide-react'
import { useState } from 'react'
import { Controller, FormProvider, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { useAppSelector } from '../../../app/store'
import { applyServerError } from '../../../shared/lib/fieldErrorMap'
import { Banner } from '../../../shared/ui/Banner'
import { Button } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Field } from '../../../shared/ui/Field'
import { Input } from '../../../shared/ui/Input'
import { Switch } from '../../../shared/ui/Switch'
import { TagInput } from '../../../shared/ui/TagInput'
import {
  useGetStudentProfileQuery,
  useUpdateStudentProfileMutation,
} from '../api'
import { TimeSlotEditor } from '../components/TimeSlotEditor'
import {
  MAX_TAGS,
  studentProfileSchema,
  type StudentProfileForm,
} from '../schemas'
import type { StudentProfile, StudentProfileRequest } from '../types'
import { useTagSuggestions } from '../useTagSuggestions'

// 服务端的档案 -> 表单的初始值。没填的字段在服务端是 null，输入框需要空字符串。
function toFormValues(profile: StudentProfile): StudentProfileForm {
  return {
    name: profile.name,
    phone: profile.phone ?? '',
    major: profile.major ?? '',
    yearOfStudy: profile.yearOfStudy ?? '',
    expectedHourlyRate:
      profile.expectedHourlyRate === null
        ? ''
        : profile.expectedHourlyRate.toFixed(2),
    tags: profile.tags.map((tag) => tag.name),
    timeSlots: profile.timeSlots,
  }
}

// 表单 -> 请求体。时薪是字符串，空表示没填（发 null）。
function toRequest(values: StudentProfileForm): StudentProfileRequest {
  return {
    ...values,
    expectedHourlyRate:
      values.expectedHourlyRate === ''
        ? null
        : Number(values.expectedHourlyRate),
  }
}

// 右侧状态卡里的三项。key 对应服务端 missing 里的名字。
const checklist = [
  { key: 'tags', label: 'Skill tags' },
  { key: 'expectedHourlyRate', label: 'Expected hourly rate' },
  { key: 'timeSlots', label: 'Free time slots' },
]

function ProfileStatus({ profile }: { profile: StudentProfile }) {
  return (
    <Card featured className="flex flex-col gap-3 p-5 lg:sticky lg:top-6">
      <span
        className={`w-fit rounded-xs px-2 py-0.5 text-caption font-semibold ${profile.complete ? 'bg-success/15 text-success' : 'bg-black/6 text-text-muted'}`}
      >
        {profile.complete ? 'Profile complete' : 'Profile incomplete'}
      </span>
      <p className="text-caption text-text-muted">
        {profile.complete
          ? 'You can apply to jobs. Matching uses these three parts:'
          : 'Fill in the missing parts to apply to jobs and get better matches:'}
      </p>
      <ul className="flex flex-col gap-2">
        {checklist.map(({ key, label }) => {
          const done = !profile.missing.includes(key)
          return (
            <li
              key={key}
              className={`flex items-center gap-2 text-body-sm ${done ? 'text-text-strong' : 'text-text-muted'}`}
            >
              {done ? (
                <CircleCheck size={16} aria-hidden className="text-success" />
              ) : (
                <Circle size={16} aria-hidden />
              )}
              {label}
              {!done && <span className="sr-only">(missing)</span>}
            </li>
          )
        })}
      </ul>
    </Card>
  )
}

// 表单本身。拿到服务端的档案之后才会渲染，所以 defaultValues 一开始就是完整的。
function StudentProfileEditor({ profile }: { profile: StudentProfile }) {
  const email = useAppSelector((state) => state.auth.user?.email)
  const [updateProfile] = useUpdateStudentProfileMutation()
  const [suggestions, setTagQuery] = useTagSuggestions()
  // 设计稿 07 的 "Matching job alerts" 开关。现在只是界面，不保存、不发请求（F7.2.1 再接后端）。
  const [alertsOn, setAlertsOn] = useState(true)

  const form = useForm<StudentProfileForm>({
    resolver: zodResolver(studentProfileSchema),
    defaultValues: toFormValues(profile),
  })
  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = form

  const tagCount = useWatch({ control, name: 'tags' }).length

  async function onSubmit(values: StudentProfileForm) {
    try {
      const saved = await updateProfile(toRequest(values)).unwrap()
      // 用服务端返回的内容重置表单：标签规范化、时间段排序后的样子，以及"没改动"的状态
      reset(toFormValues(saved))
      toast.success('Profile saved')
    } catch (error) {
      // 失败时不重置表单，已填的内容都保留
      applyServerError(error, setError)
    }
  }

  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      {/* FormProvider 让 TimeSlotEditor 不用传 props 就能拿到这个表单 */}
      <FormProvider {...form}>
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4"
        >
          <h1 className="text-h2">Profile</h1>

          <Card className="flex flex-col gap-4 p-6">
            <h2 className="text-title">About you</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Name" error={errors.name?.message}>
                {(props) => (
                  <Input autoComplete="name" {...props} {...register('name')} />
                )}
              </Field>
              <Field label="Phone (optional)" error={errors.phone?.message}>
                {(props) => (
                  <Input
                    type="tel"
                    autoComplete="tel"
                    {...props}
                    {...register('phone')}
                  />
                )}
              </Field>
              <Field label="Programme (optional)" error={errors.major?.message}>
                {(props) => <Input {...props} {...register('major')} />}
              </Field>
              <Field
                label="Year of study (optional)"
                error={errors.yearOfStudy?.message}
              >
                {(props) => <Input {...props} {...register('yearOfStudy')} />}
              </Field>
            </div>
            {email && (
              <p className="text-caption text-text-muted">
                Campus email: {email}
              </p>
            )}
          </Card>

          <Card className="flex flex-col gap-4 p-6">
            <h2 className="text-title">Skills and pay</h2>
            <Controller
              control={control}
              name="tags"
              render={({ field }) => (
                <Field
                  label="Skills and languages"
                  hint={
                    tagCount >= MAX_TAGS
                      ? `Limit reached: ${tagCount} of ${MAX_TAGS} tags`
                      : `Press Enter to add a tag · ${tagCount} of ${MAX_TAGS} tags`
                  }
                  error={errors.tags?.message ?? errors.tags?.root?.message}
                >
                  {(props) => (
                    <TagInput
                      {...props}
                      value={field.value}
                      onChange={field.onChange}
                      max={MAX_TAGS}
                      suggestions={suggestions}
                      onQueryChange={setTagQuery}
                    />
                  )}
                </Field>
              )}
            />
            <div className="max-w-60">
              <Field
                label="Expected hourly rate"
                error={errors.expectedHourlyRate?.message}
              >
                {(props) => (
                  <div className="relative">
                    <Input
                      inputMode="decimal"
                      className="pr-14"
                      {...props}
                      {...register('expectedHourlyRate')}
                    />
                    <span className="pointer-events-none absolute top-1/2 right-2.5 -translate-y-1/2 text-caption text-text-muted">
                      €/h
                    </span>
                  </div>
                )}
              </Field>
            </div>
          </Card>

          <Card className="p-6">
            <TimeSlotEditor />
          </Card>

          <Card className="p-6">
            <Switch
              label="Matching job alerts"
              description="Get an alert when a new job matches you 70% or more"
              checked={alertsOn}
              onChange={setAlertsOn}
            />
          </Card>

          <div className="flex justify-end">
            <Button type="submit" disabled={isSubmitting}>
              Save profile
            </Button>
          </div>
        </form>
      </FormProvider>

      <div className="lg:pt-13">
        <ProfileStatus profile={profile} />
      </div>
    </div>
  )
}

// 学生档案页（设计 07，US 1.4）：先加载档案，加载好了再显示表单。
export function StudentProfilePage() {
  const { data, isLoading, refetch } = useGetStudentProfileQuery()

  if (isLoading) {
    return <p className="text-body-sm text-text-muted">Loading your profile…</p>
  }
  if (!data) {
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner tone="danger">We could not load your profile.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return <StudentProfileEditor profile={data} />
}
