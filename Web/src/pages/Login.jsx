import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'

export default function Login(){
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  async function handleSubmit(e){
    e.preventDefault()
    setError(null)
    try{
      const resp = await api.post('/auth/login', { username, password })
      const { token, username: name } = resp.data
      if(token) localStorage.setItem('hc_token', token)
      if(name || username) localStorage.setItem('hc_username', name || username)
      navigate('/')
      window.location.reload()
    }catch(err){
      setError(err?.response?.data?.error || 'login_failed')
    }
  }

  return (
    <div className="page container">
      <h2>Login</h2>
      <form onSubmit={handleSubmit} className="login-form">
        <label>
          Benutzername
          <input value={username} onChange={e => setUsername(e.target.value)} />
        </label>

        <label>
          Passwort
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} />
        </label>

        <div style={{ marginTop: 12 }}>
          <button type="submit">Anmelden</button>
        </div>

        {error && <div className="error">Fehler: {error}</div>}
      </form>
    </div>
  )
}
