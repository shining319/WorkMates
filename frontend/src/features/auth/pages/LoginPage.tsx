import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { useAppDispatch } from '../../../app/store'
import { applyServerError } from '../../../shared/lib/fieldErrorMap'
import { Banner } from '../../../shared/ui/Banner'
import { Button } from '../../../shared/ui/Button'
import { Card } from '../../../shared/ui/Card'
import { Field } from '../../../shared/ui/Field'
import { Input } from '../../../shared/ui/Input'
import { useLoginMutation } from '../api'
import { setCredentials } from '../authSlice'
import { loginSchema, type LoginForm } from '../schemas'

// 登录页（设计 03，US 1.3）。做法和注册页一样：react-hook-form + Zod。
export function LoginPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [login] = useLoginMutation()

  const {
    register,
    handleSubmit,
    setError,
    resetField,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  async function onSubmit(values: LoginForm) {
    try {
      const result = await login(values).unwrap()
      dispatch(setCredentials(result))
      // 学生进职位列表，雇主进自己的职位列表。
      navigate(result.user.role === 'STUDENT' ? '/jobs' : '/postings', {
        replace: true,
      })
    } catch (error) {
      // BAD_CREDENTIALS 不属于某个字段，fieldErrorMap 把它写到 root.server，
      // 下面显示在表单顶部。邮箱不存在和密码错误是同一个错误码，所以提示一样。
      applyServerError(error, setError)
      resetField('password')
    }
  }

  return (
    <div className="flex justify-center px-4 py-14">
      <Card elevated className="w-full max-w-105 p-8">
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-4.5"
        >
          <div>
            <h1 className="text-h2">Log in</h1>
            <p className="mt-1.5 text-body-sm text-text-muted">Welcome back.</p>
          </div>

          {errors.root?.server && (
            <Banner tone="danger">{errors.root.server.message}</Banner>
          )}

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
            hint="Lost your password? Contact the WorkMates team."
            error={errors.password?.message}
          >
            {(props) => (
              <Input
                type="password"
                autoComplete="current-password"
                {...props}
                {...register('password')}
              />
            )}
          </Field>

          <Button
            type="submit"
            size="lg"
            disabled={isSubmitting}
            className="w-full"
          >
            Log in
          </Button>
        </form>
      </Card>
    </div>
  )
}
