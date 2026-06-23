/**
 * storage.js
 * مدیریت localStorage برای نگهداری اطلاعات کاربر
 */

class StorageManager {
    // Keys
    static KEY_TOKEN = 'authToken';
    static KEY_USER_ID = 'userId';
    static KEY_USERNAME = 'username';
    static KEY_THEME = 'theme';
    static KEY_REMEMBER_ME = 'rememberMe';
    
    /**
     * ذخیره token احراز هویت
     */
    static setToken(token) {
        if (!token) {
            localStorage.removeItem(this.KEY_TOKEN);
            return;
        }
        localStorage.setItem(this.KEY_TOKEN, token);
    }
    
    /**
     * دریافت token احراز هویت
     */
    static getToken() {
        return localStorage.getItem(this.KEY_TOKEN);
    }
    
    /**
     * ذخیره اطلاعات کاربر
     */
    static setUser(userId, username) {
        if (!userId || !username) {
            this.removeUser();
            return;
        }
        localStorage.setItem(this.KEY_USER_ID, userId);
        localStorage.setItem(this.KEY_USERNAME, username);
    }
    
    /**
     * دریافت اطلاعات کاربر فعلی
     */
    static getUser() {
        return {
            userId: localStorage.getItem(this.KEY_USER_ID),
            username: localStorage.getItem(this.KEY_USERNAME)
        };
    }
    
    /**
     * دریافت userId
     */
    static getUserId() {
        return localStorage.getItem(this.KEY_USER_ID);
    }
    
    /**
     * دریافت username
     */
    static getUsername() {
        return localStorage.getItem(this.KEY_USERNAME);
    }
    
    /**
     * حذف اطلاعات کاربر
     */
    static removeUser() {
        localStorage.removeItem(this.KEY_USER_ID);
        localStorage.removeItem(this.KEY_USERNAME);
    }
    
    /**
     * تغییر تم (روز/شب)
     */
    static setTheme(theme) {
        if (!theme) {
            localStorage.removeItem(this.KEY_THEME);
            return;
        }
        localStorage.setItem(this.KEY_THEME, theme);
        this.applyTheme(theme);
    }
    
    /**
     * دریافت تم فعلی
     */
    static getTheme() {
        return localStorage.getItem(this.KEY_THEME) || 'light';
    }
    
    /**
     * اعمال تم بر روی صفحه
     */
    static applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
    }
    
    /**
     * تنظیم یادآوری ورود
     */
    static setRememberMe(value) {
        localStorage.setItem(this.KEY_REMEMBER_ME, value ? 'true' : 'false');
    }
    
    /**
     * دریافت یادآوری ورود
     */
    static getRememberMe() {
        return localStorage.getItem(this.KEY_REMEMBER_ME) === 'true';
    }
    
    /**
     * بررسی کنید که آیا کاربر لاگین شده است
     */
    static isLoggedIn() {
        return !!localStorage.getItem(this.KEY_TOKEN);
    }
    
    /**
     * ✅ حذف فقط اطلاعات خود (نه تمام localStorage)
     * این برای امان از حذف داده‌های سایر برنامه‌ها است
     */
    static clear() {
        // فقط keys مربوط به این برنامه را حذف کنید
        localStorage.removeItem(this.KEY_TOKEN);
        localStorage.removeItem(this.KEY_USER_ID);
        localStorage.removeItem(this.KEY_USERNAME);
        localStorage.removeItem(this.KEY_THEME);
        localStorage.removeItem(this.KEY_REMEMBER_ME);
        
        console.log('Storage cleared successfully');
    }
    
    /**
     * ✅ نسخه خطرناک: حذف کل localStorage
     * فقط اگر واقعاً نیاز باشد استفاده کنید
     */
    static clearAll() {
        console.warn('Clearing entire localStorage - use with caution!');
        localStorage.clear();
    }
    
    /**
     * ذخیره داده دلخواه
     */
    static set(key, value) {
        if (!key) {
            console.warn('Storage.set: key cannot be empty');
            return;
        }
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.error('Storage.set error:', error);
        }
    }
    
    /**
     * دریافت داده دلخواه
     */
    static get(key) {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : null;
        } catch (error) {
            console.error('Storage.get error:', error);
            return null;
        }
    }
    
    /**
     * حذف داده دلخواه
     */
    static remove(key) {
        try {
            localStorage.removeItem(key);
        } catch (error) {
            console.error('Storage.remove error:', error);
        }
    }
    
    /**
     * دریافت تمام keys
     */
    static getAllKeys() {
        const keys = [];
        for (let i = 0; i < localStorage.length; i++) {
            keys.push(localStorage.key(i));
        }
        return keys;
    }
}

// اعمال تم ذخیره شده هنگام بارگذاری صفحه
document.addEventListener('DOMContentLoaded', () => {
    const theme = StorageManager.getTheme();
    StorageManager.applyTheme(theme);
});
