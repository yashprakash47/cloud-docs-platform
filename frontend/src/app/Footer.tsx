import React, { useState } from 'react';

export function Footer() {
  const [lang, setLang] = useState('en');

  return (
    <footer className="cd-footer">
      <div className="cd-footer-inner">
        {/* Top Centered Brand Logo */}
        <div className="cd-footer-top-logo">
          <div className="footer-brand-badge">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
            <span className="footer-brand-text">CloudDocs</span>
          </div>
        </div>

        {/* 4 Navigation Columns + Right Value Prop Column */}
        <div className="cd-footer-grid">
          <div className="cd-footer-col">
            <h4 className="cd-footer-heading">Platform</h4>
            <ul className="cd-footer-links">
              <li><a href="#features">Document Manager</a></li>
              <li><a href="#features">S3 Storage Engine</a></li>
              <li><a href="#features">SQS Workflows</a></li>
              <li><a href="#features">Trash &amp; Retention</a></li>
            </ul>
          </div>

          <div className="cd-footer-col">
            <h4 className="cd-footer-heading">Architecture</h4>
            <ul className="cd-footer-links">
              <li><a href="#architecture">Java 21 &amp; Spring Boot 3.5</a></li>
              <li><a href="#architecture">PostgreSQL 18 RDS</a></li>
              <li><a href="#architecture">AWS Lambda Bridge</a></li>
              <li><a href="#architecture">Flyway Migrations</a></li>
            </ul>
          </div>

          <div className="cd-footer-col">
            <h4 className="cd-footer-heading">Developers</h4>
            <ul className="cd-footer-links">
              <li><a href="/actuator/health" target="_blank" rel="noopener noreferrer">Health Actuator</a></li>
              <li><a href="https://github.com" target="_blank" rel="noopener noreferrer">REST API Docs</a></li>
              <li><a href="https://github.com" target="_blank" rel="noopener noreferrer">GitHub Repository</a></li>
              <li><a href="#changelog">Release Notes</a></li>
            </ul>
          </div>

          <div className="cd-footer-col">
            <h4 className="cd-footer-heading">About us</h4>
            <ul className="cd-footer-links">
              <li><a href="#about">CloudDocs Project</a></li>
              <li><a href="#security">Security &amp; Compliance</a></li>
              <li><a href="#adrs">Architecture Decisions (ADRs)</a></li>
              <li><a href="#contact">Contact</a></li>
            </ul>
          </div>

          {/* Right Column: Language selector, blurb, socials */}
          <div className="cd-footer-col cd-footer-col-right">
            <div className="cd-footer-lang-picker">
              <span className="lang-globe-icon">🌐</span>
              <select
                value={lang}
                onChange={e => setLang(e.target.value)}
                className="cd-footer-lang-select"
                aria-label="Select Language"
              >
                <option value="en">🇺🇸 English</option>
                <option value="es">🇪🇸 Español</option>
                <option value="de">🇩🇪 Deutsch</option>
                <option value="fr">🇫🇷 Français</option>
              </select>
            </div>

            <p className="cd-footer-blurb">
              CloudDocs delivers enterprise document lifecycle management backed by resilient AWS cloud infrastructure. Designed for high throughput, absolute data isolation, and audit readiness.
            </p>

            <div className="cd-footer-socials">
              <a href="https://github.com" target="_blank" rel="noreferrer" aria-label="GitHub" className="cd-social-icon" title="GitHub">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                </svg>
              </a>
              <a href="https://x.com" target="_blank" rel="noreferrer" aria-label="X (Twitter)" className="cd-social-icon" title="X (Twitter)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                </svg>
              </a>
              <a href="https://linkedin.com" target="_blank" rel="noreferrer" aria-label="LinkedIn" className="cd-social-icon" title="LinkedIn">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z" />
                </svg>
              </a>
              <a href="https://discord.com" target="_blank" rel="noreferrer" aria-label="Discord" className="cd-social-icon" title="Discord">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994.021-.041.001-.09-.041-.106a13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.061 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.894.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.028zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
                </svg>
              </a>
            </div>
          </div>
        </div>

        {/* Bottom Sub-Footer Bar */}
        <div className="cd-footer-bottom">
          <p className="cd-footer-bottom-text">
            CloudDocs Platform — Built on AWS Architecture
          </p>
          <div className="cd-footer-bottom-links">
            <span>© {new Date().getFullYear()} CloudDocs Inc. All rights reserved.</span>
            <span className="dot-sep">•</span>
            <a href="#privacy">Privacy</a>
            <span className="dot-sep">•</span>
            <a href="#terms">Terms</a>
            <span className="dot-sep">•</span>
            <a href="#status">Security</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
