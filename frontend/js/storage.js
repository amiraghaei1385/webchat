// مدیریت localStorage

const StorageManager = (() => {
  const K = CONFIG.STORAGE_KEYS;

  function saveSession(token, userId, rememberMe) {
    localStorage.setItem(K.TOKEN, token);
    localStorage.setItem(K.USER_ID, userId);
    localStorage.setItem(K.REMEMBER_ME, rememberMe ? '1' : '0');
  }

  function getToken() {
    return localStorage.getItem(K.TOKEN);
  }

  function getUserId() {
    return localStorage.getItem(K.USER_ID);
  }

  function isLoggedIn() {
    return !!getToken();
  }

  function clearSession() {
    localStorage.removeItem(K.TOKEN);
    localStorage.removeItem(K.USER_ID);
    localStorage.removeItem(K.REMEMBER_ME);
  }

  function getTheme() {
    return localStorage.getItem(K.THEME) || 'dark';
  }

  function setTheme(theme) {
    localStorage.setItem(K.THEME, theme);
    document.documentElement.setAttribute('data-theme', theme);
  }

  function getBackgroundColor() {
    return localStorage.getItem(K.BG_COLOR) || 'aurora';
  }
  function setBackgroundColor(palette) {
    localStorage.setItem(K.BG_COLOR, palette);
    document.documentElement.setAttribute('data-bg-palette', palette);
  }

  function getFontSize() {
    return localStorage.getItem(K.FONT_SIZE) || 'medium';
  }
  function setFontSize(size) {
    localStorage.setItem(K.FONT_SIZE, size);
    document.documentElement.setAttribute('data-msg-font-size', size);
  }

  return {
    saveSession,
    getToken,
    getUserId,
    isLoggedIn,
    clearSession,
    getTheme,
    setTheme,
    getBackgroundColor,
    setBackgroundColor,
    getFontSize,
    setFontSize,
  };
})();
