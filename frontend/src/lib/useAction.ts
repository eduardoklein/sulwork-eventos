import { useState } from 'react'
import { ApiError } from '../api/client'
import { useNotify } from '../notifications/context'

export type FieldErrors<Field extends string> = Partial<Record<Field, string>>

/**
 * A call to the API that ends in a notice, for a window or a form. While it runs the screen is `busy`. When it
 * fails, `message` has what the person reads and `fieldErrors` has the message of each field the API refused.
 * A check of the form that fails before the call uses `reject` to show its errors the same way, with no
 * message when the marks on the fields are enough.
 */
export function useAction<Field extends string = string>() {
  const notify = useNotify()
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors<Field>>({})

  const reject = (text: string | null, errors: FieldErrors<Field> = {}) => {
    setMessage(text)
    setFieldErrors(errors)
  }

  const reset = () => {
    setMessage(null)
    setFieldErrors({})
  }

  /** `failed` is the message for a failure that has no text of its own, and `onDone` gets the answer after the success notice. */
  const run = async <Result>(call: () => Promise<Result>, { done, failed, onDone }: { done: string; failed: string; onDone: (result: Result) => void }) => {
    reset()
    setBusy(true)
    let result: Result
    try {
      result = await call()
    } catch (error) {
      if (error instanceof ApiError) {
        reject(error.message, error.fieldErrors as FieldErrors<Field>)
      } else {
        reject(failed)
      }
      setBusy(false)
      return
    }
    notify(done, 'success')
    onDone(result)
  }

  return { busy, message, fieldErrors, reject, reset, run }
}
