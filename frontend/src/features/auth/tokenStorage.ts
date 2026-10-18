const TOKEN_KEY = 'token'

// localStorage 是浏览器自带的存储，刷新页面后数据还在。
// 但它有时会抛错（无痕窗口、用户屏蔽了网站数据），所以每次读写都用 try/catch 包起来。

export function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function saveToken(token: string | null) {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  } catch {
    // 存储不可用时不报错，用户在关闭页面之前仍保持登录。
  }
}
