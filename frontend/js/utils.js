/**
 * utils.js
 * توابع کمکی مختلف برای استفاده در سرتاسر برنامه
 */

// ============================================
// VALIDATION FUNCTIONS
// ============================================

/**
 * بررسی صحت رمز عبور طبق معیارهای پروژه
 * - حداقل 8 کاراکتر
 * - شامل حرف بزرگ (A-Z)
 * - شامل حرف کوچک (a-z)
 * - شامل عدد (0-9)
 * - شامل کاراکتر خاص (!@%$#^&*)
 */
function validatePassword(password) {
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@%$#^&*])[a-zA-Z\d!@%$#^&*]{8,}$/;
    
    if (!regex.test(password)) {
        const errors = [];
        if (password.length < 8) errors.push('حداقل 8 کاراکتر');
        if (!/[a-z]/.test(password)) errors.push('حرف کوچک');
        if (!/[A-Z]/.test(password)) errors.push('حرف بزرگ');
        if (!/\d/.test(password)) errors.push('عدد');
        if (!/[!@%$#^&*]/.test(password)) errors.push('کاراکتر خاص');
        
        return {
            valid: false,
            errors: errors
        };
    }
    
    return { valid: true, errors: [] };
}

/**
 * بررسی صحت نام کاربری
 */
function validateUsername(username) {
    if (!username || username.trim().length === 0) {
        return { valid: false, error: 'نام کاربری نمی‌تواند خالی باشد' };
    }
    if (username.trim().length < 3) {
        return { valid: false, error: 'نام کاربری حداقل 3 کاراکتر باید باشد' };
    }
    return { valid: true, error: null };
}

/**
 * بررسی صحت آیدی (شناسه کاربری)
 */
function validateUserId(userId) {
    if (!userId || userId.trim().length === 0) {
        return { valid: false, error: 'آیدی نمی‌تواند خالی باشد' };
    }
    if (userId.trim().length < 3) {
        return { valid: false, error: 'آیدی حداقل 3 کاراکتر باید باشد' };
    }
    return { valid: true, error: null };
}

/**
 * بررسی صحت ایمیل
 */
function validateEmail(email) {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

/**
 * بررسی تطابق دو رمز عبور
 */
function validatePasswordMatch(password, confirmPassword) {
    return password === confirmPassword;
}

// ============================================
// FORMAT FUNCTIONS
// ============================================

/**
 * تبدیل تاریخ/زمان به فرمت خوانا
 * - امروز: ساعت
 * - دیروز: دیروز
 * - قدیمی‌تر: تاریخ
 */
function formatDate(dateString) {
    if (!dateString) return '';
    
    try {
        const date = new Date(dateString);
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);
        
        // امروز
        if (date.toDateString() === today.toDateString()) {
            return date.toLocaleTimeString('fa-IR', {
                hour: '2-digit',
                minute: '2-digit'
            });
        }
        
        // دیروز
        if (date.toDateString() === yesterday.toDateString()) {
            return 'دیروز';
        }
        
        // قدیمی‌تر
        return date.toLocaleDateString('fa-IR');
    } catch (error) {
        console.error('formatDate error:', error);
        return dateString;
    }
}

/**
 * تبدیل فرمت تاریخ/زمان مفصل
 */
function formatFullDate(dateString) {
    if (!dateString) return '';
    
    try {
        const date = new Date(dateString);
        return date.toLocaleString('fa-IR');
    } catch (error) {
        console.error('formatFullDate error:', error);
        return dateString;
    }
}

/**
 * کاهش متن طولانی
 */
function truncateText(text, maxLength = 50) {
    if (!text) return '';
    const trimmed = text.trim();
    if (trimmed.length <= maxLength) return trimmed;
    return trimmed.substring(0, maxLength) + '...';
}

/**
 * ✅ بهتر کردن: حفاظت از XSS - Escape HTML Special Characters
 */
function escapeHtml(unsafe) {
    if (!unsafe) return '';
    
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    
    return String(unsafe).replace(/[&<>"']/g, char => map[char]);
}

/**
 * تبدیل متن عادی به HTML با حفاظت از newlines
 */
function textToHtml(text) {
    return escapeHtml(text).replace(/\n/g, '<br>');
}

// ============================================
// DOM HELPER FUNCTIONS
// ============================================

/**
 * نمایش عنصر DOM
 */
function showElement(element) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.style.display = 'block';
    }
}

/**
 * پنهان کردن عنصر DOM
 */
function hideElement(element) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.style.display = 'none';
    }
}

/**
 * تغییر class
 */
