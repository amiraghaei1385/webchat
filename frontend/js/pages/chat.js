/**
 * pages/chat.js
 * منطق صفحه چت - نمایش و ارسال پیام
 */

let currentChatId = null;
let currentUserId = null;
let allMessages = [];

document.addEventListener('DOMContentLoaded', () => {
    // بررسی احراز هویت
    requireAuth();
    
    // دریافت chatId از URL
    currentChatId = getUrlParameter('chatId');
    currentUserId = StorageManager.getUserId();
    
    if (!currentChatId) {
        alert('چت‌ای انتخاب نشده است');
        window.location.href = 'home.html';
        return;
    }
    
    // بارگذاری پیام‌ها
    loadMessages();
    
    // تنظیم Event Listeners
    setupEventListeners();
    
    // Refresh پیام‌ها هر 3 ثانیه
    setInterval(loadMessages, 3000);
});

/**
 * تنظیم Event Listeners
 */
function setupEventListeners() {
    const input = document.getElementById('message-input');
    if (input) {
        input.addEventListener('keypress', handleKeyPress);
    }
}

/**
 * بارگذاری پیام‌های چت
 */
async function loadMessages() {
    try {
        const container = document.getElementById('messages-container');
        
        // اگر این اولین بار است، نمایش spinner
        if (allMessages.length === 0) {
            container.innerHTML = '<div class="loading-messages"><div class="spinner"></div></div>';
        }
        
        // درخواست API
        const messages = await getChatMessages(currentChatId);
        
        if (!Array.isArray(messages)) {
            showEmptyChat();
            return;
        }
        
        allMessages = messages;
        
        if (messages.length === 0) {
            showEmptyChat();
        } else {
            renderMessages(messages);
        }
    } catch (error) {
        console.error('خطا در بارگذاری پیام‌ها:', error);
        const container = document.getElementById('messages-container');
        container.innerHTML = '<div class="empty-chat"><p>خطا در بارگذاری پیام‌ها</p></div>';
    }
}

/**
 * رندر کردن پیام‌ها
 */
function renderMessages(messages) {
    const container = document.getElementById('messages-container');
    container.innerHTML = '';
    
    messages.forEach(msg => {
        const msgElement = createMessageElement(msg);
        container.appendChild(msgElement);
    });
    
    // اسکرول به آخر
    scrollToBottom();
}

/**
 * ایجاد عنصر پیام
 */
function createMessageElement(message) {
    const div = document.createElement('div');
    const isSent = message.senderId === currentUserId;
    
    div.className = `message ${isSent ? 'sent' : 'received'}`;
    
    const time = formatDate(message.sentAt);
    
    div.innerHTML = `
        <div>
            <div class="message-bubble">${escapeHtml(message.content)}</div>
            <div class="message-time">${time}</div>
        </div>
    `;
    
    return div;
}

/**
 * مدیریت Enter برای ارسال پیام
 */
function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendNewMessage();
    }
}

/**
 * ارسال پیام جدید
 */
async function sendNewMessage() {
    const input = document.getElementById('message-input');
    const content = input.value.trim();
    
    // Validation
    if (!content) {
        return;
    }
    
    if (content.length > 5000) {
        alert('پیام نمی‌تواند بیش از 5000 کاراکتر باشد');
        return;
    }
    
    // غیرفعال کردن دکمه و input
    disableMessageInput(true);
    
    try {
        // ارسال پیام
        await sendMessage(currentChatId, content);
        
        // پاک کردن input
        input.value = '';
        
        // بارگذاری مجدد پیام‌ها
        loadMessages();
    } catch (error) {
        console.error('خطا در ارسال پیام:', error);
        alert('خطا در ارسال پیام. لطفاً دوباره تلاش کنید.');
    } finally {
        // فعال کردن دکمه و input
        disableMessageInput(false);
        document.getElementById('message-input').focus();
    }
}

/**
 * فعال/غیرفعال کردن input
 */
function disableMessageInput(disable) {
    const input = document.getElementById('message-input');
    const btn = document.getElementById('send-btn');
    
    input.disabled = disable;
    btn.disabled = disable;
}

/**
 * اسکرول به آخر
 */
function scrollToBottom() {
    const container = document.getElementById('messages-container');
    container.scrollTop = container.scrollHeight;
}

/**
 * نمایش حالت چت خالی
 */
function showEmptyChat() {
    const container = document.getElementById('messages-container');
    container.innerHTML = `
        <div class="empty-chat">
            <div style="font-size: 3em; margin-bottom: 1rem;">💬</div>
            <h2>هنوز پیام‌ی نیست</h2>
            <p>یک پیام ارسال کنید تا گفتوگو شروع شود</p>
        </div>
    `;
}

/**
 * برگشتن به صفحه اصلی
 */
function goBack() {
    window.location.href = 'home.html';
}
