import { Link, useNavigate } from 'react-router-dom';
import './index.css';
import { useEffect } from 'react';

export default function NotFoundPage() {
  const navigate = useNavigate();

  useEffect(() => {
    document.title = '404 - Page Not Found | AI Trip Assistant';
  }, []);

  return (
    <div className="nf-wrap">
      <div className="nf-card" role="main" aria-labelledby="nf-title">
        <div className="nf-header">
          <img src="/ai.trip.planner.icon.png" alt="AI Trip Assistant" />
          <span>AI Trip Assistant</span>
        </div>

        <div className="nf-content">
          <div className="nf-badge" aria-hidden="true">Error <span className="nf-code">404</span> · Page Not Found</div>
          <h1 id="nf-title">Sorry, this page can’t be found</h1>
          <p className="nf-sub">The address you requested doesn’t exist, has been moved, or is temporarily unavailable. You can go back to Home, head to Trips, or return to the previous page.</p>

          <div className="nf-actions">
            <Link className="nf-btn nf-primary" to="/">Go Home</Link>
            <Link className="nf-btn" to="/trips">Go to My Trips</Link>
            <button className="nf-link-btn" type="button" onClick={() => navigate(-1)} aria-label="Go back to previous page">Go Back</button>
          </div>

          <div className="nf-grid" aria-hidden="true">
            <div className="nf-hint">If this happened due to an internal link, please <strong>refresh the page</strong> or try again later.</div>
            <div className="nf-hint">If you arrived from an external link, check whether the <strong>URL is correct</strong>, or start from Home.</div>
          </div>

          <div className="nf-footer">Error code <span className="nf-code">404</span> · Not Found</div>
        </div>
      </div>
    </div>
  );
}
