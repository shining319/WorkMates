import { zodResolver } from '@hookform/resolvers/zod'
import { GraduationCap, Store } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { useAppDispatch } from '../../../app/store'
import { applyServerError } from '../../../shared/lib/fieldErrorMap'
import { Button } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Field } from '../../../shared/ui/Field'
import { Input } from '../../../shared/ui/Input'
import { RadioGroup } from '../../../shared/ui/RadioGroup'
import { useRegisterMutation } from '../api'
import { setCredentials } from '../authSlice'
import { registerSchema, type RegisterForm } from '../schemas'

const roleOptions = [
  { value: 'STUDENT', label: 'Student', icon: <GraduationCap size={16} /> },
  { value: 'EMPLOYER', label: 'Employer', icon: <Store size={16} /> },
]

// 注册页（设计 02，US 1.1 / 1.2）。
// 表单用 react-hook-form 管理，校验规则在 schemas.ts（Zod），通过 zodResolver 接上。
export function RegisterPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [registerUser] = useRegisterMutation()

  const {
    register,
    handleSubmit,
    setError,
    resetField,
    trigger,
    formState: { errors, isSubmitting, isSubmitted },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      role: 'STUDENT',
      name: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  // 邮箱对不对取决于角色。提交过一次后，切换角色要立刻重新检查邮箱，
  // 否则旧的错误提示会一直留着，或者该报的错没报。
  const roleField = register('role')

  // handleSubmit 先跑 Zod 校验，全部通过才会调用下面的函数。
  async function onSubmit(values: RegisterForm) {
    try {
      // unwrap() 让失败时抛出错误（否则 RTK Query 会把错误当作普通返回值）
      const result = await registerUser(values).unwrap()
      dispatch(setCredentials(result))
      toast.success(`Welcome to WorkMates, ${result.user.name}!`)
      navigate(
        result.user.role === 'STUDENT' ? '/profile' : '/employer/profile',
        { replace: true },
      )
    } catch (error) {
      // 邮箱类错误显示在邮箱输入框下面，其他错误弹 toast。
      applyServerError(error, setError)
      // 出错后清空两个密码框，其他字段保留（PRD 要求）。
      resetField('password')
      resetField('confirmPassword')
    }
  }

  return (
    <div className="flex justify-center px-4 py-14">
      <Card elevated className="w-full max-w-105 p-8">
        {/* noValidate：关掉浏览器自带的校验气泡，统一用我们的错误提示 */}
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4.5"
        >
          <div>
            <h1 className="text-h2">Create your account</h1>
            <p className="mt-1.5 text-body-sm text-text-muted">
              Students sign up with their campus email.
            </p>
          </div>

          <RadioGroup
            legend="I am a"
            options={roleOptions}
            {...roleField}
            onChange={(event) => {
              void roleField.onChange(event)
              if (isSubmitted) void trigger('email')
            }}
          />

          <Field label="Name" error={errors.name?.message}>
            {(props) => (
              <Input autoComplete="name" {...props} {...register('name')} />
            )}
          </Field>

          <Field label="Email" error={errors.email?.message}>
            {(props) => (
              <Input
                type="email"
                autoComplete="email"
                {...props}
                {...register('email')}
              />
            )}
          </Field>

          <Field
            label="Password"
            hint="At least 8 characters with a letter and a number"
            error={errors.password?.message}
          >
            {(props) => (
              <Input
                type="password"
                autoComplete="new-password"
                {...props}
                {...register('password')}
              />
            )}
          </Field>

          <Field
            label="Confirm password"
            error={errors.confirmPassword?.message}
          >
            {(props) => (
              <Input
                type="password"
                autoComplete="new-password"
                {...props}
                {...register('confirmPassword')}
              />
            )}
          </Field>

          <Button
            type="submit"
            size="lg"
            disabled={isSubmitting}
            className="w-full"
          >
            Create account
          </Button>

          <p className="text-center text-caption text-text-muted">
            Already have an account?{' '}
            <Link to="/login" className="text-primary hover:underline">
              Log in
            </Link>
          </p>
        </form>
      </Card>
    </div>
  )
}
