/**
 * pages/home.js
 * منطق صفحه اصلی - نمایش لیست چت‌ها
 */

// متغیرهای global
let allChats = [];
let filteredChats = [];

document.addEventListener('DOMContentLoaded', () => {
    // بررسی احراز هویت
    requireAuth();
    
    // بارگذاری چت‌ها
    loadChats();
    
    // تنظیم Event Listeners
    setupEventListeners();
});

/**
 * تنظیم Event Listeners
 */
function setupEventListeners() {
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keyup', handleSearch);
    }
}

/**
 * بارگذاری لیست چت‌ها از سرور
 */
async function loadChats() {
    const container = document.getElementById('chat-list-container');
    
    try {
        // نمایش loading
        container.innerHTML = '<div class="loading-spinner"><div class="spinner"></div></div>';
        
        // درخواست API
        const response = await getUserChats();
        
        if (!response || !Array.isArray(response)) {
            showEmptyState();
            return;
        }
        
        allChats = response;
        filteredChats = response;
        
        if (allChats.length === 0) {
            showEmptyState();
        } else {
            renderChats(allChats);
        }
    } catch (error) {
        console.error('خطا در بارگذاری چت‌ها:', error);
        showEmptyState('خطا در بارگذاری چت‌ها. لطفاً دوباره تلاش کنید.');
    }
}

/**
 * رندر کردن لیست چت‌ها
 */
function renderChats(chats) {
    const container = document.getElementById('chat-list-container');
    container.innerHTML = '';
    
    if (chats.length === 0) {
        showEmptyState('چت‌ای یافت نشد');
        return;
    }
    
    chats.forEach(chat => {
        const chatElement = createChatElement(chat);
        container.appendChild(chatElement);
    });
}

/**
 * ایجاد عنصر چت
 */
function createChatElement(chat) {
    const div = document.createElement('div');
    div.className = 'chat-item';
    
    // نام چت و آواتار
    const chatName = getChatName(chat);
    const avatar = getInitials(chatName);
    
    // زمان
    const time = formatDate(chat.lastMessageAt);
    
    // HTML
    div.innerHTML = `
        <div class="chat-avatar">${avatar}</div>
        <div class="chat-info">
            <div class="chat-header">
                <div class="chat-name">${escapeHtml(chatName)}</div>
                <div class="chat-time">${time}</div>
            </div>
            <div class="chat-preview">آخرین پیام...</div>
        </div>
        <div class="chat-actions">
            <button class="action-btn" title="سنجاق کن" onclick="togglePin('${chat.id}', ${chat.pinned || false})">📌</button>
            <button class="action-btn" title="آرشیو کن" onclick="toggleArchive('${chat.id}', ${chat.archived || false})">📁</button>
        </div>
    `;
    
    // کلیک برای باز کردن چت
    div.addEventListener('click', () => {
        window.location.href = `chat.html?chatId=${chat.id}`;
    });
    
    return div;
}

/**
 * دریافت نام چت (بر اساس نوع چت)
 */
function getChatName(chat) {
    // برای اکنون از ID استفاده می‌کنیم
    // در فاز 2 می‌توانیم اطلاعات بیشتری دریافت کنیم
    return chat.id || 'چت بدون نام';
}

/**
 * دریافت حروف اول برای آواتار
 */
function getInitials(name) {
    return name
        .split(' ')
        .map(word => word[0])
        .join('')
        .toUpperCase()
        .slice(0, 2) || '💬';
}

/**
 * جستجو در چت‌ها
 */
function handleSearch() {
    const searchInput = document.getElementById('search-input');
    const query = searchInput.value.toLowerCase().trim();
    
    if (!query) {
        filteredChats = allChats;
    } else {
        filteredChats = allChats.filter(chat => {
            const chatName = getChatName(chat).toLowerCase();
            return chatName.includes(query);
        });
    }
    
    renderChats(filteredChats);
}

/**
 * سنجاق/آنسنجاق کردن چت
 */
async function togglePin(chatId, currentPinned) {
    try {
        await setPinChat(chatId, !currentPinned);
        loadChats(); // دوباره بارگذاری
    } catch (error) {
        console.error('خطا در سنجاق کردن چت:', error);
        alert('خطا در سنجاق کردن چت');
    }
}

/**
 * آرشیو/خارج کردن از آرشیو
 */
async function toggleArchive(chatId, currentArchived) {
    try {
        await setArchiveChat(chatId, !currentArchived);
        loadChats(); // دوباره بارگذاری
    } catch (error) {
        console.error('خطا در آرشیو کردن چت:', error);
        alert('خطا در آرشیو کردن چت');
    }
}

/**
 * نمایش حالت خالی
 */
function showEmptyState(message = null) {
    const container = document.getElementById('chat-list-container');
    
    if (!message) {
        message = 'چت‌ای ندارید. یک چت جدید شروع کنید!';
    }
    
    container.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">💬</div>
            <h2>چت‌ی وجود ندارد</h2>
            <p>${message}</p>
            <button class="btn btn-primary" onclick="goToNewChat()" style="margin-top: 1rem;">
                شروع چت جدید
            </button>
        </div>
    `;
}

/**
 * رفتن به صفحه چت جدید
 */
function goToNewChat() {
    window.location.href = 'newChat.html';
}

/**
 * رفتن به صفحه تنظیمات
 */
function goToSettings() {
    window.location.href = 'settings.html';
}
