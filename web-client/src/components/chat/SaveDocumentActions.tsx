import { useState } from 'react'
import type { GeneratedDocumentType } from '~/api/schemas'
import { useCreateDocumentMutation } from '~/services/documents/documentsApi'

interface Props {
  /** The assistant message body — saved verbatim as the document content. */
  content: string
  /** The application this chat is bound to. Without one there is nowhere to save. */
  applicationId: string
}

/**
 * Explicit save for a generated document.
 *
 * The assistant used to persist letters itself, so a draft was stored before anyone had read
 * it and every revision left another row. Now it just writes the document into the chat, and
 * the user decides what is worth keeping — which also means no LLM sits in the save path.
 */
export function SaveDocumentActions({ content, applicationId }: Props) {
  const [createDocument, { isLoading }] = useCreateDocumentMutation()
  const [savedAs, setSavedAs] = useState<GeneratedDocumentType | null>(null)
  const [error, setError] = useState(false)

  async function save(type: GeneratedDocumentType) {
    setError(false)
    try {
      await createDocument({ application_id: applicationId, type, content }).unwrap()
      setSavedAs(type)
    } catch {
      setError(true)
    }
  }

  if (savedAs) {
    return (
      <p className="mt-2 text-xs text-offer">
        Saved as {savedAs === 'cover_letter' ? 'cover letter' : 'resume'} — it is on the
        application now.
      </p>
    )
  }

  return (
    <div className="mt-3 flex items-center gap-2 flex-wrap">
      {(['cover_letter', 'resume'] as const).map((type) => (
        <button
          key={type}
          onClick={() => save(type)}
          disabled={isLoading}
          className="text-xs border border-line hover:border-applied hover:text-applied text-dim px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50"
        >
          {isLoading ? 'Saving…' : `Save as ${type === 'cover_letter' ? 'cover letter' : 'resume'}`}
        </button>
      ))}
      {error && <span className="text-xs text-closed">Could not save — try again.</span>}
    </div>
  )
}
