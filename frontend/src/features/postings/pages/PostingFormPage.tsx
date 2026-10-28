import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft } from 'lucide-react'
import {
  Controller,
  FormProvider,
  useForm,
  useFormContext,
  useWatch,
} from 'react-hook-form'
import type { ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { useAppSelector } from '../../../app/store'
import { isApiError } from '../../../api/types'
import { AREA_OPTIONS, JOB_TYPE_OPTIONS } from '../../../shared/lib/constants'
import { applyServerError } from '../../../shared/lib/fieldErrorMap'
import { formatDate } from '../../../shared/lib/format'
import { Banner } from '../../../shared/ui/Banner'
import { Button, ButtonLink } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Field } from '../../../shared/ui/Field'
import { Input } from '../../../shared/ui/Input'
import { ItemUnavailable } from '../../../shared/ui/ItemUnavailable'
import { RadioGroup } from '../../../shared/ui/RadioGroup'
import { Select } from '../../../shared/ui/Select'
import { TagInput } from '../../../shared/ui/TagInput'
import { Textarea } from '../../../shared/ui/Textarea'
import { JobCard } from '../../jobs/components/JobCard'
import { TimeSlotEditor } from '../../profile/components/TimeSlotEditor'
import { useGetEmployerProfileQuery } from '../../profile/api'
import { useTagSuggestions } from '../../profile/useTagSuggestions'
import {
  useCreatePostingMutation,
  useGetPostingQuery,
  useUpdatePostingMutation,
} from '../api'
import {
  DESCRIPTION_MAX_LENGTH,
  postingSchema,
  type PostingForm,
} from '../schemas'
import type { Posting, PostingRequest } from '../types'

const emptyForm: PostingForm = {
  title: '',
  jobType: 'PART_TIME',
  hourlyRate: '',
  weeklyHours: '',
  area: 'ATHLONE',
  address: '',
  description: '',
  headcount: '1',
  tags: [],
  timeSlots: [],
}

// 服务端的职位 -> 表单的初始值（编辑时）。数字在输入框里都是字符串。
function toFormValues(posting: Posting): PostingForm {
  return {
    title: posting.title,
    jobType: posting.jobType,
    hourlyRate: posting.hourlyRate.toFixed(2),
    weeklyHours: String(posting.weeklyHours),
    area: posting.area,
    address: posting.address,
    description: posting.description ?? '',
    headcount: String(posting.headcount),
    tags: posting.tags.map((tag) => tag.name),
    timeSlots: posting.timeSlots,
  }
}

// 表单 -> 请求体：把字符串转成数字。
function toRequest(values: PostingForm): PostingRequest {
  return {
    ...values,
    hourlyRate: Number(values.hourlyRate),
    weeklyHours: Number(values.weeklyHours),
    headcount: Number(values.headcount),
  }
}

// 预览用：输入框里的字符串还不是合法数字时返回 null。
function toNumberOrNull(text: string): number | null {
  const value = Number(text)
  return text.trim() === '' || Number.isNaN(value) ? null : value
}

// 一个带单位的输入框（€/h、h/week）
function WithSuffix({
  suffix,
  children,
}: {
  suffix: string
  children: ReactNode
}) {
  return (
    <div className="relative">
      {children}
      <span className="pointer-events-none absolute top-1/2 right-2.5 -translate-y-1/2 text-caption text-text-muted">
        {suffix}
      </span>
    </div>
  )
}

// 右侧的 "How students see it"：用 JobCard 按表单当前内容实时预览。
function PostingPreview({
  postedAt,
  employerName,
}: {
  postedAt: string
  employerName: string
}) {
  const { control } = useFormContext<PostingForm>()
  // 不传 name 就是监听整个表单
  const values = useWatch({ control })
  return (
    <div className="flex flex-col gap-2.5 lg:sticky lg:top-6">
      <h2 className="text-eyebrow tracking-[0.4px] text-text-muted uppercase">
        How students see it
      </h2>
      <JobCard
        title={values.title?.trim() || 'Job title'}
        employerName={employerName}
        jobType={values.jobType ?? 'PART_TIME'}
        rate={toNumberOrNull(values.hourlyRate ?? '')}
        weeklyHours={toNumberOrNull(values.weeklyHours ?? '')}
        area={values.area ?? 'ATHLONE'}
        postedAt={postedAt}
      />
      <p className="text-caption text-text-muted">
        Students also see a match score based on their own profile.
      </p>
    </div>
  )
}

