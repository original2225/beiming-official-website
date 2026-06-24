/** 时间显示 — 统一格式化 ISO 时间为本地可读格式 */
export function TimeDisplay({ iso }: { iso: string }) {
  const d = new Date(iso)
  const fmt = (n: number) => String(n).padStart(2, '0')
  return (
    <time dateTime={iso} className="text-text-muted text-xs">
      {d.getFullYear()}-{fmt(d.getMonth() + 1)}-{fmt(d.getDate())} {fmt(d.getHours())}:{fmt(d.getMinutes())}
    </time>
  )
}
