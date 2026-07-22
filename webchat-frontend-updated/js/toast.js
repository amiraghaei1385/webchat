// نوتیفیکیشن شناور

const Toast = (() => {
  function ensureRoot() {
    let root = document.getElementById('toastRoot');
    if (!root) {
      root = document.createElement('div');
      root.id = 'toastRoot';
      root.style.cssText = `
        position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
        display: flex; flex-direction: column; gap: 8px; z-index: 1000;
        align-items: center; pointer-events: none;
      `;
      document.body.appendChild(root);
    }
    return root;
  }

  function show(message, kind = 'success') {
    const root = ensureRoot();
    const el = document.createElement('div');
    const bg = kind === 'error' ? 'var(--state-error)' : 'var(--state-success)';
    el.style.cssText = `
      background: ${bg}; color: var(--aurora-deep, #1a1440); font-size: 13.5px; font-weight: 600;
      padding: 10px 20px; border-radius: 999px; box-shadow: var(--shadow-lift, 0 10px 30px rgba(0,0,0,0.3));
      opacity: 0; transform: translateY(12px); transition: all 0.25s ease;
    `;
    el.textContent = message;
    root.appendChild(el);

    requestAnimationFrame(() => {
      el.style.opacity = '1';
      el.style.transform = 'translateY(0)';
    });

    setTimeout(() => {
      el.style.opacity = '0';
      el.style.transform = 'translateY(12px)';
      setTimeout(() => el.remove(), 250);
    }, 2400);
  }

  return { show };
})();
