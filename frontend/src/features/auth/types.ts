// 两种角色：学生和雇主。写成"字符串联合类型"，写错拼写时 TypeScript 会报错。
export type Role = 'STUDENT' | 'EMPLOYER'

/** `/api/auth/me`、登录和注册接口返回的用户。 */
export interface User {
  id: number
  role: Role
  name: string
  email: string
}

/** `POST /api/auth/register` 的请求体。 */
export interface RegisterRequest {
  role: Role
  name: string
  email: string
  password: string
  confirmPassword: string
}

/** `POST /api/auth/login` 的请求体。 */
export interface LoginRequest {
  email: string
  password: string
}

/** 注册和登录成功时返回的内容：令牌加用户。 */
export interface AuthResult {
  token: string
  user: User
}
