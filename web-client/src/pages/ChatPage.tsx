import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { useAppSelector } from "~/store/hooks";
import { SessionList } from "~/components/chat/SessionList";
import { MessageBubble } from "~/components/chat/MessageBubble";
import { ChatInput } from "~/components/chat/ChatInput";
import { InterviewScoreCard } from "~/components/chat/InterviewScoreCard";
import { InterviewStartModal } from "~/components/chat/InterviewStartModal";
import {
  useCreateSessionMutation,
  useDeleteSessionMutation,
  useEndInterviewMutation,
  useGetMessagesQuery,
  useGetSessionsQuery,
  useSendMessageMutation,
  type InterviewResult,
  type Message,
  type Session,
} from "~/services/chat/chatApi";

type Tab = "assistant" | "interview";

const isInterview = (s: Session) => s.session_type === "mock_interview";

export default function ChatPage() {
  const [tab, setTab] = useState<Tab>("assistant");
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  // The live score card for the active interview — set when it finishes or is ended early.
  const [result, setResult] = useState<InterviewResult | null>(null);
  const [showStart, setShowStart] = useState(false);
  const [presetAppId, setPresetAppId] = useState<string | undefined>();
  const bottomRef = useRef<HTMLDivElement>(null);
  const bootstrapped = useRef(false);
  const user = useAppSelector((s) => s.auth.user);

  // Hand-off from another page: a prepared assistant command (prefill), or a request to open
  // the interview tab (optionally with an application pre-selected for the start modal).
  const nav = useLocation().state as
    | { prefill?: string; tab?: Tab; presetApplicationId?: string }
    | null;
  const prefill = nav?.prefill;

  const { data: sessionsData, isLoading: sessionsLoading } =
    useGetSessionsQuery();
  const sessions = sessionsData?.sessions ?? [];
  const assistantSessions = sessions.filter((s) => !isInterview(s));
  const interviewSessions = sessions.filter(isInterview);
  const visibleSessions = tab === "interview" ? interviewSessions : assistantSessions;

  const activeSession = sessions.find((s) => s.id === activeSessionId);
  const interviewDone =
    tab === "interview" && activeSession?.interview_status === "completed";

  const { data: historyData } = useGetMessagesQuery(activeSessionId ?? "", {
    skip: !activeSessionId,
  });
  const [createSession] = useCreateSessionMutation();
  const [deleteSession] = useDeleteSessionMutation();
  const [sendMessage, { isLoading: sending }] = useSendMessageMutation();
  const [endInterview, { isLoading: ending }] = useEndInterviewMutation();

  // Open straight into the interview tab when routed there (e.g. "Practice interview").
  useEffect(() => {
    if (nav?.tab !== "interview") return;
    setTab("interview");
    setPresetAppId(nav.presetApplicationId);
    setShowStart(true);
  }, [nav?.tab, nav?.presetApplicationId]);

  // On the assistant tab, auto-select the latest chat or create one so the input is ready.
  // The interview tab never auto-creates — an interview needs an application chosen first.
  useEffect(() => {
    if (
      tab !== "assistant" ||
      sessionsLoading ||
      !sessionsData ||
      activeSessionId ||
      bootstrapped.current
    )
      return;
    bootstrapped.current = true;

    if (assistantSessions.length > 0) {
      setActiveSessionId(assistantSessions[0].id);
    } else {
      createSession({ session_type: "insight_chat" })
        .unwrap()
        .then((s) => setActiveSessionId(s.id))
        .catch(() => {});
    }
  }, [tab, sessionsData, sessionsLoading, activeSessionId, assistantSessions, createSession]);

  // Sync message history when switching sessions
  useEffect(() => {
    if (!historyData) return;
    setMessages(
      historyData.messages.map((m) => ({ role: m.role, content: m.content })),
    );
  }, [historyData]);

  // Auto-scroll on new messages / a new score card
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, result]);

  function switchTab(next: Tab) {
    if (next === tab) return;
    setTab(next);
    setResult(null);
    setMessages([]);
    const list = next === "interview" ? interviewSessions : assistantSessions;
    setActiveSessionId(list[0]?.id ?? null);
  }

  function selectSession(id: string) {
    setMessages([]);
    setResult(null);
    setActiveSessionId(id);
  }

  async function handleNewSession() {
    if (tab === "interview") {
      setPresetAppId(undefined);
      setShowStart(true);
      return;
    }
    const session = await createSession({ session_type: "insight_chat" }).unwrap();
    setActiveSessionId(session.id);
    setMessages([]);
  }

  function handleInterviewStarted(session: Session) {
    setShowStart(false);
    setResult(null);
    setMessages([]);
    setActiveSessionId(session.id);
  }

  async function handleDeleteSession(id: string) {
    await deleteSession(id).unwrap();
    if (id !== activeSessionId) return;
    bootstrapped.current = false;
    setMessages([]);
    setResult(null);
    setActiveSessionId(null);
  }

  async function handleSend(message: string) {
    if (!activeSessionId) return;
    const sessionId = activeSessionId;
    setMessages((prev) => [...prev, { role: "user", content: message }]);

    try {
      const res = await sendMessage({ sessionId, message }).unwrap();
      if (activeSessionId !== sessionId) return;
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: res.response },
      ]);
      if (res.interview) setResult(res.interview);
    } catch {
      if (activeSessionId === sessionId) {
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: "Something went wrong. Please try again." },
        ]);
      }
    }
  }

  async function handleEndInterview() {
    if (!activeSessionId) return;
    try {
      const res = await endInterview(activeSessionId).unwrap();
      setResult(res);
    } catch {
      // Non-fatal: the score card just won't appear; the session stays as-is.
    }
  }

  return (
    <div className="flex h-full flex-col bg-ink text-fg">
      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-line px-4">
        {(["assistant", "interview"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => switchTab(t)}
            className={`-mb-px border-b-2 px-4 py-3 text-sm font-medium transition ${
              tab === t
                ? "border-offer text-fg"
                : "border-transparent text-dim hover:text-fg"
            }`}
          >
            {t === "assistant" ? "Assistant" : "Mock Interview"}
          </button>
        ))}
      </div>

      <div className="flex min-h-0 flex-1">
        <SessionList
          sessions={visibleSessions}
          activeId={activeSessionId}
          onSelect={selectSession}
          onNew={handleNewSession}
          onDelete={handleDeleteSession}
          loading={sessionsLoading}
          newLabel={tab === "interview" ? "+ New interview" : "+ New chat"}
          emptyHint={
            tab === "interview"
              ? "No interviews yet. Start one to practise against a job application."
              : undefined
          }
        />

        <div className="flex min-w-0 flex-1 flex-col">
          {/* Header */}
          <div className="flex items-center gap-3 border-b border-line px-6 py-4">
            <h1 className="mr-auto max-w-md truncate text-sm font-medium text-dim">
              {tab === "interview"
                ? activeSession
                  ? "Mock interview"
                  : "Start a mock interview"
                : (activeSession?.first_message ?? "New conversation")}
            </h1>
            {tab === "interview" && activeSession && !interviewDone && (
              <button
                onClick={handleEndInterview}
                disabled={ending}
                className="tag rounded-lg border border-line bg-raised-2 px-3 py-1.5 text-dim transition hover:text-fg disabled:opacity-50"
              >
                {ending ? "Ending…" : "End interview"}
              </button>
            )}
            {user?.email && (
              <span className="tag text-faint hidden md:block">{user.email}</span>
            )}
          </div>

          {/* Messages */}
          <div className="flex-1 space-y-4 overflow-y-auto px-6 py-4">
            {tab === "interview" && !activeSessionId && (
              <div className="flex h-full flex-col items-center justify-center gap-4 text-center">
                <p className="text-sm font-medium text-fg">Practice makes ready</p>
                <p className="max-w-sm text-xs text-faint">
                  Run a realistic interview tailored to one of your applications.
                  You'll get a score and feedback at the end.
                </p>
                <button
                  onClick={() => {
                    setPresetAppId(undefined);
                    setShowStart(true);
                  }}
                  className="cta rounded-lg px-4 py-2 text-sm font-medium"
                >
                  Start a mock interview
                </button>
              </div>
            )}

            {tab === "assistant" && !activeSessionId && (
              <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
                <span className="dot-live h-2 w-2 rounded-full bg-offer" />
                <p className="text-sm text-faint">Setting up your session…</p>
              </div>
            )}

            {messages.map((msg, i) => (
              <MessageBubble
                key={i}
                role={msg.role}
                content={msg.content}
                applicationId={
                  tab === "interview" ? undefined : activeSession?.application_id
                }
              />
            ))}

            {sending && (
              <div className="flex justify-start">
                <div className="rounded-2xl rounded-bl-sm bg-raised-2 px-4 py-3 text-sm text-dim">
                  {tab === "interview" ? "The interviewer is thinking…" : "Thinking…"}
                </div>
              </div>
            )}

            {result && <InterviewScoreCard result={result} />}

            {!result && interviewDone && (
              <p className="mx-auto w-full max-w-2xl rounded-xl border border-line bg-raised-2/40 px-4 py-3 text-center text-sm text-dim">
                Interview completed — score{" "}
                <span className="font-medium text-fg">
                  {activeSession?.interview_score}/100
                </span>
                .
              </p>
            )}

            <div ref={bottomRef} />
          </div>

          {tab === "interview" && interviewDone ? (
            <div className="border-t border-line px-6 py-4 text-center text-xs text-faint">
              This interview has ended. Start a new one to practise again.
            </div>
          ) : (
            <ChatInput
              onSend={handleSend}
              disabled={!activeSessionId || sending}
              prefill={tab === "interview" ? undefined : prefill}
            />
          )}
        </div>
      </div>

      {showStart && (
        <InterviewStartModal
          onClose={() => setShowStart(false)}
          onStarted={handleInterviewStarted}
          presetApplicationId={presetAppId}
        />
      )}
    </div>
  );
}
