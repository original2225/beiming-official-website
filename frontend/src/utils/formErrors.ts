import type { FieldError } from '../types/api'

export function getFieldError(errors: FieldError[] | undefined, field: string): string | undefined {
  return errors?.find((e) => e.field === field)?.reason
}
