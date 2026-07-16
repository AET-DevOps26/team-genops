import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { useAppSelector } from "~/store/hooks";
import { SessionList } from "~/components/chat/SessionList";
import { MessageBubble } from "~/components/chat/MessageBubble";
import { ChatInput } from "~/components/chat/ChatInput";
import {
  useCreateSessionMutation,
  useDeleteSessionMutation,
  useGetMessagesQuery,
  useGetSessionsQuery,
  useSendMessageMutation,
  type Message,
} from "~/services/chat/chatApi";

export default function ChatPage() {
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const bottomRef = useRef<HTMLDivElement>(null);
  const bootstrapped = useRef(false);
  const user = useAppSelector((s) => s.auth.user);
  // An application page can hand off a prepared command (e.g. "/cover_letter …
  // application id: <uuid>") via router state — it lands in the input, the user sends it.
  const prefill = (useLocation().state as { prefill?: string } | null)?.prefill;

  const { data: sessionsData, isLoading: sessionsLoading } =
    useGetSessionsQuery();
  const activeSession = sessionsData?.sessions.find(
    (s) => s.id === activeSessionId,
  );
  const { data: historyData } = useGetMessagesQuery(activeSessionId ?? "", {
    skip: !activeSessionId,
  });
  const [createSession] = useCreateSessionMutation();
  const [deleteSession] = useDeleteSessionMutation();
  const [sendMessage, { isLoading: sending }] = useSendMessageMutation();

  // Auto-select the latest session, or create one, so the input is ready immediately
  useEffect(() => {
    if (
      sessionsLoading ||
      !sessionsData ||
      activeSessionId ||
      bootstrapped.current
    )
      return;
    bootstrapped.current = true;

    const sessions = sessionsData.sessions;
    if (sessions.length > 0) {
      setActiveSessionId(sessions[0].id);
    } else {
      createSession({ session_type: "insight_chat" })
        .unwrap()
        .then((s) => setActiveSessionId(s.id))
        .catch(() => {});
    }
  }, [sessionsData, sessionsLoading, activeSessionId, createSession]);

  // Sync message history when switching sessions
  useEffect(() => {
    if (!historyData) return;
    setMessages(
      historyData.messages.map((m) => ({ role: m.role, content: m.content })),
    );
  }, [historyData]);

  // Auto-scroll on new messages
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function handleNewSession() {
    const session = await createSession({
      session_type: "insight_chat",
    }).unwrap();
    setActiveSessionId(session.id);
    setMessages([]);
  }

  function handleSelectSession(id: string) {
    setMessages([]);
    setActiveSessionId(id);
  }

  async function handleDeleteSession(id: string) {
    await deleteSession(id).unwrap();
    if (id !== activeSessionId) return;

    // Deleted the active session — prevent bootstrap effect from reselecting stale data
    // by resetting the bootstrap flag so it waits for fresh data after invalidation
    bootstrapped.current = false;
    setMessages([]);
    setActiveSessionId(null);

    // The bootstrap effect will run again after RTK Query invalidates and refetches sessions,
    // selecting the first available session or creating a new one from fresh data
  }

  async function handleSend(message: string) {
    if (!activeSessionId) return;

    // Capture session id before async call to ensure late-arriving responses
    // don't get appended to the wrong chat if user switches sessions mid-flight
    const sessionId = activeSessionId;

    setMessages((prev) => [...prev, { role: "user", content: message }]);

    try {
      const result = await sendMessage({ sessionId, message }).unwrap();
      // Only append if user is still viewing the same session
      if (activeSessionId === sessionId) {
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: result.response },
        ]);
      }
    } catch {
      // Only append error if user is still viewing the same session
      if (activeSessionId === sessionId) {
        setMessages((prev) => [
          ...prev,
          {
            role: "assistant",
            content: "Something went wrong. Please try again.",
          },
        ]);
      }
    }
  }

  return (
    <div className="flex h-full bg-ink text-fg">
      <SessionList
        sessions={sessionsData?.sessions ?? []}
        activeId={activeSessionId}
        onSelect={handleSelectSession}
        onNew={handleNewSession}
        onDelete={handleDeleteSession}
        loading={sessionsLoading}
      />

      <div className="flex flex-col flex-1 min-w-0">
        {/* Header */}
        <div className="flex items-center gap-3 px-6 py-4 border-b border-line">
          <h1 className="text-sm font-medium text-dim truncate max-w-md mr-auto">
            {activeSession?.first_message ?? "New conversation"}
          </h1>
          {user?.email && (
            <span className="tag text-faint hidden md:block">{user.email}</span>
          )}
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
          {!activeSessionId && (
            <div className="flex flex-col items-center justify-center h-full gap-3 text-center">
              <span className="w-2 h-2 rounded-full bg-offer dot-live" />
              <p className="text-faint text-sm">Setting up your session…</p>
            </div>
          )}
          {activeSessionId && messages.length === 0 && (
            <div className="flex flex-col items-center justify-center h-full gap-4 text-center anim-rise">
              <div className="grid h-12 w-12 place-items-center rounded-full bg-offer/15 text-offer">
                <svg
                  width="22"
                  height="22"
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden
                >
                  <path
                    d="M12 3C6.48 3 2 6.92 2 11.75c0 2.6 1.28 4.94 3.32 6.6L4 21l3.5-1.75A11.3 11.3 0 0 0 12 20.5c5.52 0 10-3.92 10-8.75S17.52 3 12 3Z"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    strokeLinejoin="round"
                  />
                </svg>
              </div>
              <div className="space-y-1">
                <p className="text-fg text-sm font-medium">
                  How can I help you today?
                </p>
                <p className="text-faint text-xs">
                  Try a command or just ask a question
                </p>
              </div>
              <div className="flex flex-wrap gap-2 justify-center">
                {["/cover_letter", "/resume_tailor", "/fit_analysis"].map(
                  (cmd) => (
                    <span
                      key={cmd}
                      className="font-mono text-xs bg-raised border border-line text-applied px-3 py-1.5 rounded-lg"
                    >
                      {cmd}
                    </span>
                  ),
                )}
              </div>
            </div>
          )}
          {messages.map((msg, i) => (
            <MessageBubble key={i} role={msg.role} content={msg.content} />
          ))}
          {sending && (
            <div className="flex justify-start">
              <div className="bg-raised-2 rounded-2xl rounded-bl-sm px-4 py-3 text-sm text-dim">
                Thinking…
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <ChatInput
          onSend={handleSend}
          disabled={!activeSessionId || sending}
          prefill={prefill}
        />
      </div>
    </div>
  );
}
