import { useState } from 'react'

function App() {
  const [result, setResult] = useState<string | null>(null)

  async function testRegister() {
    try {
      const res = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/v1/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'test@test.com', password: 'password123' }),
      })
      const data = await res.json()
      setResult(JSON.stringify(data, null, 2))
    } catch (err) {
      setResult(`Error: ${err}`)
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-4 bg-gray-50">
      <h1 className="text-3xl font-bold text-gray-900">JobReady</h1>
      <button
        onClick={testRegister}
        className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
      >
        Test Register
      </button>
      {result && (
        <pre className="bg-white border rounded p-4 text-sm text-gray-800">{result}</pre>
      )}
    </div>
  )
}

export default App
