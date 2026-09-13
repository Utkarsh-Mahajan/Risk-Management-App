import { Link, useNavigate } from 'react-router-dom'

export function AppHeader() {
  const navigate = useNavigate()

  return (
    <nav className="navbar navbar-dark bg-dark sticky-top shadow-sm mb-4">
      <div className="container">
        <Link to="/" className="navbar-brand fw-bold">Risk Register</Link>
        <button className="btn btn-primary" onClick={() => navigate('/risks/new')}>
          + New Risk
        </button>
      </div>
    </nav>
  )
}
