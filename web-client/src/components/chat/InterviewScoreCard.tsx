import type { InterviewResult } from "~/services/chat/chatApi";

// The palette has no red; amber (interview) is the design's caution colour. Two bands:
// green (offer) for a solid score, amber (interview) for anything that needs work.
function scoreTone(score: number): string {
  return score >= 70 ? "text-offer" : "text-interview";
}

function barTone(score: number): string {
  return score >= 70 ? "bg-offer" : "bg-interview";
}

export function InterviewScoreCard({ result }: { result: InterviewResult }) {
  return (
    <div className="anim-rise mx-auto w-full max-w-2xl rounded-2xl border border-line bg-raised-2/50 p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="tag text-faint">Interview result</p>
          {result.verdict && (
            <p className="mt-1 text-lg font-medium text-fg">{result.verdict}</p>
          )}
        </div>
        <div className="text-right">
          <span className={`font-display text-4xl font-semibold ${scoreTone(result.score)}`}>
            {result.score}
          </span>
          <span className="text-faint text-sm">/100</span>
        </div>
      </div>

      {result.ended_early && (
        <p className="mt-4 rounded-lg border border-interview/30 bg-interview/10 px-3 py-2 text-xs text-interview">
          You ended the interview early ({result.questions_answered} of{" "}
          {result.questions_total} questions answered), which lowered your score.
        </p>
      )}

      {result.summary && (
        <p className="mt-4 text-sm leading-relaxed text-dim">{result.summary}</p>
      )}

      {result.competencies.length > 0 && (
        <div className="mt-5 space-y-3">
          {result.competencies.map((c, i) => (
            <div key={i}>
              <div className="flex items-center justify-between text-xs">
                <span className="text-fg">{c.name}</span>
                <span className={scoreTone(c.score)}>{c.score}</span>
              </div>
              <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-raised">
                <div
                  className={`h-full rounded-full ${barTone(c.score)}`}
                  style={{ width: `${Math.max(0, Math.min(100, c.score))}%` }}
                />
              </div>
              {c.comment && (
                <p className="mt-1 text-xs text-faint">{c.comment}</p>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        {result.strengths.length > 0 && (
          <div>
            <p className="tag mb-2 text-offer">Strengths</p>
            <ul className="space-y-1.5">
              {result.strengths.map((s, i) => (
                <li key={i} className="text-xs leading-relaxed text-dim">
                  {s}
                </li>
              ))}
            </ul>
          </div>
        )}
        {result.improvements.length > 0 && (
          <div>
            <p className="tag mb-2 text-applied">To work on</p>
            <ul className="space-y-1.5">
              {result.improvements.map((s, i) => (
                <li key={i} className="text-xs leading-relaxed text-dim">
                  {s}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
