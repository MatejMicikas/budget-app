/** Empty input → null (no limit). Otherwise a number; server requires @Positive when set. */
export function parseOptionalAllocatedAmount(raw) {
  if (raw === null || raw === undefined) return null
  const s = String(raw).trim()
  if (s === '') return null
  const n = Number(s)
  if (Number.isNaN(n)) return null
  return n
}
