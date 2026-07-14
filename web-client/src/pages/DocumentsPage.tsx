import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Markdown } from '~/components/Markdown'
import type { GeneratedDocument, GeneratedDocumentType, JobApplication } from '~/api/schemas'
import { useListApplicationsQuery } from '~/services/applications/applicationsApi'
import { useDeleteDocumentMutation, useGetDocumentsQuery } from '~/services/documents/documentsApi'

type Filter = 'all' | GeneratedDocumentType

const FILTERS: { key: Filter; label: string }[] = [
  { key: 'all', label: 'All' },
  { key: 'cover_letter', label: 'Cover letters' },
  { key: 'resume', label: 'Resumes' },
]

function typeLabel(type: GeneratedDocumentType) {
  return type === 'cover_letter' ? 'Cover letter' : 'Resume'
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/**
 * Every generated document in one place.
 *
 * Documents were only reachable from inside the application they were filed under, so a
 * standalone resume had nowhere to live and a letter you couldn't place was effectively lost.
 * Here they are listed together; the ones tailored for a job carry that job as a label and
 * still appear on the application itself.
 */
export default function DocumentsPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<Filter>('all')
  const [openId, setOpenId] = useState<string | null>(null)

  const { data, isLoading } = useGetDocumentsQuery()
  // Documents store only an application_id, so the job title/company are resolved here.
  const { data: applicationsData } = useListApplicationsQuery()
  const [deleteDocument] = useDeleteDocumentMutation()

  const applicationsById = useMemo(() => {
    const map = new Map<string, JobApplication>()
    for (const app of applicationsData?.items ?? []) map.set(app.id, app)
    return map
  }, [applicationsData])

  const documents = (data?.items ?? []).filter((d) => filter === 'all' || d.type === filter)

  if (isLoading) return <p className="p-6 text-sm text-dim">Loading your documents…</p>

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <header className="mb-6">
        <h1 className="text-xl font-semibold text-fg">Documents</h1>
        <p className="mt-1 text-sm text-dim">
          Cover letters and resumes you saved from the assistant.
        </p>
      </header>

      <div className="mb-5 flex gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
              filter === f.key
                ? 'border-applied text-applied bg-applied/10'
                : 'border-line text-dim hover:text-fg'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {documents.length === 0 ? (
        <div className="rounded-xl border border-line bg-raised px-6 py-10 text-center">
          <p className="text-sm text-dim">Nothing saved yet.</p>
          <p className="mt-1 text-xs text-faint">
            Ask the assistant for a cover letter or a resume, then save the reply.
          </p>
          <button onClick={() => navigate('/chat')} className="cta mt-4 rounded-xl px-4 py-2 text-sm">
            Open the assistant
          </button>
        </div>
      ) : (
        <ul className="space-y-3">
          {documents.map((doc) => (
            <DocumentCard
              key={doc.id}
              document={doc}
              application={doc.application_id ? applicationsById.get(doc.application_id) : undefined}
              expanded={openId === doc.id}
              onToggle={() => setOpenId(openId === doc.id ? null : doc.id)}
              onDelete={() => deleteDocument(doc.id)}
            />
          ))}
        </ul>
      )}
    </div>
  )
}

function DocumentCard({
  document,
  application,
  expanded,
  onToggle,
  onDelete,
}: {
  document: GeneratedDocument
  application?: JobApplication
  expanded: boolean
  onToggle: () => void
  onDelete: () => void
}) {
  return (
    <li className="rounded-xl border border-line bg-raised overflow-hidden">
      <div className="flex items-center gap-3 px-4 py-3">
        <button onClick={onToggle} className="min-w-0 flex-1 text-left">
          <span className="flex items-center gap-2 flex-wrap">
            <span className="text-sm text-fg">{typeLabel(document.type)}</span>

            {application ? (
              <span className="tag bg-applied/10 border border-applied/30 text-applied">
                {application.job_title} · {application.company}
              </span>
            ) : (
              // Tailored for nothing in particular — say so, rather than leaving a blank
              // where every other document shows a job.
              <span className="tag text-faint border border-line">General</span>
            )}

            <span className="text-xs text-faint">{formatDate(document.created_at)}</span>
          </span>

          {!expanded && (
            <span className="mt-1 block text-xs text-dim truncate">
              {document.content.slice(0, 120)}
            </span>
          )}
        </button>

        <button
          onClick={onToggle}
          className="text-xs text-dim hover:text-fg transition-colors shrink-0"
        >
          {expanded ? 'Hide' : 'View'}
        </button>
        <button
          onClick={onDelete}
          className="text-xs text-faint hover:text-closed transition-colors shrink-0"
        >
          Delete
        </button>
      </div>

      {expanded && (
        <div className="border-t border-line px-5 py-4 text-sm text-fg">
          <Markdown content={document.content} />
        </div>
      )}
    </li>
  )
}
