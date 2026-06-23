/**
 * pages/login.js
 * منطق صفحه ورود
 */

document.addEventListener('DOMContentLoaded', () => {
    // اگر کاربر لاگین شده باشد، به صفحه اصلی برود
    if (StorageManager.isLoggedIn()) {
        window.location.href = 'home.html';
        return;
    }
    
    // تنظیم Event Listeners
    setupEventListeners();
});

/**
 * تنظیم Event Listeners
 */
function setupEventListeners() {
    const form = document.getElementById('login-form');
    if (form) {
        form.addEventListener('submit', handleLogin);
    }
    
    // اجازه دادن Enter برای ارسال فرم
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleLogin(e);
            }
        });
    }
}

/**
 * مدیریت درخواست ورود
 */
async function handleLogin(e) {
    e.preventDefault();
    
    // دریافت مقادیر
    const username = getInputValue('username');
    const password = getInputValue('password');
    const rememberMe = document.getElementById('rememberMe').checked;
    
    // پاک کردن پیام‌های قبلی
    clearMessages();
    
    // Validation
    if (!username || !password) {
        showError('لطفاً نام کاربری و رمز عبور را وارد کنید');
        return;
    }
    
    // نمایش loading
    showLoading(true);
    disableSubmitButton(true);
    
    try {
        // درخواست API
        const response = await loginUser(username, password, rememberMe);
        
        if (response && response.token) {
            // ذخیره rememberMe
            if (rememberMe) {
                StorageManager.setRememberMe(true);
            }
            
            // نمایش پیام موفقیت
            showSuccess('ورود موفق! در حال بارگذاری...');
            
            // ریدایرکت به صفحه اصلی
            setTimeout(() => {
                window.location.href = 'home.html';
            }, 1000);
        } else {
            showError('مشکل در ورود. لطفاً دوباره سعی کنید');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError(error.message || 'خطا در ورود. لطفاً مجدداً تلاش کنید');
    } finally {
        // پنهان کردن loading
        showLoading(false);
        disableSubmitButton(false);
    }
}

/**
 * نمایش/پنهان کردن loading
 */
function showLoading(show) {
    const loading = document.getElementById('loading');
    if (loading) {
        loading.style.display = show ? 'block' : 'none';
    }
}

/**
 * فعال/غیرفعال کردن دکمه ارسال
 */
function disableSubmitButton(disable) {
    const btn = document.getElementById('login-btn');
    if (btn) {
        btn.disabled = disable;
    }
}

/**
 * نمایش پیام خطا
 */
function showError(message) {
    const errorElement = document.getElementById('error-message');
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
}

/**
 * نمایش پیام موفقیت
 */
function showSuccess(message) {
    const successElement = document.getElementById('success-message');
    if (successElement) {
        successElement.textContent = message;
        successElement.style.display = 'block';
    }
}

/**
 * پاک کردن پیام‌ها
 */
function clearMessages() {
    const errorElement = document.getElementById('error-message');
    const successElement = document.getElementById('success-message');
    
    if (errorElement) {
        errorElement.style.display = 'none';
    }
    if (successElement) {
        successElement.style.display = 'none';
    }
}
