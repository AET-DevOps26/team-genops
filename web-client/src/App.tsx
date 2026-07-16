import { useEffect } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAppDispatch } from "~/store/hooks";
import { fetchMe } from "~/services/auth/authSlice";
import { ProtectedRoute } from "~/components/routing/ProtectedRoute";
import { PublicOnlyRoute } from "~/components/routing/PublicOnlyRoute";
import { AppShell } from "~/components/layout/AppShell";
import AuthPage from "~/pages/AuthPage";
import ChatPage from "~/pages/ChatPage";
import DashboardPage from "~/pages/DashboardPage";
import ApplicationsPage from "~/pages/ApplicationsPage";
import DocumentsPage from "~/pages/DocumentsPage";
import JobsPage from "~/pages/JobsPage";
import ProfilePage from "~/pages/ProfilePage";
import OnboardingWizard from "~/pages/onboarding/OnboardingWizard";

function App() {
  const dispatch = useAppDispatch();

  // Session bootstrap: ask /me once on mount. Until it resolves the gates show a Splash
  // (status === 'unknown'), so a refreshing user is never bounced to /login prematurely.
  useEffect(() => {
    dispatch(fetchMe());
  }, [dispatch]);

  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<AuthPage />} />
      </Route>
      <Route element={<ProtectedRoute />}>
        {/* Full-screen, outside the shell: the post-registration wizard. */}
        <Route path="/onboarding" element={<OnboardingWizard />} />
        <Route element={<AppShell />}>
          <Route index path="/" element={<DashboardPage />} />
          <Route path="/applications" element={<ApplicationsPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/jobs" element={<JobsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/chat" element={<ChatPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
