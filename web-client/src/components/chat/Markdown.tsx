import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'

// Mapped to the app's tokens rather than @tailwindcss/typography: prose needs its own
// dark-theme config under Tailwind v4, and the bubble only needs a handful of elements.
// Raw HTML is not enabled (no rehype-raw), so model output cannot inject markup.
const components: Components = {
  p: ({ children }) => <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>,

  h1: ({ children }) => <h1 className="mt-3 mb-2 first:mt-0 text-base font-semibold">{children}</h1>,
  h2: ({ children }) => <h2 className="mt-3 mb-2 first:mt-0 text-sm font-semibold">{children}</h2>,
  h3: ({ children }) => (
    <h3 className="mt-3 mb-1 first:mt-0 text-sm font-medium text-dim">{children}</h3>
  ),

  ul: ({ children }) => <ul className="mb-2 last:mb-0 pl-5 list-disc space-y-1">{children}</ul>,
  ol: ({ children }) => <ol className="mb-2 last:mb-0 pl-5 list-decimal space-y-1">{children}</ol>,
  li: ({ children }) => <li className="leading-relaxed">{children}</li>,

  strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
  em: ({ children }) => <em className="italic">{children}</em>,

  a: ({ children, href }) => (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="text-applied underline underline-offset-2 hover:opacity-80"
    >
      {children}
    </a>
  ),

  blockquote: ({ children }) => (
    <blockquote className="my-2 border-l-2 border-line pl-3 text-dim italic">{children}</blockquote>
  ),

  hr: () => <hr className="my-3 border-line" />,

  // Fenced blocks arrive as <pre><code>. Style the code element for inline spans and let
  // `pre` own the block frame + horizontal scroll, so long lines never widen the bubble.
  code: ({ className, children }) => {
    const isBlock = typeof className === 'string' && className.startsWith('language-')
    if (isBlock) {
      return <code className="font-mono text-xs leading-relaxed">{children}</code>
    }
    return (
      <code className="font-mono text-[0.85em] bg-raised border border-line rounded px-1 py-0.5">
        {children}
      </code>
    )
  },
  pre: ({ children }) => (
    <pre className="my-2 overflow-x-auto rounded-lg bg-raised border border-line p-3">
      {children}
    </pre>
  ),

  // Tables come from remark-gfm; scroll them instead of letting them stretch the bubble.
  table: ({ children }) => (
    <div className="my-2 overflow-x-auto">
      <table className="w-full text-xs border-collapse">{children}</table>
    </div>
  ),
  th: ({ children }) => (
    <th className="border border-line px-2 py-1 text-left font-medium text-dim">{children}</th>
  ),
  td: ({ children }) => <td className="border border-line px-2 py-1">{children}</td>,
}

export function Markdown({ content }: { content: string }) {
  return (
    <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
      {content}
    </ReactMarkdown>
  )
}
