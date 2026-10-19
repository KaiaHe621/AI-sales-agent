import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import MainLayout from '@/layout/MainLayout';
import LoginPage from '@/pages/Login';
import ChatPage from '@/pages/Chat';
import KnowledgeBasePage from '@/pages/KnowledgeBase';
import KbDocumentsPage from '@/pages/KbDocuments';
import EvalPage from '@/pages/Evaluation';
import DashboardPage from '@/pages/Dashboard';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const isAuth = useAuthStore((s) => s.isAuthenticated());
  return isAuth ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <MainLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<Navigate to="/chat" replace />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="kb" element={<KnowledgeBasePage />} />
        <Route path="kb/:kbId/documents" element={<KbDocumentsPage />} />
        <Route path="eval" element={<EvalPage />} />
        <Route path="dashboard" element={<DashboardPage />} />
      </Route>
    </Routes>
  );
}
