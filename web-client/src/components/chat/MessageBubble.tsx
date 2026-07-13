import { Markdown } from './Markdown'

interface Props {
  role: 'user' | 'assistant'
  content: string
}

export function MessageBubble({ role, content }: Props) {
  const isUser = role === 'user'
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
      </div>
    </div>
  )
}
