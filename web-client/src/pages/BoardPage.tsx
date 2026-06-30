import { useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '~/store/hooks'
import { logout } from '~/services/auth/authSlice'
import { Button, Card, Tag } from '~/components/ui'

// Placeholder authenticated landing — proves the ProtectedRoute gate and the primitives.
// Real board views will live here / under pages/ once the application service exists.
export default function BoardPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const user = useAppSelector((s) => s.auth.user)

  return (
    <div className="grid min-h-screen place-items-center bg-ink px-5 text-fg">
      <Card
        className="w-full max-w-[420px] p-7"
        header={
          <>
            <Tag className="text-dim">Your board</Tag>
            <Tag className="text-faint">{user?.email}</Tag>
          </>
        }
      >
        <p className="px-6 py-8 text-center text-sm text-dim">
          You&apos;re in. The application board lands here once the{' '}
          <code className="font-mono text-fg">application</code> service ships.
        </p>
        <div className="px-6 pb-7 flex flex-col gap-2">
          <Button className="w-full" onClick={() => navigate('/chat')}>
            Open AI Assistant
          </Button>
          <Button variant="ghost" className="w-full" onClick={() => dispatch(logout())}>
            Sign out
          </Button>
        </div>
      </Card>
    </div>
  )
}
