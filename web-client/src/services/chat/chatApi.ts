import { api } from "~/services/apiClient";

export interface Session {
  id: string;
  user_id: string;
  session_type: string;
  /** The application this chat is about, bound on first reference; null for a general chat. */
  application_id: string | null;
  summary: string | null;
  first_message: string | null;
  /** Mock-interview lifecycle; null for every other session type. */
  interview_status: "in_progress" | "completed" | null;
  /** Final mock-interview score (0-100), set once completed. */
  interview_score: number | null;
  created_at: string;
}

export interface InterviewCompetency {
  name: string;
  score: number;
  comment: string | null;
}

/** The score card returned when a mock interview finishes (last answer) or is ended early. */
export interface InterviewResult {
  score: number;
  verdict: string | null;
  summary: string | null;
  competencies: InterviewCompetency[];
  strengths: string[];
  improvements: string[];
  ended_early: boolean;
  questions_answered: number;
  questions_total: number;
}

export interface Message {
  role: "user" | "assistant";
  content: string;
}

export interface MessageItem {
  id: string;
  role: "user" | "assistant";
  content: string;
  created_at: string;
}

const chatApi = api.injectEndpoints({
  endpoints: (build) => ({
    createSession: build.mutation<
      Session,
      { session_type?: string; application_id?: string }
    >({
      query: (body) => ({
        url: "/chat/sessions",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Chat"],
    }),

    getSessions: build.query<{ sessions: Session[] }, void>({
      query: () => "/chat/sessions",
      providesTags: ["Chat"],
    }),

    getMessages: build.query<{ messages: MessageItem[] }, string>({
      query: (sessionId) => `/chat/sessions/${sessionId}/messages`,
      providesTags: (_result, _error, sessionId) => [
        { type: "Messages", id: sessionId },
      ],
    }),

    deleteSession: build.mutation<void, string>({
      query: (sessionId) => ({
        url: `/chat/sessions/${sessionId}`,
        method: "DELETE",
        responseHandler: "text", // 204 No Content has no body; JSON.parse('') throws PARSING_ERROR
      }),
      invalidatesTags: ["Chat"],
    }),

    sendMessage: build.mutation<
      { response: string; interview?: InterviewResult | null },
      { sessionId: string; message: string }
    >({
      query: ({ sessionId, message }) => ({
        url: `/chat/sessions/${sessionId}/messages`,
        method: "POST",
        body: { message },
      }),
      invalidatesTags: (_result, _error, { sessionId }) => [
        "Chat",
        { type: "Messages", id: sessionId },
      ],
    }),

    endInterview: build.mutation<InterviewResult, string>({
      query: (sessionId) => ({
        url: `/chat/sessions/${sessionId}/end-interview`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, sessionId) => [
        "Chat",
        { type: "Messages", id: sessionId },
      ],
    }),
  }),
});

export const {
  useCreateSessionMutation,
  useDeleteSessionMutation,
  useGetSessionsQuery,
  useGetMessagesQuery,
  useSendMessageMutation,
  useEndInterviewMutation,
} = chatApi;
