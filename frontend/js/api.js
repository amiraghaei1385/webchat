/**
 * api.js
 * مدیریت تمام فراخوانی‌های API REST
 * 
 * استفاده:
 * - تمام درخواست‌های API از اینجا انجام دهید
 * - token خودکار از localStorage دریافت می‌شود
 * - Unauthorized (401) خودکار به login ریدایرکت می‌کند
 */

// ✅ تغییر به let تا بتوان تغییر داد
let API_BASE_URL = 'http://localhost:8080/api';

/**
 * دریافت token از localStorage
 */
function getAuthToken() {
    return localStorage.getItem('authToken');
}

/**
 * ایجاد header‌های استاندارد برای درخواست‌های API
 */
function getHeaders(includeAuth = true) {
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (includeAuth) {
        const token = getAuthToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
    }
    
    return headers;
}

/**
 * ارسال درخواست HTTP عمومی
 * @param {string} endpoint - آدرس endpoint
 * @param {string} method - GET, POST, PUT, DELETE
 * @param {object} body - بدی درخواست (برای POST/PUT)
 * @param {boolean} includeAuth - آیا token را شامل کنیم
 * @returns {Promise} پاسخ API
 */
async function makeRequest(endpoint, method = 'GET', body = null, includeAuth = true) {
    const options = {
        method,
        headers: getHeaders(includeAuth)
    };
    
    if (body && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(body);
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
        
        // ✅ بهتر کردن مدیریت خطای 401
        if (response.status === 401) {
            console.warn('Unauthorized - clearing session and redirecting to login');
            StorageManager.clear();
            window.location.href = '/html/login.html';
            return null;
        }
        
        // ✅ بررسی صحیح خطا با مدیریت JSON parsing
        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch (parseError) {
                errorData = { error: response.statusText || 'Unknown error' };
            }
            throw new Error(errorData.error || `HTTP ${response.status}: ${response.statusText}`);
        }
        
        // ✅ مدیریت بهتر پاسخ JSON
        try {
            return await response.json();
        } catch (parseError) {
            console.error('Failed to parse API response:', parseError);
            throw new Error('Invalid API response format');
        }
    } catch (error) {
        console.error(`API Error [${method} ${endpoint}]:`, error);
        throw error;
    }
}

// ============================================
// AUTHENTICATION APIs
// ============================================

/**
 * ورود به سیستم
 * @param {string} username - نام کاربری
 * @param {string} password - رمز عبور
 * @param {boolean} rememberMe - یادآوری ورود
 */
async function loginUser(username, password, rememberMe = false) {
    const response = await makeRequest('/auth/login', 'POST', {
        username,
        password,
        rememberMe
    }, false);
    
    if (response && response.token) {
        StorageManager.setToken(response.token);
        StorageManager.setUser(response.userId, username);
    }
    
    return response;
}

/**
 * ثبت‌نام کاربر جدید
 * @param {string} userId - شناسه منحصر به فرد
 * @param {string} username - نام نمایشی
 * @param {string} password - رمز عبور
 * @param {string} confirmPassword - تأیید رمز عبور
 */
async function registerUser(userId, username, password, confirmPassword) {
    const response = await makeRequest('/auth/register', 'POST', {
        userId,
        username,
        password,
        confirmPassword
    }, false);
    
    if (response && response.token) {
        StorageManager.setToken(response.token);
        StorageManager.setUser(response.userId, username);
    }
    
    return response;
}

/**
 * خروج از سیستم
 */
async function logoutUser() {
    try {
        await makeRequest('/auth/logout', 'POST');
    } catch (error) {
        console.error('Logout error:', error);
    } finally {
        StorageManager.clear();
        window.location.href = '/html/login.html';
    }
}

// ============================================
// USER APIs
// ============================================

/**
 * دریافت اطلاعات پروفایل کاربر
 * @param {string} userId - شناسه کاربر
 */
async function getUserProfile(userId) {
    return makeRequest(`/users/${userId}`);
}

// ============================================
// CHAT APIs
// ============================================

/**
 * دریافت لیست تمام چت‌های کاربر
 * مرتب شده بر اساس آخرین پیام
 */
async function getUserChats() {
    return makeRequest('/chats');
}

/**
 * ایجاد یا دریافت چت خصوصی با کاربر دیگر
 * @param {string} targetUserId - شناسه کاربری که می‌خواهیم با او چت کنیم
 */
async function getOrCreatePrivateChat(targetUserId) {
    return makeRequest('/chats/private', 'POST', { targetUserId });
}

/**
 * دریافت پیام‌های یک چت
 * @param {string} chatId - شناسه چت
 */
async function getChatMessages(chatId) {
    return makeRequest(`/chats/${chatId}/messages`);
}

/**
 * ارسال پیام جدید
 * @param {string} chatId - شناسه چت
 * @param {string} content - محتوای پیام
 */
async function sendMessage(chatId, content) {
    return makeRequest(`/chats/${chatId}/messages`, 'POST', { content });
}

/**
 * پین (ثابت) کردن چت در بالای لیست
 * @param {string} chatId - شناسه چت
 * @param {boolean} pinned - آیا pinned باشد
 */
async function setPinChat(chatId, pinned) {
    return makeRequest(`/chats/${chatId}/pin`, 'PUT', { pinned });
}

/**
 * آرشیو کردن چت (پنهان کردن از لیست معمول)
 * @param {string} chatId - شناسه چت
 * @param {boolean} archived - آیا archived باشد
 */
async function setArchiveChat(chatId, archived) {
    return makeRequest(`/chats/${chatId}/archive`, 'PUT', { archived });
}

// ============================================
// GROUP APIs
// ============================================

/**
 * ایجاد گروه جدید
 * @param {string} name - نام گروه
 */
async function createGroup(name) {
    return makeRequest('/groups', 'POST', { name });
}

/**
 * دریافت اطلاعات گروه و اعضای آن
 * @param {string} groupId - شناسه گروه
 */
async function getGroupInfo(groupId) {
    return makeRequest(`/groups/${groupId}`);
}

// ============================================
// CONTACT APIs
// ============================================

/**
 * اضافه کردن مخاطب جدید
 * @param {string} contactId - شناسه کاربری که می‌خواهیم به مخاطبین اضافه کنیم
 */
async function addContact(contactId) {
    return makeRequest('/contacts', 'POST', { contactId });
}

/**
 * دریافت لیست مخاطبین
 */
async function getContacts() {
    return makeRequest('/contacts');
}

// ============================================
// Helper Functions
// ============================================

/**
 * بررسی کنید که آیا کاربر احراز هویت شده است
 */
function isAuthenticated() {
    return !!getAuthToken();
}

/**
 * اگر کاربر احراز هویت نشده باشد، به login برود
 */
function requireAuth() {
    if (!isAuthenticated()) {
        window.location.href = '/html/login.html';
    }
}

/**
 * تنظیم Base URL (اگر سرور در محل دیگری باشد)
 * @param {string} url - آدرس جدید
 */
function setApiBaseUrl(url) {
    API_BASE_URL = url;
}
