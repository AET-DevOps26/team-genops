import { Markdown } from './Markdown'
import { SaveDocumentActions } from './SaveDocumentActions'

// A greeting or a clarifying question is not a document. Only offer to save a reply
// substantial enough to be one, so the buttons do not clutter ordinary conversation.
const MIN_DOCUMENT_CHARS = 400

interface Props {
  role: 'user' | 'assistant'
  content: string
  /** The application this chat is bound to, if any — where a save would go. */
  applicationId?: string | null
}

export function MessageBubble({ role, content, applicationId }: Props) {
  const isUser = role === 'user'
  const canSave = !isUser && !!applicationId && content.length >= MIN_DOCUMENT_CHARS

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[75%] min-w-0 rounded-2xl px-4 py-3 text-sm ${
          isUser
            ? 'bg-applied text-fg rounded-br-sm whitespace-pre-wrap'
            : 'bg-raised-2 text-fg rounded-bl-sm'
        }`}
      >
        {/* The model answers in markdown; what the user typed is shown verbatim. */}
        {isUser ? content : <Markdown content={content} />}

        {canSave && <SaveDocumentActions content={content} applicationId={applicationId} />}
      </div>
    </div>
  )
}
