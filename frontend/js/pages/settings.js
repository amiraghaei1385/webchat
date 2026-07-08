/**
 * pages/settings.js
 * منطق صفحه تنظیمات
 */

document.addEventListener('DOMContentLoaded', () => {
    // بررسی احراز هویت
    requireAuth();
    
    // بارگذاری اطلاعات کاربر
    loadUserInfo();
    
    // تنظیم تم
    setupThemeToggle();
});

/**
 * بارگذاری اطلاعات کاربر
 */
function loadUserInfo() {
    const user = StorageManager.getUser();
    
    const usernameDisplay = document.getElementById('username-display');
    const useridDisplay = document.getElementById('userid-display');
    
    if (usernameDisplay) {
        usernameDisplay.textContent = user.username || 'کاربر';
    }
    
    if (useridDisplay) {
        useridDisplay.textContent = `@${user.userId || 'unknown'}`;
    }
}

/**
 * تنظیم تم toggle
 */
function setupThemeToggle() {
    const currentTheme = StorageManager.getTheme();
    const themeToggle = document.getElementById('theme-toggle');
    
    if (themeToggle) {
        // اگر تم dark باشد، checkbox را check کنید
        themeToggle.checked = currentTheme === 'dark';
    }
}

/**
 * تغییر تم
 */
function toggleTheme() {
    const currentTheme = StorageManager.getTheme();
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    
    StorageManager.setTheme(newTheme);
    
    // Update checkbox
    const themeToggle = document.getElementById('theme-toggle');
    if (themeToggle) {
        themeToggle.checked = newTheme === 'dark';
    }
}

/**
 * مدیریت خروج
 */
function handleLogout() {
    const confirmed = confirm('آیا می‌خواهید از حساب خود خارج شوید؟');
    
    if (confirmed) {
        logoutUser();
        // خودکار به login ریدایرکت می‌شود
    }
}

/**
 * برگشتن به صفحه اصلی
 */
function goBack() {
    window.location.href = 'home.html';
}
