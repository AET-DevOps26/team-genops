import { api } from "~/services/apiClient";

export interface Session {
  id: string;
  user_id: string;
  session_type: string;
  /** The application this chat is about, bound on first reference; null for a general chat. */
  application_id: string | null;
  summary: string | null;
  first_message: string | null;
  created_at: string;
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
    createSession: build.mutation<Session, { session_type?: string }>({
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
      { response: string },
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
  }),
});

export const {
  useCreateSessionMutation,
  useDeleteSessionMutation,
  useGetSessionsQuery,
  useGetMessagesQuery,
  useSendMessageMutation,
} = chatApi;
