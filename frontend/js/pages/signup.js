/**
 * pages/signup.js
 * منطق صفحه ثبت‌نام
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
    const form = document.getElementById('signup-form');
    if (form) {
        form.addEventListener('submit', handleSignup);
    }
    
    // نمایش requirements پسورد به صورت live
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('input', updatePasswordRequirements);
    }
}

/**
 * بروزرسانی نمایش requirements پسورد
 */
function updatePasswordRequirements() {
    const password = getInputValue('password');
    
    // بررسی هر requirement
    const requirements = {
        'req-length': password.length >= 8,
        'req-uppercase': /[A-Z]/.test(password),
        'req-lowercase': /[a-z]/.test(password),
        'req-number': /\d/.test(password),
        'req-special': /[!@%$#^&*]/.test(password)
    };
    
    // بروزرسانی UI
    for (const [id, met] of Object.entries(requirements)) {
        const element = document.getElementById(id);
        if (element) {
            if (met) {
                element.classList.add('met');
            } else {
                element.classList.remove('met');
            }
        }
    }
}

/**
 * مدیریت درخواست ثبت‌نام
 */
async function handleSignup(e) {
    e.preventDefault();
    
    // دریافت مقادیر
    const username = getInputValue('username');
    const userId = getInputValue('userId');
    const password = getInputValue('password');
    const confirmPassword = getInputValue('confirmPassword');
    
    // پاک کردن پیام‌های قبلی
    clearMessages();
    
    // Validation
    const validation = validateFormInputs(username, userId, password, confirmPassword);
    if (!validation.valid) {
        showError(validation.error);
        return;
    }
    
    // نمایش loading
    showLoading(true);
    disableSubmitButton(true);
    
    try {
        // درخواست API
        const response = await registerUser(userId, username, password, confirmPassword);
        
        if (response && response.token) {
            // نمایش پیام موفقیت
            showSuccess('ثبت‌نام موفق! در حال بارگذاری...');
            
            // ریدایرکت به صفحه اصلی
            setTimeout(() => {
                window.location.href = 'home.html';
            }, 1500);
        } else {
            showError('مشکل در ثبت‌نام. لطفاً دوباره سعی کنید');
        }
    } catch (error) {
        console.error('Signup error:', error);
        showError(error.message || 'خطا در ثبت‌نام. لطفاً مجدداً تلاش کنید');
    } finally {
        // پنهان کردن loading
        showLoading(false);
        disableSubmitButton(false);
    }
}

/**
 * Validation فرم
 */
function validateFormInputs(username, userId, password, confirmPassword) {
    // بررسی نام کاربری
    if (!username || username.trim().length === 0) {
        return {
            valid: false,
            error: 'نام کاربری نمی‌تواند خالی باشد'
        };
    }
    
    if (username.length < 3) {
        return {
            valid: false,
            error: 'نام کاربری حداقل 3 کاراکتر باید باشد'
        };
    }
    
    // بررسی آیدی
    if (!userId || userId.trim().length === 0) {
        return {
            valid: false,
            error: 'آیدی نمی‌تواند خالی باشد'
        };
    }
    
    if (userId.length < 3) {
        return {
            valid: false,
            error: 'آیدی حداقل 3 کاراکتر باید باشد'
        };
    }
    
    // بررسی رمز عبور
    const passValidation = validatePassword(password);
    if (!passValidation.valid) {
        const requirements = passValidation.errors.join(', ');
        return {
            valid: false,
            error: `رمز عبور باید شامل: ${requirements}`
        };
    }
    
    // بررسی تطابق رمز‌ها
    if (password !== confirmPassword) {
        return {
            valid: false,
            error: 'رمز‌های عبور مطابقت ندارند'
        };
    }
    
    return { valid: true };
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
    const btn = document.getElementById('signup-btn');
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
