import { useState } from 'react'

export default function AiTest() {
    const [loading, setLoading] = useState(false)
    const [text, setText] = useState('')
    const [err, setErr] = useState<string>()

    const callApi = async () => {
        setLoading(true); setErr(undefined); setText('')
        try {
            const res = await fetch('/api/ai/test')
            if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
            setText(await res.text())
        } catch (e:any) {
            setErr(e.message ?? String(e))
        } finally {
            setLoading(false)
        }
    }

    return (
        <div>
            <button onClick={callApi} disabled={loading}>
                {loading ? 'Calling…' : 'Test AI Connection'}
            </button>
            {err && <p style={{color:'red'}}>Error: {err}</p>}
            {text && <pre>{text}</pre>}
        </div>
    )
}
