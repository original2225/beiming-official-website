type ClassValue = string | false | undefined | null | { [key: string]: boolean | undefined | null }

export function cn(...inputs: ClassValue[]): string {
  return inputs
    .flatMap((input) => {
      if (!input) return []
      if (typeof input === 'string') return input
      return Object.entries(input)
        .filter(([, value]) => Boolean(value))
        .map(([key]) => key)
    })
    .join(' ')
}
