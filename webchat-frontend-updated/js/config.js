// تنظیمات API و ثابت‌های کلی
// این فایل آدرس بک‌اند واقعی (پروژه Backend.zip) را مشخص می‌کند.
// در صورت اجرا روی سرور/دامنه دیگر، فقط همین دو مقدار را تغییر دهید.

const CONFIG = Object.freeze({
  API_BASE_URL: 'http://localhost:8080/api', // HttpApiServer -> HTTP_PORT = 8080, همه route ها با /api شروع می‌شوند
  WS_URL: 'ws://localhost:8081',              // ChatWebSocketServer -> WS_PORT = 8081

  POLLING_INTERVAL_MS: 3000,

  SPAM_MAX_MESSAGES: 5,
  SPAM_WINDOW_MS: 1000,

  MAX_MESSAGE_LENGTH: 2000,

  STORAGE_KEYS: {
    TOKEN: 'wc_auth_token',
    USER_ID: 'wc_user_id',
    REMEMBER_ME: 'wc_remember_me',
    LANG: 'wc_lang',
    THEME: 'wc_theme',
    BG_COLOR: 'wc_bg_color',
    FONT_SIZE: 'wc_font_size',
  },
});