// 表单本身。新建时不传 posting；编辑时传服务端的职位，用它预填。
function PostingEditor({ posting }: { posting?: Posting }) {
  const navigate = useNavigate()
  const [createPosting] = useCreatePostingMutation()
  const [updatePosting] = useUpdatePostingMutation()
  const { data: employer } = useGetEmployerProfileQuery()
  const [suggestions, setTagQuery] = useTagSuggestions()

  const form = useForm<PostingForm>({
    resolver: zodResolver(postingSchema),
    defaultValues: posting ? toFormValues(posting) : emptyForm,
  })
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = form

  const descriptionLength = useWatch({ control, name: 'description' }).length
  const backTo = posting ? `/postings/${posting.id}` : '/postings'

  async function onSubmit(values: PostingForm) {
    const body = toRequest(values)
    try {
      if (posting) {
        await updatePosting({ id: posting.id, body }).unwrap()
        toast.success('Posting saved')
        navigate(`/postings/${posting.id}`)
      } else {
        await createPosting(body).unwrap()
        toast.success('Posting published')
        navigate('/postings')
      }
    } catch (error) {
      // 编辑期间职位被关闭了：整个页面已经没用，提示后回到列表
      if (isApiError(error) && error.errorCode === 'POSTING_NOT_EDITABLE') {
        toast.error(error.errorMsg)
        navigate('/postings')
        return
      }
      // 失败时不重置表单，已填的内容都保留
      applyServerError(error, setError)
    }
  }

  return (
    <div>
      <Link
        to="/postings"
        className="mb-4 inline-flex items-center gap-1 text-caption text-text-muted hover:text-text"
      >
        <ArrowLeft size={14} aria-hidden />
        My postings
      </Link>
      <h1 className="mb-5 text-h2">
        {posting ? 'Edit posting' : 'New posting'}
      </h1>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        {/* FormProvider 让 TimeSlotEditor 不用传 props 就能拿到这个表单 */}
        <FormProvider {...form}>
          <form
            onSubmit={handleSubmit(onSubmit)}
            noValidate
            className="flex flex-col gap-4"
          >
            <Card className="flex flex-col gap-4 p-6">
              <h2 className="text-title">The job</h2>
              <Field label="Title" error={errors.title?.message}>
                {(props) => <Input {...props} {...register('title')} />}
              </Field>
              <RadioGroup
                legend="Job type"
                options={JOB_TYPE_OPTIONS}
                {...register('jobType')}
              />
              <div className="grid grid-cols-1 items-start gap-4 sm:grid-cols-3">
                <Field label="Hourly rate" error={errors.hourlyRate?.message}>
                  {(props) => (
                    <WithSuffix suffix="€/h">
                      <Input
                        inputMode="decimal"
                        className="pr-14"
                        {...props}
                        {...register('hourlyRate')}
                      />
                    </WithSuffix>
                  )}
                </Field>
                <Field label="Weekly hours" error={errors.weeklyHours?.message}>
                  {(props) => (
                    <WithSuffix suffix="h/week">
                      <Input
                        inputMode="decimal"
                        className="pr-16"
                        {...props}
                        {...register('weeklyHours')}
                      />
                    </WithSuffix>
                  )}
                </Field>
                <Field
                  label="Headcount"
                  hint="Places to fill"
                  error={errors.headcount?.message}
                >
                  {(props) => (
                    <Input
                      inputMode="numeric"
                      {...props}
                      {...register('headcount')}
                    />
                  )}
                </Field>
              </div>
            </Card>

            <Card className="p-6">
              <TimeSlotEditor
                title="Weekly time slots"
                emptyText="No time slots yet. Add the shifts students will work each week."
              />
            </Card>

            <Card className="flex flex-col gap-4 p-6">
              <h2 className="text-title">Place and skills</h2>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-[180px_minmax(0,1fr)]">
                <Field label="Location area" error={errors.area?.message}>
                  {(props) => (
                    <Select
                      options={AREA_OPTIONS}
                      {...props}
                      {...register('area')}
                    />
                  )}
                </Field>
                <Field label="Address line" error={errors.address?.message}>
                  {(props) => (
                    <Input
                      autoComplete="street-address"
                      {...props}
                      {...register('address')}
                    />
                  )}
                </Field>
              </div>
              <Controller
                control={control}
                name="tags"
                render={({ field }) => (
                  <Field
                    label="Required skills and languages"
                    hint="Press Enter to add a tag"
                    error={errors.tags?.message ?? errors.tags?.root?.message}
                  >
                    {(props) => (
                      <TagInput
                        {...props}
                        value={field.value}
                        onChange={field.onChange}
                        suggestions={suggestions}
                        onQueryChange={setTagQuery}
                      />
                    )}
                  </Field>
                )}
              />
              <Field
                label="Description (optional)"
                hint={`${descriptionLength.toLocaleString('en-IE')} / ${DESCRIPTION_MAX_LENGTH.toLocaleString('en-IE')}`}
                error={errors.description?.message}
              >
                {(props) => (
                  <Textarea rows={4} {...props} {...register('description')} />
                )}
              </Field>
            </Card>

            <div className="flex justify-end gap-2">
              <ButtonLink to={backTo} variant="ghost">
                Cancel
              </ButtonLink>
              <Button type="submit" disabled={isSubmitting}>
                {posting ? 'Save changes' : 'Publish posting'}
              </Button>
            </div>
          </form>

          <PostingPreview
            employerName={employer?.displayName ?? ''}
            postedAt={posting ? formatDate(posting.createdAt) : 'just now'}
          />
        </FormProvider>
      </div>
    </div>
  )
}

