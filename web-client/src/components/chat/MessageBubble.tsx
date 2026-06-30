interface Props {
  role: 'user' | 'assistant'
  content: string
}

export function MessageBubble({ role, content }: Props) {
  const isUser = role === 'user'
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[75%] rounded-2xl px-4 py-3 text-sm whitespace-pre-wrap ${
          isUser
            ? 'bg-applied text-fg rounded-br-sm'
            : 'bg-raised-2 text-fg rounded-bl-sm'
        }`}
      >
        {content}
      </div>
    </div>
  )
}
