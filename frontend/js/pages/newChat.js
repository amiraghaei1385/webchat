/**
 * pages/newChat.js
 * منطق صفحه ایجاد گفتوگو جدید
 */

let allContacts = [];
let filteredContacts = [];

document.addEventListener('DOMContentLoaded', () => {
    // بررسی احراز هویت
    requireAuth();
    
    // بارگذاری مخاطبین
    loadContacts();
});

/**
 * بارگذاری لیست مخاطبین
 */
async function loadContacts() {
    const container = document.getElementById('contacts-list');
    
    try {
        const response = await getContacts();
        
        if (!response || !Array.isArray(response)) {
            showEmptyContacts();
            return;
        }
        
        allContacts = response;
        filteredContacts = response;
        
        if (allContacts.length === 0) {
            showEmptyContacts();
        } else {
            renderContacts(allContacts);
        }
    } catch (error) {
        console.error('خطا در بارگذاری مخاطبین:', error);
        container.innerHTML = '<div class="empty-contacts">خطا در بارگذاری مخاطبین</div>';
    }
}

/**
 * رندر کردن لیست مخاطبین
 */
function renderContacts(contacts) {
    const container = document.getElementById('contacts-list');
    container.innerHTML = '';
    
    if (contacts.length === 0) {
        showEmptyContacts();
        return;
    }
    
    contacts.forEach(contact => {
        const contactElement = createContactElement(contact);
        container.appendChild(contactElement);
    });
}

/**
 * ایجاد عنصر مخاطب
 */
function createContactElement(contact) {
    const div = document.createElement('div');
    div.className = 'contact-item';
    
    const initials = getInitials(contact.contactId);
    
    div.innerHTML = `
        <div class="contact-avatar">${initials}</div>
        <div class="contact-info">
            <div class="contact-name">${escapeHtml(contact.contactId)}</div>
            <div class="contact-id">@${escapeHtml(contact.contactId)}</div>
        </div>
    `;
    
    div.addEventListener('click', () => {
        startChatWithContact(contact.contactId);
    });
    
    return div;
}

/**
 * دریافت حروف اول
 */
function getInitials(name) {
    return name
        .split(' ')
        .map(word => word[0])
        .join('')
        .toUpperCase()
        .slice(0, 2) || '👤';
}

/**
 * جستجو در مخاطبین
 */
function handleContactSearch() {
    const searchInput = document.getElementById('user-id');
    const query = searchInput.value.toLowerCase().trim();
    
    if (!query) {
        filteredContacts = allContacts;
    } else {
        filteredContacts = allContacts.filter(contact => {
            return contact.contactId.toLowerCase().includes(query);
        });
    }
    
    renderContacts(filteredContacts);
}

/**
 * ایجاد گروه جدید
 */
async function createNewGroup() {
    const groupNameInput = document.getElementById('group-name');
    const groupName = groupNameInput.value.trim();
    
    // Validation
    if (!groupName) {
        alert('لطفاً نام گروه را وارد کنید');
        return;
    }
    
    if (groupName.length < 3) {
        alert('نام گروه حداقل 3 کاراکتر باید باشد');
        return;
    }
    
    const btn = document.getElementById('create-group-btn');
    btn.disabled = true;
    btn.textContent = 'در حال ایجاد...';
    
    try {
        const group = await createGroup(groupName);
        
        if (group && group.chatId) {
            // ریدایرکت به صفحه چت گروه جدید
            window.location.href = `chat.html?chatId=${group.chatId}`;
        } else {
            alert('خطا در ایجاد گروه');
        }
    } catch (error) {
        console.error('خطا در ایجاد گروه:', error);
        alert('خطا در ایجاد گروه. لطفاً دوباره تلاش کنید.');
    } finally {
        btn.disabled = false;
        btn.textContent = 'ایجاد';
    }
}

/**
 * شروع چت جدید با کاربر
 */
async function startNewChat() {
    const userIdInput = document.getElementById('user-id');
    const userId = userIdInput.value.trim();
    
    // Validation
    if (!userId) {
        alert('لطفاً آیدی کاربر یا نام را وارد کنید');
        return;
    }
    
    if (userId.length < 3) {
        alert('آیدی کاربر حداقل 3 کاراکتر باید باشد');
        return;
    }
    
    const btn = document.getElementById('start-chat-btn');
    btn.disabled = true;
    btn.textContent = 'در حال بارگذاری...';
    
    try {
        const chat = await getOrCreatePrivateChat(userId);
        
        if (chat && chat.id) {
            // ریدایرکت به صفحه چت
            window.location.href = `chat.html?chatId=${chat.id}`;
        } else {
            alert('خطا در شروع چت');
        }
    } catch (error) {
        console.error('خطا در شروع چت:', error);
        alert('خطا در شروع چت. لطفاً دوباره تلاش کنید.');
    } finally {
        btn.disabled = false;
        btn.textContent = 'شروع';
    }
}

/**
 * شروع چت با مخاطب از لیست
 */
async function startChatWithContact(contactId) {
    const btn = event.target.closest('.contact-item');
    btn.style.opacity = '0.6';
    btn.style.pointerEvents = 'none';
    
    try {
        const chat = await getOrCreatePrivateChat(contactId);
        
        if (chat && chat.id) {
            window.location.href = `chat.html?chatId=${chat.id}`;
        } else {
            alert('خطا در شروع چت');
            btn.style.opacity = '1';
            btn.style.pointerEvents = 'auto';
        }
    } catch (error) {
        console.error('خطا:', error);
        alert('خطا در شروع چت');
        btn.style.opacity = '1';
        btn.style.pointerEvents = 'auto';
    }
}

/**
 * نمایش حالت خالی
 */
function showEmptyContacts() {
    const container = document.getElementById('contacts-list');
    container.innerHTML = `
        <div class="empty-contacts">
            <p>هنوز مخاطبین ندارید</p>
            <p style="font-size: 0.85em;">
                با وارد کردن آیدی کاربر می‌توانید چت جدید شروع کنید
            </p>
        </div>
    `;
}

/**
 * برگشتن به صفحه اصلی
 */
function goBack() {
    window.location.href = 'home.html';
}
