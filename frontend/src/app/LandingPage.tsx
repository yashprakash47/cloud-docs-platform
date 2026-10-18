import React, { useState } from 'react';
import { Footer } from './Footer';

interface LandingPageProps {
  healthStatus: string;
  onLogin: (email: string) => void;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
}

export function LandingPage({ healthStatus, onLogin, theme, onToggleTheme }: LandingPageProps) {
  const [authTab, setAuthTab] = useState<'signin' | 'signup'>('signin');
  const [emailInput, setEmailInput] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isGoogleModalOpen, setIsGoogleModalOpen] = useState(false);

  const mockGoogleAccounts = [
    { name: 'Alex Johnson', email: 'alex.johnson@gmail.com', avatar: 'AJ' },
    { name: 'Dev Lead (CloudDocs)', email: 'admin@clouddocs.com', avatar: 'CD' },
    { name: 'Sarah Miller', email: 'sarah.miller@enterprise.io', avatar: 'SM' },
  ];

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const cleanEmail = emailInput.trim();
    if (!cleanEmail) {
      setError('Please enter a valid email address');
      return;
    }
    if (!cleanEmail.includes('@') || !cleanEmail.includes('.')) {
      setError('Please enter a valid email address (e.g. user@example.com)');
      return;
    }
    setError(null);
    onLogin(cleanEmail);
  }

  function handleGoogleAccountSelect(selectedEmail: string) {
    setIsGoogleModalOpen(false);
    onLogin(selectedEmail);
  }

  return (
    <div className="landing-layout">
      {/* Landing Top Navigation Bar */}
      <header className="landing-nav">
        <div className="landing-nav-inner">
          <div className="landing-brand">
            <div className="brand-logo-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <span className="brand-logo-name">CloudDocs</span>
            <span className="landing-tag">Enterprise Platform</span>
          </div>

          <div className="landing-nav-links">
            <a href="#features" className="landing-link">Features</a>
            <a href="#architecture" className="landing-link">Architecture</a>
            <a href="#about" className="landing-link">About</a>
            <a href="/actuator/health" target="_blank" rel="noopener noreferrer" className="landing-link">Health</a>
          </div>

          <div className="landing-nav-actions">
            <div className="landing-health-pill" title="Real-time AWS EC2 Backend Health">
              <span className={`health-dot ${healthStatus.toLowerCase()}`} />
              <span className="health-label">EC2 Backend: {healthStatus}</span>
            </div>

            <button
              type="button"
              className="icon-action-btn"
              onClick={onToggleTheme}
              aria-label={`Toggle ${theme === 'light' ? 'dark' : 'light'} mode`}
              title={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
            >
              {theme === 'light' ? '🌙' : '☀️'}
            </button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="landing-hero-section">
        <div className="hero-glow-blob" />
        <div className="hero-container">
          <div className="hero-badge-pill">
            <span className="hero-badge-spark">✨</span>
            <span>Production Cloud Architecture on AWS</span>
          </div>

          <h1 className="hero-title">
            Next-Generation Cloud <span className="hero-gradient-text">Document Management</span>
          </h1>

          <p className="hero-subtitle">
            Enterprise multi-tenant document repository engineered on AWS S3, asynchronous Lambda bridges, Amazon SQS queue pipelines, and PostgreSQL 18 on Amazon RDS.
          </p>

          <div className="hero-cta-group">
            <a href="#auth-card" className="btn-hero-primary">
              Get Started Free
            </a>
            <a href="#features" className="btn-hero-secondary">
              Explore Platform Features ↓
            </a>
          </div>

          {/* Auth Card: Google SSO + Email Input */}
          <div id="auth-card" className="auth-card-wrapper">
            <div className="auth-card">
              <div className="auth-card-glow" />
              
              {/* Tabs */}
              <div className="auth-tabs">
                <button
                  type="button"
                  className={`auth-tab ${authTab === 'signin' ? 'active' : ''}`}
                  onClick={() => { setAuthTab('signin'); setError(null); }}
                >
                  Sign In
                </button>
                <button
                  type="button"
                  className={`auth-tab ${authTab === 'signup' ? 'active' : ''}`}
                  onClick={() => { setAuthTab('signup'); setError(null); }}
                >
                  Sign Up
                </button>
              </div>

              <div className="auth-card-body">
                <p className="auth-card-desc">
                  {authTab === 'signin'
                    ? 'Sign in to access your multi-tenant document workspaces.'
                    : 'Create your account with automatic tenant provisioning.'}
                </p>

                {/* Branded Google Sign-In Button */}
                <button
                  type="button"
                  className="google-sso-btn"
                  onClick={() => setIsGoogleModalOpen(true)}
                  aria-label="Continue with Google"
                >
                  <svg className="google-icon" viewBox="0 0 24 24" width="20" height="20">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                  <span className="google-btn-text">Continue with Google</span>
                </button>

                {/* Divider */}
                <div className="auth-divider">
                  <span className="divider-line" />
                  <span className="divider-text">or continue with email</span>
                  <span className="divider-line" />
                </div>

                {/* Email Form */}
                <form onSubmit={handleSubmit} className="auth-email-form">
                  <div className="auth-input-group">
                    <label htmlFor="user-email-input" className="auth-label">Work Email</label>
                    <div className="auth-input-wrap">
                      <svg className="input-email-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                        <polyline points="22,6 12,13 2,6"></polyline>
                      </svg>
                      <input
                        id="user-email-input"
                        type="email"
                        placeholder="name@company.com"
                        value={emailInput}
                        onChange={e => { setEmailInput(e.target.value); setError(null); }}
                        className="auth-input"
                        autoComplete="email"
                        required
                      />
                    </div>
                  </div>

                  {error && <div className="auth-error-banner">{error}</div>}

                  <button type="submit" className="auth-submit-btn">
                    {authTab === 'signin' ? 'Sign In to Workspace' : 'Create Free Account'}
                  </button>

                  <p className="auth-terms-hint">
                    By continuing, you agree to CloudDocs multi-tenant security policies and automated tenant provisioning.
                  </p>
                </form>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Smooth-Scrolling Feature Showcase Section */}
      <section id="features" className="landing-features-section">
        <div className="features-container">
          <div className="section-header-centered">
            <span className="section-sub-tag">CAPABILITIES</span>
            <h2 className="section-title">Built for Scalable Document Workflows</h2>
            <p className="section-description">
              Experience seamless enterprise lifecycle control from direct binary uploads to automated SQS processing and zero-download previews.
            </p>
          </div>

          <div className="features-grid">
            {/* Feature 1 */}
            <div className="feature-card">
              <div className="feature-icon-badge s3-accent">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
              </div>
              <h3 className="feature-card-title">Direct S3 Upload Intent</h3>
              <p className="feature-card-text">
                Bypass heavy web application servers with pre-signed direct S3 upload URLs. File binaries stream securely directly into isolated AWS S3 storage buckets.
              </p>
              <div className="feature-card-meta">
                <span className="feature-pill">S3 SSE-KMS</span>
                <span className="feature-pill">Pre-signed URLs</span>
              </div>
            </div>

            {/* Feature 2 */}
            <div className="feature-card">
              <div className="feature-icon-badge sqs-accent">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                </svg>
              </div>
              <h3 className="feature-card-title">Event-Driven SQS Processing</h3>
              <p className="feature-card-text">
                Every uploaded document triggers an AWS S3 event routed through Amazon SQS queues to asynchronous worker pools for virus scanning, OCR, and validation.
              </p>
              <div className="feature-card-meta">
                <span className="feature-pill">Amazon SQS</span>
                <span className="feature-pill">Async Workers</span>
              </div>
            </div>

            {/* Feature 3 */}
            <div className="feature-card">
              <div className="feature-icon-badge folder-accent">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                </svg>
              </div>
              <h3 className="feature-card-title">Folder Taxonomy &amp; Multi-Tenancy</h3>
              <p className="feature-card-text">
                Organize documents into structured folders, assign colorful taxonomy tags, and manage team member roles (Admin, Collaborator, Viewer) with strict data isolation.
              </p>
              <div className="feature-card-meta">
                <span className="feature-pill">Role RBAC</span>
                <span className="feature-pill">Custom Tags</span>
              </div>
            </div>

            {/* Feature 4 */}
            <div className="feature-card">
              <div className="feature-icon-badge preview-accent">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
              </div>
              <h3 className="feature-card-title">In-App Document Preview</h3>
              <p className="feature-card-text">
                Instantly view Adobe PDFs, plain text files, and high-res PNG images directly in your browser without downloading binaries to local storage.
              </p>
              <div className="feature-card-meta">
                <span className="feature-pill">Inline Viewer</span>
                <span className="feature-pill">30-Day Trash</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Architecture Highlights Section */}
      <section id="architecture" className="landing-arch-section">
        <div className="features-container">
          <div className="section-header-centered">
            <span className="section-sub-tag">SYSTEM TOPOLOGY</span>
            <h2 className="section-title">Built on Robust AWS Infrastructure</h2>
            <p className="section-description">
              Production-tested architecture combining modern Java 21, Spring Boot, and cloud-native AWS services.
            </p>
          </div>

          <div className="arch-diagram-card">
            <div className="arch-step">
              <div className="arch-step-num">01</div>
              <h4>Direct Client Upload</h4>
              <p>React SPA requests pre-signed URL and streams file payload directly into Amazon S3.</p>
            </div>
            <div className="arch-arrow">→</div>
            <div className="arch-step">
              <div className="arch-step-num">02</div>
              <h4>Event Trigger</h4>
              <p>Amazon S3 triggers ObjectCreated notification published to Amazon SQS queue.</p>
            </div>
            <div className="arch-arrow">→</div>
            <div className="arch-step">
              <div className="arch-step-num">03</div>
              <h4>EC2 Worker Pool</h4>
              <p>Spring Boot daemon consumes jobs, verifies checksums, and updates PostgreSQL RDS.</p>
            </div>
            <div className="arch-arrow">→</div>
            <div className="arch-step">
              <div className="arch-step-num">04</div>
              <h4>Live Dashboard</h4>
              <p>Users inspect pipelines, view documents inline, or restore from 30-day trash retention.</p>
            </div>
          </div>
        </div>
      </section>

      {/* Dark Footer (Styled after Odoo Reference) */}
      <Footer />

      {/* Google Account Picker Modal */}
      {isGoogleModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsGoogleModalOpen(false)}>
          <div className="modal-box google-picker-modal" onClick={e => e.stopPropagation()}>
            <div className="google-picker-header">
              <svg className="google-icon" viewBox="0 0 24 24" width="24" height="24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
              <div>
                <h3 className="google-picker-title">Sign in with Google</h3>
                <p className="google-picker-sub">Choose an account to continue to CloudDocs</p>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setIsGoogleModalOpen(false)}
                aria-label="Close Google account selection"
              >
                ✕
              </button>
            </div>

            <div className="google-accounts-list">
              {mockGoogleAccounts.map(account => (
                <button
                  key={account.email}
                  type="button"
                  className="google-account-item"
                  onClick={() => handleGoogleAccountSelect(account.email)}
                >
                  <div className="google-account-avatar">{account.avatar}</div>
                  <div className="google-account-details">
                    <span className="google-account-name">{account.name}</span>
                    <span className="google-account-email">{account.email}</span>
                  </div>
                  <span className="google-account-arrow">→</span>
                </button>
              ))}
            </div>

            <div className="google-custom-prompt">
              <span className="or-text">or use another Google account:</span>
              <div className="google-custom-input-row">
                <input
                  type="email"
                  placeholder="custom.user@gmail.com"
                  className="auth-input"
                  id="google-custom-email"
                  onKeyDown={e => {
                    if (e.key === 'Enter') {
                      const val = (e.currentTarget as HTMLInputElement).value.trim();
                      if (val && val.includes('@')) {
                        handleGoogleAccountSelect(val);
                      }
                    }
                  }}
                />
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => {
                    const el = document.getElementById('google-custom-email') as HTMLInputElement;
                    if (el && el.value.trim() && el.value.includes('@')) {
                      handleGoogleAccountSelect(el.value.trim());
                    }
                  }}
                >
                  Sign In
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
