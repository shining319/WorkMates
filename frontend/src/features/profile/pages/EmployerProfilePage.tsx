import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { applyServerError } from '../../../shared/lib/fieldErrorMap'
import { Banner } from '../../../shared/ui/Banner'
import { Button } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Field } from '../../../shared/ui/Field'
import { Input } from '../../../shared/ui/Input'
import { Textarea } from '../../../shared/ui/Textarea'
import {
  useGetEmployerProfileQuery,
  useUpdateEmployerProfileMutation,
} from '../api'
import {
  BIO_MAX_LENGTH,
  employerProfileSchema,
  type EmployerProfileForm,
} from '../schemas'
import type { EmployerProfile } from '../types'

// 服务端的档案 -> 表单的初始值。没填的字段在服务端是 null，输入框需要空字符串。
function toFormValues(profile: EmployerProfile): EmployerProfileForm {
  return {
    displayName: profile.displayName,
    bio: profile.bio ?? '',
    contactEmail: profile.contactEmail,
    phone: profile.phone ?? '',
  }
}

// 占位数据：评价功能（US 6.3）还没做，这里先写死，让页面和设计稿 15 一致。
// 做评价功能时，换成从接口读取。
const mockReviewSummary = {
  count: 24,
  rows: [
    { label: 'Paid on time', value: '96% yes' },
    { label: 'Shift stability', value: '4.5 / 5' },
    { label: 'Overall', value: '4.6 / 5' },
  ],
}

function ReviewSummary() {
  return (
    <Card featured className="flex flex-col gap-3 p-5">
      <h2 className="text-eyebrow tracking-[0.4px] text-text-muted uppercase">
        Your reviews · {mockReviewSummary.count}
      </h2>
      <dl className="flex flex-col gap-3">
        {mockReviewSummary.rows.map((row) => (
          <div
            key={row.label}
            className="flex justify-between text-body-sm text-text tabular-nums"
          >
            <dt>{row.label}</dt>
            <dd className="font-bold text-text-strong">{row.value}</dd>
          </div>
        ))}
      </dl>
    </Card>
  )
}

function EmployerProfileEditor({ profile }: { profile: EmployerProfile }) {
  const [updateProfile] = useUpdateEmployerProfileMutation()
  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<EmployerProfileForm>({
    resolver: zodResolver(employerProfileSchema),
    defaultValues: toFormValues(profile),
  })

  const bioLength = useWatch({ control, name: 'bio' }).length

  async function onSubmit(values: EmployerProfileForm) {
    try {
      const saved = await updateProfile(values).unwrap()
      reset(toFormValues(saved))
      toast.success('Profile saved')
    } catch (error) {
      // 失败时不重置表单，已填的内容都保留
      applyServerError(error, setError)
    }
  }

  return (
    <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="flex flex-col gap-4"
      >
        <h1 className="text-h2">Profile</h1>
        <Card className="flex flex-col gap-4 p-6">
          <Field
            label="Display name"
            hint="Shown to students on your jobs"
            error={errors.displayName?.message}
          >
            {(props) => <Input {...props} {...register('displayName')} />}
          </Field>
          <Field
            label="Short description (optional)"
            hint={`${bioLength.toLocaleString('en-US')} / ${BIO_MAX_LENGTH.toLocaleString('en-US')}`}
            error={errors.bio?.message}
          >
            {(props) => <Textarea rows={3} {...props} {...register('bio')} />}
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Contact email" error={errors.contactEmail?.message}>
              {(props) => (
                <Input
                  type="email"
                  autoComplete="email"
                  {...props}
                  {...register('contactEmail')}
                />
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
          </div>
          <p className="text-caption text-text-muted">
            Students see your contact details only on their own applications.
          </p>
          <div className="flex justify-end border-t border-border pt-4">
            <Button type="submit" disabled={isSubmitting}>
              Save profile
            </Button>
          </div>
        </Card>
      </form>

      <div className="lg:pt-13">
        <ReviewSummary />
      </div>
    </div>
  )
}

// 雇主档案页（设计 15，US 1.5）：先加载档案，加载好了再显示表单。
export function EmployerProfilePage() {
  const { data, isLoading, refetch } = useGetEmployerProfileQuery()

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
  return <EmployerProfileEditor profile={data} />
}
