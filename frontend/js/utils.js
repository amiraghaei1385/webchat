// توابع کمکی عمومی

const Utils = (() => {

  const RULES = {
    length: (pw) => pw.length >= 8,
    upper: (pw) => /[A-Z]/.test(pw),
    lower: (pw) => /[a-z]/.test(pw),
    digit: (pw) => /[0-9]/.test(pw),
    special: (pw) => /[!@#$%^&*]/.test(pw),
  };

  function checkPasswordRules(password, username = '') {
    return {
      length: RULES.length(password),
      upper: RULES.upper(password),
      lower: RULES.lower(password),
      digit: RULES.digit(password),
      special: RULES.special(password),
      noUsername: !username || !password.toLowerCase().includes(username.toLowerCase()),
    };
  }

  function isPasswordValid(password, username = '') {
    const r = checkPasswordRules(password, username);
    return Object.values(r).every(Boolean);
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str ?? '';
    return div.innerHTML;
  }

  function debounce(fn, delay) {
    let timer;
    return (...args) => {
      clearTimeout(timer);
      timer = setTimeout(() => fn(...args), delay);
    };
  }

  function formatRelativeTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return '';
    const diffMs = Date.now() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return I18N.getLang() === 'fa' ? 'اکنون' : 'now';
    if (diffMin < 60) return `${diffMin}${I18N.getLang() === 'fa' ? ' دقیقه پیش' : 'm ago'}`;
    const diffHr = Math.floor(diffMin / 60);
    if (diffHr < 24) return `${diffHr}${I18N.getLang() === 'fa' ? ' ساعت پیش' : 'h ago'}`;
    return date.toLocaleDateString(I18N.getLang() === 'fa' ? 'fa-IR' : 'en-US');
  }

  return { checkPasswordRules, isPasswordValid, escapeHtml, debounce, formatRelativeTime };
})();
