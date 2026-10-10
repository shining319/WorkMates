const TOKEN_KEY = 'token'

// localStorage can throw (private window, blocked site data), so every call is guarded.

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
    // Storage is unavailable; the user stays logged in until the page is closed.
  }
}
