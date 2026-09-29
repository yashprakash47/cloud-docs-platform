import { useState } from 'react';
import { apiConfig } from '../config/api';
import './app.css';

type LoadState = 'idle' | 'loading' | 'success' | 'error';

export function App() {
  const [loadState, setLoadState] = useState<LoadState>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function checkBackend() {
    setLoadState('loading');
    setErrorMessage(null);

    try {
      const response = await fetch(`${apiConfig.baseUrl}/actuator/health`);
      if (!response.ok) {
        throw new Error(`Backend returned HTTP ${response.status}`);
      }
      setLoadState('success');
    } catch (error) {
      setLoadState('error');
      setErrorMessage(error instanceof Error ? error.message : 'Unable to reach the backend.');
    }
  }

  return (
    <main className="shell">
      <section className="hero" aria-labelledby="page-title">
        <p className="eyebrow">CloudDocs</p>
        <h1 id="page-title">Document management, ready to grow.</h1>
        <p className="intro">
          The local application shell is running. Business workflows will be added in later phases.
        </p>
        <div className="status-card" aria-live="polite">
          <span className={`status-dot status-${loadState}`} aria-hidden="true" />
          <div>
            <strong>Backend connection</strong>
            <p>
              {loadState === 'idle' && 'Ready to check the local Spring Boot service.'}
              {loadState === 'loading' && 'Checking the local health endpoint…'}
              {loadState === 'success' && 'The backend health endpoint is responding.'}
              {loadState === 'error' && errorMessage}
            </p>
          </div>
        </div>
        <button type="button" onClick={checkBackend} disabled={loadState === 'loading'}>
          {loadState === 'loading' ? 'Checking…' : 'Check backend health'}
        </button>
      </section>
    </main>
  );
}
