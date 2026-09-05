const VARIANT_CLASS = {
  success: "status status--success",
  error: "status status--error",
  info: "status status--info",
  warning: "status status--warning",
};

export function StatusMessage({ variant = "info", children, onClose }) {
  if (!children) return null;

  return (
    <div className={VARIANT_CLASS[variant] ?? VARIANT_CLASS.info} role="status">
      <span>{children}</span>
      {onClose && (
        <button type="button" className="status__close" onClick={onClose} aria-label="Cerrar mensaje">
          x
        </button>
      )}
    </div>
  );
}

export function messageForError(error) {
  if (!error) return null;
  if (error.status === 400 && error.details) {
    const detail = Object.values(error.details)[0];
    return detail ?? error.message;
  }
  return error.message;
}

export function variantForStatus(status) {
  if (status >= 200 && status < 300) return "success";
  if (status === 401 || status === 403) return "warning";
  return "error";
}
