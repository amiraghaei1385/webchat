# 💬 Web Chat - Frontend Phase 1

راهنمای جامع برای پیاده‌سازی فرانت‌اند فاز اول پروژه Web Chat

---

## 📋 فهرست

1. [ساختار پروژه](#ساختار-پروژه)
2. [نحوه شروع](#نحوه-شروع)
3. [توضیح فایل‌ها](#توضیح-فایل‌ها)
4. [API Integration](#api-integration)
5. [صفحات مورد نیاز](#صفحات-مورد-نیاز)
6. [نکات مهم](#نکات-مهم)

---

## 📁 ساختار پروژه

```
webchat-frontend/
│
├── index.html                           # صفحه شروع (redirect کننده)
│
├── html/
│   ├── login.html                       # صفحه ورود
│   ├── signup.html                      # صفحه ثبت‌نام
│   ├── home.html                        # صفحه اصلی
│   ├── chat.html                        # صفحه چت
│   ├── settings.html                    # صفحه تنظیمات
│   └── newChat.html                     # صفحه ایجاد گفتوگو
│
├── css/
│   ├── style.css                        # استایل‌های کلی و پایه
│   ├── login.css                        # استایل‌های مختص صفحه لاگین
│   ├── home.css                         # استایل‌های مختص صفحه اصلی
│   ├── chat.css                         # استایل‌های مختص صفحه چت
│   └── responsive.css                   # responsive design
│
└── js/
    ├── storage.js                       # مدیریت localStorage
    ├── api.js                           # تمام API calls
    ├── utils.js                         # توابع کمکی
    │
    └── pages/
        ├── login.js                     # منطق صفحه لاگین
        ├── signup.js                    # منطق صفحه ثبت‌نام
        ├── home.js                      # منطق صفحه اصلی
        ├── chat.js                      # منطق صفحه چت
        ├── settings.js                  # منطق صفحه تنظیمات
        └── newChat.js                   # منطق صفحه ایجاد گفتوگو
```

---

## 🚀 نحوه شروع

### 1. تنظیم سرور
سرور جاوا باید در `http://localhost:8080` اجرا شود. اگر سرور در جای دیگری است، `API_BASE_URL` در `api.js` را تغییر دهید:

```javascript
const API_BASE_URL = 'http://your-server:port/api';
```

### 2. باز کردن پروژه
یک HTTP Server اجرا کنید (نه فایل‌های محلی):

```bash
# اگر Python دارید
python -m http.server 8000

# یا با Node.js
npx http-server

# یا با Live Server در VS Code
```

سپس مروگر را به `http://localhost:8000/index.html` بروید.

### 3. ساختار تکمیل کنید
- فایل‌های HTML را یک‌یک اکمال کنید
- CSS را برای هر صفحه اضافه کنید
- منطق JS را پیاده‌سازی کنید

---

## 📄 توضیح فایل‌ها

### `storage.js`
مدیریت `localStorage` برای نگهداری اطلاعات کاربر.

**متدهای مهم:**
- `setToken(token)` - ذخیره token احراز هویت
- `setUser(userId, username)` - ذخیره اطلاعات کاربر
- `getToken()` - دریافت token
- `isLoggedIn()` - بررسی ورود
- `clear()` - پاک کردن تمام اطلاعات

### `api.js`
تمام فراخوانی‌های API REST.

**مثال استفاده:**
```javascript
// ورود
const response = await loginUser('username', 'password', false);

// دریافت چت‌ها
const chats = await getUserChats();

// ارسال پیام
const message = await sendMessage('chatId', 'سلام!');
```

### `utils.js`
توابع کمکی برای validation، formatting و DOM manipulation.

**مثال استفاده:**
```javascript
// Validation
const passResult = validatePassword('MyPass123!');
if (!passResult.valid) {
    console.log(passResult.errors);
}

// Formatting
const formatted = formatDate('2024-06-23T10:30:00');

// Navigation
navigate('home');
```

---

## 🔌 API Integration

### Authentication
```javascript
// ورود
await loginUser(username, password, rememberMe);
// Response: { token: "...", userId: "..." }

// ثبت‌نام
await registerUser(userId, username, password, confirmPassword);
// Response: { token: "...", userId: "..." }

// خروج
await logoutUser();
```

### Chats
```javascript
// دریافت لیست چت‌ها
const chats = await getUserChats();

// ایجاد یا دریافت چت خصوصی
const chat = await getOrCreatePrivateChat(targetUserId);

// دریافت پیام‌ها
const messages = await getChatMessages(chatId);

// ارسال پیام
const message = await sendMessage(chatId, 'متن پیام');
```

### Error Handling
```javascript
try {
    const response = await getUserChats();
} catch (error) {
    if (error.message.includes('401')) {
        // بدون احراز هویت
    } else {
        showError(error.message);
    }
}
```

---

## 📋 صفحات مورد نیاز فاز اول

### 1️⃣ صفحه Login (`html/login.html`)

**اجزاء:**
- فیلد نام کاربری
- فیلد رمز عبور
- checkbox "مرا یادآوری کن"
- دکمه "ورود"
- لینک "ثبت‌نام کنید"

**Functionality:**
- Validation
- فراخوانی `loginUser()` API
- ذخیره token در localStorage
- Redirect به صفحه اصلی
- نمایش پیام خطا

**مثال:**
```javascript
async function handleLogin(e) {
    e.preventDefault();
    const username = getInputValue('username');
    const password = getInputValue('password');
    
    try {
        const response = await loginUser(username, password);
        if (response.token) {
            window.location.href = 'home.html';
        }
    } catch (error) {
        showError(error.message);
    }
}
```

### 2️⃣ صفحه Sign Up (`html/signup.html`)

**اجزاء:**
- فیلد نام کاربری
- فیلد آیدی (منحصر به فرد)
- فیلد رمز عبور
- فیلد تأیید رمز عبور
- دکمه "ثبت‌نام"

**Validation:**
- نام کاربری: حداقل 3 کاراکتر
- آیدی: منحصر به فرد، حداقل 3 کاراکتر
- رمز عبور:
  - حداقل 8 کاراکتر
  - شامل حرف بزرگ
  - شامل حرف کوچک
  - شامل عدد
  - شامل کاراکتر خاص (!@%$#^&*)
- تطابق رمز‌ها

**مثال:**
```javascript
const passValidation = validatePassword(password);
if (!passValidation.valid) {
    showError(`رمز عبور باید شامل: ${passValidation.errors.join(', ')}`);
}
```

### 3️⃣ صفحه Home (`html/home.html`)

**بخش‌ها:**
1. **Sidebar (منوی جانبی)**
   - لوگو/عنوان
   - دکمه تنظیمات
   - دکمه + (ایجاد گفتوگو)

2. **Main Content**
   - نوار جستجو
   - لیست چت‌ها:
     - نام کاربر/گروه
     - عکس پروفایل
     - تعداد پیام جدید
     - آخرین پیام
     - زمان آخرین پیام

**Functionality:**
```javascript
// دریافت چت‌ها
async function loadChats() {
    const chats = await getUserChats();
    renderChats(chats);
}

// Pin/Unpin
async function togglePin(chatId, pinned) {
    await setPinChat(chatId, !pinned);
    loadChats();
}

// Archive
async function toggleArchive(chatId, archived) {
    await setArchiveChat(chatId, !archived);
    loadChats();
}
```

### 4️⃣ صفحه Chat (`html/chat.html`)

**بالای صفحه:**
- دکمه بازگشت
- نام کاربر/گروه
- عکس پروفایل
- وضعیت (آنلاین/آخرین دیده شدن)

**بخش پیام‌ها:**
- لیست پیام‌ها مرتب شده
- نام فرستنده
- محتوای پیام
- زمان ارسال

**پایین صفحه:**
- فیلد ورودی پیام
- دکمه ارسال
- دکمه آپلود فایل (فاز 2)

**Functionality:**
```javascript
async function loadMessages(chatId) {
    const messages = await getChatMessages(chatId);
    renderMessages(messages);
}

async function sendNewMessage(chatId, content) {
    const message = await sendMessage(chatId, content);
    // اضافه کردن به صفحه
}
```

### 5️⃣ صفحه Settings (`html/settings.html`)

**گزینه‌ها:**
- تغییر عکس پروفایل
- تغییر نام کاربری
- تغییر آیدی
- تغییر تم (روز/شب)
- خروج
- حذف حساب (اختیاری)

**مثال:**
```javascript
function handleLogout() {
    logoutUser();
    // خودکار ریدایرکت می‌شود
}

function toggleTheme() {
    const theme = StorageManager.getTheme() === 'light' ? 'dark' : 'light';
    StorageManager.setTheme(theme);
}
```

### 6️⃣ صفحه New Chat (`html/newChat.html`)

**دو بخش:**
1. **ایجاد گروه**
   - فیلد نام گروه
   - دکمه ایجاد

2. **شروع چت**
   - جستجو یا انتخاب از مخاطبین
   - دکمه شروع چت

**Functionality:**
```javascript
// ایجاد گروه
async function createNewGroup(name) {
    const group = await createGroup(name);
    // ریدایرکت به چت گروه
}

// شروع چت
async function startPrivateChat(targetUserId) {
    const chat = await getOrCreatePrivateChat(targetUserId);
    window.location.href = `chat.html?chatId=${chat.id}`;
}
```

---

## ⚠️ نکات مهم

### Security
- ✅ Token هرگز در URL نشود
- ✅ Token هرگز در HTML نشود
- ✅ رمز عبور هرگز در localStorage نشود
- ✅ XSS Prevention - از `escapeHtml()` استفاده کنید
- ✅ HTTPS استفاده کنید (در production)

### Browser Storage
```javascript
// ✅ درست
localStorage.setItem('authToken', token);

// ❌ غلط
localStorage.setItem('password', password);
localStorage.setItem('user', JSON.stringify({ token, password }));
```

### Error Handling
```javascript
try {
    await loginUser(username, password);
} catch (error) {
    if (error.status === 401) {
        // Unauthorized
    } else if (error.status === 400) {
        // Bad Request
    } else {
        showError('خطای سرور');
    }
}
```

### Performance
- ✅ استفاده از `async/await`
- ✅ Loading indicators نمایش دهید
- ✅ Error handling
- ✅ Lazy loading برای لیست طولانی

### Responsive Design
- ✅ Mobile-first approach
- ✅ Media queries در CSS
- ✅ Viewport meta tag

---

## 📝 چک‌لیست پیاده‌سازی فاز اول

- [ ] صفحه Login - تکمیل
- [ ] صفحه Sign Up - تکمیل
- [ ] صفحه Home - تکمیل
- [ ] صفحه Chat - تکمیل
- [ ] صفحه Settings - تکمیل
- [ ] صفحه New Chat - تکمیل
- [ ] Routing صحیح بین صفحات
- [ ] Authorization Checks
- [ ] Error Handling
- [ ] Loading Indicators
- [ ] Responsive Design
- [ ] CSS Styling کامل
- [ ] Test در Browsers مختلف

---

## 🆘 عیب‌یابی

### مشکل: CORS Error
**راه‌حل:** سرور باید CORS headers صحیح بفرستد

### مشکل: Token Expired
**راه‌حل:** ریدایرکت خودکار به login (در `api.js`)

### مشکل: 404 Not Found
**راه‌حل:** بررسی کنید URL endpoint صحیح است

### مشکل: localStorage خالی است
**راه‌حل:** استفاده از DevTools > Application > Local Storage

---

## 📞 تماس و کمک

برای سؤالات یا مشکلات:
1. چک کنید browser console را (F12)
2. Network tab را برای مشاهده API calls استفاده کنید
3. localStorage را بررسی کنید
4. دوباره سعی کنید

---

**موفق باشید! 🎉**
