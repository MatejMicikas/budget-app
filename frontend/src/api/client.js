export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export function toQuery(params) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      q.append(key, value)
    }
  })
  const query = q.toString()
  return query ? `?${query}` : ''
}

/**
 * @param {string} path
 * @param {{ method?: string, token?: string, body?: object, isCsv?: boolean }} [options]
 */
export async function api(path, { method = 'GET', token, body, isCsv = false } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: {
      ...(isCsv ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const payload = await response.json()
      if (payload?.error) message = payload.error
      if (payload?.message) message = payload.message
    } catch {
      // ignore non-JSON error body
    }
    throw new Error(message)
  }

  if (isCsv) {
    return response.blob()
  }

  if (response.status === 204) return null
  return response.json()
}
