export type Role = 'STUDENT' | 'EMPLOYER'

/** The user returned by `/api/auth/me` and by login and register. */
export interface User {
  id: number
  role: Role
  name: string
  email: string
}