// 编辑页（/postings/:id/edit）：先加载职位，再按情况显示表单、"已关闭"或"不存在"。
function EditPosting({ id }: { id: number }) {
  const userId = useAppSelector((state) => state.auth.user?.id)
  const { data, isLoading, error, refetch } = useGetPostingQuery(id)

  if (isLoading) {
    return <p className="text-body-sm text-text-muted">Loading the posting…</p>
  }
  if (!data) {
    if (isApiError(error) && error.status === 404) return <NotAvailable />
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner tone="danger">We could not load this posting.</Banner>
        <Button variant="utility" onClick={() => void refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  // 别人的职位：和后端一致，当作不存在（保存时后端也会返回 404）
  if (data.employerId !== userId) return <NotAvailable />
  if (data.status === 'CLOSED') {
    return (
      <div className="flex max-w-md flex-col items-start gap-3">
        <Banner>This posting is closed and can no longer be changed.</Banner>
        <ButtonLink to={`/postings/${data.id}`} variant="utility">
          Back to the posting
        </ButtonLink>
      </div>
    )
  }
  return <PostingEditor posting={data} />
}

function NotAvailable() {
  return (
    <ItemUnavailable>
      <ButtonLink to="/postings" variant="utility">
        Back to my postings
      </ButtonLink>
    </ItemUnavailable>
  )
}

// 新建和编辑共用的页面：路由里有 :id 就是编辑，没有就是新建（设计 12，US 2.1 / 2.2）。
export function PostingFormPage() {
  const { id } = useParams()
  if (id === undefined) return <PostingEditor />
  const postingId = Number(id)
  if (!Number.isInteger(postingId)) return <NotAvailable />
  return <EditPosting id={postingId} />
}
