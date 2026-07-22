// محدودیت ارسال پیام (ضداسپم)

const SpamGuard = (() => {
  let timestamps = [];

  function canSend() {
    const now = Date.now();
    timestamps = timestamps.filter(t => now - t < CONFIG.SPAM_WINDOW_MS);
    return timestamps.length < CONFIG.SPAM_MAX_MESSAGES;
  }

  function recordSend() {
    timestamps.push(Date.now());
  }

  return { canSend, recordSend };
})();
