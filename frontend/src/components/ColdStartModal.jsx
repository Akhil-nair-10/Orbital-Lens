import { useEffect, useRef, useState } from "react";

/**
 * Popup shown on every page load explaining that the backend runs on Render's
 * free tier and may need time to wake up (cold start).
 * Closes via the button, the Escape key, or a click on the dark backdrop.
 */
function ColdStartModal({ isConnected }) {
  const [open, setOpen] = useState(true);
  const closeBtnRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    closeBtnRef.current?.focus();
    const onKeyDown = (e) => {
      if (e.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open]);

  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={() => setOpen(false)}>
      <div
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cold-start-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="cold-start-title" className="modal-title">
          Welcome to OrbitalLens
        </h2>
        <p className="modal-text">
          This application is deployed on Render&apos;s free tier, so the server may be
          asleep and need time to wake up (a &quot;cold start&quot;). Data can take a while
          to appear. Please be patient and keep this page open, even if it takes a lot
          of retries. It retries automatically.
        </p>

        <div className={`modal-status ${isConnected ? "connected" : ""}`}>
          {isConnected ? (
            <span>Connected. Live data loaded.</span>
          ) : (
            <>
              <span className="modal-spinner" />
              <span>Waiting for the server to respond...</span>
            </>
          )}
        </div>

        <button ref={closeBtnRef} className="submit-btn" onClick={() => setOpen(false)}>
          Got it
        </button>
      </div>
    </div>
  );
}

export default ColdStartModal;