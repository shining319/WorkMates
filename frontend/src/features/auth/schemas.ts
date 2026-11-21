import { z } from 'zod'

const STUDENT_DOMAIN = '@student.tus.ie'

// 密码规则和后端一致：8–72 位，至少一个字母和一个数字。
// 后端按 UTF-8 字节数限制 72（BCrypt 只认前 72 字节），所以中文密码要按字节数再查一次。
const PASSWORD_RULE = 'Use at least 8 characters with a letter and a number.'

export const registerSchema = z
  .object({
    role: z.enum(['STUDENT', 'EMPLOYER']),
    name: z
      .string()
      .trim()
      .min(1, 'Enter your name.')
      .max(100, 'Use at most 100 characters.'),
    // trim() 先去掉首尾空格再校验，和后端一致。
    email: z
      .string()
      .trim()
      .pipe(
        z
          .email('Enter a valid email address.')
          .max(254, 'Use at most 254 characters.'),
      ),
    password: z
      .string()
      .min(8, PASSWORD_RULE)
      .regex(/[A-Za-z]/, PASSWORD_RULE)
      .regex(/[0-9]/, PASSWORD_RULE)
      .refine(
        (value) => new TextEncoder().encode(value).length <= 72,
        'Use at most 72 characters.',
      ),
    confirmPassword: z.string().min(1, 'Confirm your password.'),
  })
  // 下面的检查要同时看多个字段，所以放在整个对象上。path 指定错误显示在哪个字段旁边。
  .superRefine((form, ctx) => {
    const isStudentEmail = form.email.toLowerCase().endsWith(STUDENT_DOMAIN)
    if (form.role === 'STUDENT' && !isStudentEmail) {
      ctx.addIssue({
        code: 'custom',
        path: ['email'],
        message: 'Student accounts need a @student.tus.ie email.',
      })
    }
    if (form.role === 'EMPLOYER' && isStudentEmail) {
      ctx.addIssue({
        code: 'custom',
        path: ['email'],
        message: 'Student emails can only be used for student accounts.',
      })
    }
    if (form.password !== form.confirmPassword) {
      ctx.addIssue({
        code: 'custom',
        path: ['confirmPassword'],
        message: 'Passwords do not match.',
      })
    }
  })

export type RegisterForm = z.infer<typeof registerSchema>

// 登录只检查"不为空"。邮箱格式、密码规则不查：后端登录也不查，
// 格式不对一律返回 BAD_CREDENTIALS，不暴露"哪一项有问题"。
export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email.'),
  password: z.string().min(1, 'Enter your password.'),
})

export type LoginForm = z.infer<typeof loginSchema>
