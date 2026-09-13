import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import 'bootstrap/dist/css/bootstrap.min.css'
import './styles/main.css'
import { AppHeader } from './components/AppHeader'
import { AppFooter } from './components/AppFooter'
import { RiskListPage } from './pages/RiskListPage'
import { RiskDetailPage } from './pages/RiskDetailPage'
import { RiskFormPage } from './pages/RiskFormPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppHeader />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<RiskListPage />} />
            <Route path="/risks/new" element={<RiskFormPage />} />
            <Route path="/risks/:id" element={<RiskDetailPage />} />
            <Route path="/risks/:id/edit" element={<RiskFormPage />} />
          </Routes>
        </main>
        <AppFooter />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>
)