function toggleClass(element, className) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.classList.toggle(className);
    }
}

/**
 * افزودن class
 */
function addClass(element, className) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.classList.add(className);
    }
}

/**
 * حذف class
 */
function removeClass(element, className) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.classList.remove(className);
    }
}

/**
 * ✅ بهتر شده: دریافت مقدار input با خطا handling
 */
function getInputValue(elementId) {
    const element = document.getElementById(elementId);
    if (!element) {
        console.warn(`Element with id "${elementId}" not found`);
        return '';
    }
    return element.value ? element.value.trim() : '';
}

/**
 * تنظیم مقدار input
 */
function setInputValue(elementId, value) {
    const element = document.getElementById(elementId);
    if (!element) {
        console.warn(`Element with id "${elementId}" not found`);
        return;
    }
    element.value = value || '';
}

/**
 * تخالی کردن input
 */
function clearInput(elementId) {
    setInputValue(elementId, '');
}

// ============================================
// NOTIFICATION FUNCTIONS
// ============================================

/**
 * نمایش پیام خطا
 */
function showError(message, elementId = 'error-message') {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = 'message error-message';
        element.style.display = 'block';
    } else {
        console.error('Error:', message);
        alert('خطا: ' + message);
    }
}

/**
 * نمایش پیام موفقیت
 */
function showSuccess(message, elementId = 'success-message') {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = 'message success-message';
        element.style.display = 'block';
    } else {
        console.log('Success:', message);
        alert('موفق: ' + message);
    }
}

/**
 * نمایش پیام معلومات
 */
function showInfo(message, elementId = 'info-message') {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = 'message info-message';
        element.style.display = 'block';
    }
}

/**
 * پاک کردن پیام‌ها
 */
function clearMessages() {
    const messages = document.querySelectorAll('.message');
    messages.forEach(msg => {
        msg.style.display = 'none';
    });
}

/**
 * نمایش خودکار پیام با timeout
 */
function showAutoMessage(message, type = 'info', duration = 3000, elementId = 'message') {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.className = `message ${type}-message`;
        element.style.display = 'block';
        
        setTimeout(() => {
            element.style.display = 'none';
        }, duration);
    }
}

// ============================================
// ROUTER & NAVIGATION
// ============================================

/**
 * ✅ بهتر شده: ریدایرکت به صفحه دیگر با مسیرهای مطلق
 */
function navigate(page) {
    if (!page) {
        console.error('navigate: page parameter is required');
        return;
    }
    
    // استفاده از مسیر مطلق برای امان از مشکلات مسیر
    window.location.href = `/html/${page}.html`;
}

/**
 * ریدایرکت با base path خاص
 */
function navigateTo(page, basePath = '/html/') {
    if (!page) {
        console.error('navigateTo: page parameter is required');
        return;
    }
    window.location.href = `${basePath}${page}.html`;
}

/**
 * بررسی احراز هویت و ریدایرکت اگر لاگین نشده
 */
function requireAuth() {
    if (!StorageManager || !StorageManager.isLoggedIn()) {
        window.location.href = '/html/login.html';
    }
}

/**
 * اگر لاگین شده باشد، به صفحه دیگری ریدایرکت کنید
 */
function redirectIfLoggedIn(page = 'home') {
    if (StorageManager && StorageManager.isLoggedIn()) {
        window.location.href = `/html/${page}.html`;
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

/**
 * تاخیر (setTimeout wrapper)
 */
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * تولید UUID ساده
 */
function generateId() {
    return 'id_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

/**
 * بررسی کنید که آیا رشته خالی است
 */
function isEmpty(str) {
    return !str || str.trim().length === 0;
}

/**
 * دریافت پارامتر از URL
 */
function getUrlParameter(name) {
    try {
        const params = new URLSearchParams(window.location.search);
        return params.get(name);
    } catch (error) {
        console.error('getUrlParameter error:', error);
        return null;
    }
}

/**
 * Clone یک object
 */
function cloneObject(obj) {
    try {
        return JSON.parse(JSON.stringify(obj));
    } catch (error) {
        console.error('cloneObject error:', error);
        return null;
    }
}

/**
 * بررسی اینترنت
 */
function isOnline() {
    return navigator.onLine;
}

/**
 * ✅ تابع جدید: بررسی تاریخ معتبر
 */
function isValidDate(dateString) {
    const date = new Date(dateString);
    return date instanceof Date && !isNaN(date);
}

/**
 * ✅ تابع جدید: تبدیل رشته به boolean
 */
function stringToBoolean(str) {
    return str === 'true' || str === '1' || str === true;
}
