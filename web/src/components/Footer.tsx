export function Footer() {
  return (
    <footer className="footer">
      <div className="shell footer-inner">
        <span className="legal">
          Siphon is for saving content you own or have permission to download. Respect each
          platform's terms of service and creators' rights.
        </span>
        <span>© {new Date().getFullYear()} Siphon</span>
      </div>
    </footer>
  );
}
