// لایه ارتباط با بک‌اند (Java REST API روی http://localhost:8080/api)
// این فایل فقط با جاوااسکریپت خالص نوشته شده و از fetch استفاده می‌کند.

const Api = (() => {

  // --------------------------------------------------------------
  // هسته‌ی مشترک درخواست‌ها
  // --------------------------------------------------------------

  async function request(path, { method = 'GET', body = null, auth = true } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth) {
      const token = StorageManager.getToken();
      if (token) headers['Authorization'] = token; // بک‌اند از هدر Authorization خام (بدون Bearer) استفاده می‌کند
    }

    let res;
    try {
      res = await fetch(`${CONFIG.API_BASE_URL}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch (networkErr) {
      throw new ApiError('NETWORK_ERROR', 'Could not reach the server.', 0);
    }

    // برخی پاسخ‌های موفق ممکن است بدنه نداشته باشند
    let data = {};
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch (_) { data = {}; }
    }

    if (!res.ok) {
      throw new ApiError('HTTP_ERROR', data.error || 'Request failed.', res.status);
    }
    return data;
  }

  class ApiError extends Error {
    constructor(kind, message, status) {
      super(message);
      this.kind = kind;
      this.status = status;
    }
  }

  // --------------------------------------------------------------
  // Auth  (/api/auth/*)
  // --------------------------------------------------------------
  const Auth = {
    register: (userId, username, password, confirmPassword) =>
      request('/auth/register', {
        method: 'POST', auth: false,
        body: { userId, username, password, confirmPassword },
      }),

    login: (username, password, rememberMe) =>
      request('/auth/login', {
        method: 'POST', auth: false,
        body: { username, password, rememberMe },
      }),

    logout: () => request('/auth/logout', { method: 'POST' }),

    forgotPassword: (username) =>
      request('/auth/forgot-password', {
        method: 'POST', auth: false,
        body: { username },
      }),

    resetPassword: (token, tempPassword, newPassword, confirmNewPassword) =>
      request('/auth/reset-password', {
        method: 'POST', auth: false,
        body: { token, tempPassword, newPassword, confirmNewPassword },
      }),
  };

  // --------------------------------------------------------------
  // Chats  (/api/chats/*)
  // --------------------------------------------------------------
  const Chats = {
    // لیست چت‌های کاربر
    list: () => request('/chats'),

    // ساخت یا گرفتن چت خصوصی با یک کاربر دیگر
    getOrCreatePrivate: (targetUserId) =>
      request('/chats/private', { method: 'POST', body: { targetUserId } }),

    // خوانده‌شده کردن یک چت
    markAsRead: (chatId) =>
      request(`/chats/${encodeURIComponent(chatId)}/read`, { method: 'POST' }),

    // پین یا برداشتن پین
    setPinned: (chatId, pinned) =>
      request(`/chats/${encodeURIComponent(chatId)}/pin`, {
        method: 'PUT', body: { pinned },
      }),

    // آرشیو یا خروج از آرشیو
    setArchived: (chatId, archived) =>
      request(`/chats/${encodeURIComponent(chatId)}/archive`, {
        method: 'PUT', body: { archived },
      }),

    // بی‌صدا کردن یا خروج از بی‌صدا
    setMuted: (chatId, muted) =>
      request(`/chats/${encodeURIComponent(chatId)}/mute`, {
        method: 'PUT', body: { muted },
      }),

    // پیام‌های یک چت
    getMessages: (chatId) => request(`/chats/${encodeURIComponent(chatId)}/messages`),

    // فرستادن پیام جدید
    sendMessage: (chatId, content) =>
      request(`/chats/${encodeURIComponent(chatId)}/messages`, {
        method: 'POST', body: { content },
      }),

    // ویرایش یک پیام
    editMessage: (chatId, messageId, content) =>
      request(`/chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}`, {
        method: 'PUT', body: { content },
      }),

    // حذف یک پیام
    deleteMessage: (chatId, messageId) =>
      request(`/chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}`, {
        method: 'DELETE',
      }),

    // گزارش کردن یک پیام
    reportMessage: (chatId, messageId, reason) =>
      request(`/chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}/report`, {
        method: 'POST', body: { reason },
      }),

    // تاریخچه‌ی ویرایش/حذف یک پیام
    getMessageHistory: (chatId, messageId) =>
      request(`/chats/${encodeURIComponent(chatId)}/messages/${encodeURIComponent(messageId)}/history`),

    // تاریخچه‌ی کامل یک چت (همه‌ی پیام‌های ویرایش‌شده و حذف‌شده)
    getChatHistory: (chatId) =>
      request(`/chats/${encodeURIComponent(chatId)}/history`),
  };

  // --------------------------------------------------------------
  // Groups  (/api/groups/*)
  // --------------------------------------------------------------
  const Groups = {
    create: (name, groupId) => request('/groups', { method: 'POST', body: { name, groupId } }),

    get: (groupId) => request(`/groups/${encodeURIComponent(groupId)}`),

    // یافتن گروه بر اساس آیدی چت؛ برای اعضایی که خودشان گروه را نساخته‌اند
    getByChatId: (chatId) => request(`/groups/by-chat/${encodeURIComponent(chatId)}`),

    edit: (groupId, name, description) =>
      request(`/groups/${encodeURIComponent(groupId)}`, {
        method: 'PUT', body: { name, description },
      }),

    addMember: (groupId, userId) =>
      request(`/groups/${encodeURIComponent(groupId)}/members`, {
        method: 'POST', body: { userId },
      }),

    removeMember: (groupId, userId) =>
      request(`/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(userId)}`, {
        method: 'DELETE',
      }),

    leave: (groupId) =>
      request(`/groups/${encodeURIComponent(groupId)}/leave`, { method: 'POST' }),

    // آپلود مستقیم عکس گروه (fileBase64 بدون پیشوند data:...)؛ همه‌ی اعضا اجازه دارند
    uploadPicture: (groupId, fileBase64, originalFileName) =>
      request(`/groups/${encodeURIComponent(groupId)}/picture/upload`, {
        method: 'POST',
        body: { fileBase64, originalFileName },
      }),

    // دانلود عکس گروه به‌صورت blob URL (همانند getAvatarBlobUrl کاربر، چون
    // endpoint نیاز به هدر Authorization دارد و تگ <img> نمی‌تواند آن را بفرستد)
    getAvatarBlobUrl: async (groupId) => {
      const token = StorageManager.getToken();
      const headers = {};
      if (token) headers['Authorization'] = token;
      const res = await fetch(`${CONFIG.API_BASE_URL}/avatars/group/${encodeURIComponent(groupId)}`, { headers });
      if (!res.ok) return null;
      const blob = await res.blob();
      return URL.createObjectURL(blob);
    },

    // گروه‌های مشترک بین کاربر فعلی و یک کاربر دیگر
    getMutual: (peerUserId) =>
      request(`/groups/mutual/${encodeURIComponent(peerUserId)}`),
  };

  // --------------------------------------------------------------
  // Contacts  (/api/contacts/*)
  // --------------------------------------------------------------
  const Contacts = {
    add: (contactId) => request('/contacts', { method: 'POST', body: { contactId } }),

    list: () => request('/contacts'),

    // لیست کامل کاربران بلاک‌شده
    listBlocked: () => request('/contacts/blocked'),

    remove: (contactId) =>
      request(`/contacts/${encodeURIComponent(contactId)}`, { method: 'DELETE' }),

    block: (contactId) =>
      request(`/contacts/${encodeURIComponent(contactId)}/block`, { method: 'POST' }),

    unblock: (contactId) =>
      request(`/contacts/${encodeURIComponent(contactId)}/block`, { method: 'DELETE' }),

    isBlocked: (contactId) =>
      request(`/contacts/${encodeURIComponent(contactId)}/blocked`),
  };

  // --------------------------------------------------------------
  // Users  (/api/users/*)
  // --------------------------------------------------------------
  const Users = {
    getProfile: (userId) => request(`/users/${encodeURIComponent(userId)}`),

    updateUsername: (username) =>
      request('/users/me/username', { method: 'PUT', body: { username } }),

    updateUserId: (userId) =>
      request('/users/me/id', { method: 'PUT', body: { userId } }),

    updateEmail: (email) =>
      request('/users/me/email', { method: 'PUT', body: { email } }),

    removeEmail: () => request('/users/me/email', { method: 'DELETE' }),

    updateBio: (bio) =>
      request('/users/me/bio', { method: 'PUT', body: { bio } }),

    updateBirthDate: (birthDate) =>
      request('/users/me/birth-date', { method: 'PUT', body: { birthDate } }),

    removeBirthDate: () => request('/users/me/birth-date', { method: 'DELETE' }),

    // picturePath باید مسیر فایلی باشد که قبلاً از طریق Media آپلود شده است
    updateProfilePicture: (picturePath) =>
      request('/users/me/profile-picture', { method: 'PUT', body: { picturePath } }),

    // آپلود مستقیم فایل عکس پروفایل (fileBase64 بدون پیشوند data:...)
    uploadProfilePicture: (fileBase64, originalFileName) =>
      request('/users/me/profile-picture/upload', {
        method: 'POST',
        body: { fileBase64, originalFileName },
      }),

    removeProfilePicture: () =>
      request('/users/me/profile-picture', { method: 'DELETE' }),

    updateBackgroundPicture: (picturePath) =>
      request('/users/me/background-picture', { method: 'PUT', body: { picturePath } }),

    removeBackgroundPicture: () =>
      request('/users/me/background-picture', { method: 'DELETE' }),

    // دانلود عکس پروفایل به‌صورت blob URL (چون endpoint نیاز به هدر Authorization دارد
    // و تگ <img> نمی‌تواند هدر سفارشی بفرستد، این تابع fetch را خودمان انجام می‌دهیم)
    getAvatarBlobUrl: async (userId) => {
      const token = StorageManager.getToken();
      const headers = {};
      if (token) headers['Authorization'] = token;
      const res = await fetch(`${CONFIG.API_BASE_URL}/avatars/${encodeURIComponent(userId)}`, { headers });
      if (!res.ok) return null;
      const blob = await res.blob();
      return URL.createObjectURL(blob);
    },

    deleteAccount: () => request('/users/me', { method: 'DELETE' }),
  };

  // --------------------------------------------------------------
  // Settings  (/api/settings/*)
  // --------------------------------------------------------------
  const Settings = {
    get: () => request('/settings'),

    // اگر darkMode مشخص نشود، بک‌اند خودش toggle می‌کند
    setDarkMode: (darkMode) =>
      request('/settings/dark-mode', {
        method: 'PUT',
        body: darkMode === undefined ? {} : { darkMode },
      }),
  };

  // --------------------------------------------------------------
  // Reactions  (/api/messages/{id}/reactions*)
  // --------------------------------------------------------------
  const Reactions = {
    // افزودن/تغییر/حذف (toggle) یک ری‌اکشن روی پیام
    toggle: (messageId, emoji) =>
      request(`/messages/${encodeURIComponent(messageId)}/reactions`, {
        method: 'POST', body: { emoji },
      }),

    // حذف صریح ری‌اکشن کاربر فعلی از یک پیام
    remove: (messageId) =>
      request(`/messages/${encodeURIComponent(messageId)}/reactions`, {
        method: 'DELETE',
      }),

    // تمام ری‌اکشن‌های خام یک پیام
    getAll: (messageId) =>
      request(`/messages/${encodeURIComponent(messageId)}/reactions`),

    // خلاصه‌ی گروه‌بندی‌شده (emoji -> count)
    getSummary: (messageId) =>
      request(`/messages/${encodeURIComponent(messageId)}/reactions/summary`),
  };

  // --------------------------------------------------------------
  // Media  (/api/media/*)
  // --------------------------------------------------------------
  // mediaType یکی از: IMAGE, VIDEO, AUDIO, VOICE, DOCUMENT, STICKER
  const Media = {
    // fileBase64 باید محتوای فایل به‌صورت Base64 (بدون پیشوند data:...) باشد
    upload: (chatId, { fileBase64, originalFileName, mimeType, mediaType, caption }) =>
      request(`/media/${encodeURIComponent(chatId)}`, {
        method: 'POST',
        body: { fileBase64, originalFileName, mimeType, mediaType, caption },
      }),

    // دانلود فایل رسانه (پاسخ شامل fileBase64 است)
    download: (chatId, messageId) =>
      request(`/media/${encodeURIComponent(chatId)}/${encodeURIComponent(messageId)}`),

    editCaption: (chatId, messageId, caption) =>
      request(`/media/${encodeURIComponent(chatId)}/${encodeURIComponent(messageId)}`, {
        method: 'PUT', body: { caption },
      }),

    delete: (chatId, messageId) =>
      request(`/media/${encodeURIComponent(chatId)}/${encodeURIComponent(messageId)}`, {
        method: 'DELETE',
      }),

    // فقط متادیتا، بدون بایت‌های فایل
    getInfo: (chatId, messageId) =>
      request(`/media/${encodeURIComponent(chatId)}/${encodeURIComponent(messageId)}/info`),
  };

  // --------------------------------------------------------------
  // Search  (/api/search/*)
  // --------------------------------------------------------------
  const Search = {
    chats: (query) =>
      request(`/search/chats?q=${encodeURIComponent(query)}`),

    messagesInChat: (chatId, query) =>
      request(`/search/messages/${encodeURIComponent(chatId)}?q=${encodeURIComponent(query)}`),
  };

  // --------------------------------------------------------------
  // Stories  (/api/stories/*)
  // --------------------------------------------------------------
  // mediaType یکی از: IMAGE, VIDEO, TEXT
  const Stories = {
    // فید استوری‌های صفحه‌ی هوم، گروه‌بندی‌شده بر اساس مالک
    getFeed: () => request('/stories/feed'),

    // استوری‌های فعال یک کاربر خاص
    getUserStories: (userId) =>
      request(`/stories/user/${encodeURIComponent(userId)}`),

    // ساخت استوری متنی
    createText: (text) =>
      request('/stories', { method: 'POST', body: { mediaType: 'TEXT', text } }),

    // ساخت استوری رسانه‌ای (عکس یا ویدیو)
    createMedia: ({ fileBase64, originalFileName, mimeType, mediaType, caption }) =>
      request('/stories', {
        method: 'POST',
        body: { fileBase64, originalFileName, mimeType, mediaType, caption },
      }),

    // دانلود فایل رسانه‌ی استوری (پاسخ شامل fileBase64 است)
    downloadMedia: (storyId) =>
      request(`/stories/${encodeURIComponent(storyId)}/media`),

    markAsViewed: (storyId) =>
      request(`/stories/${encodeURIComponent(storyId)}/view`, { method: 'POST' }),

    delete: (storyId) =>
      request(`/stories/${encodeURIComponent(storyId)}`, { method: 'DELETE' }),
  };

  // --------------------------------------------------------------
  // WebSocket (اتصال زنده برای دریافت پیام‌های جدید و آنلاین/آفلاین)
  // سرور فقط broadcast می‌کند؛ کلاینت پیامی برای ارسال ندارد.
  // --------------------------------------------------------------
  const Realtime = {
    /**
     * یک اتصال WebSocket به سرور باز می‌کند.
     * @param {Object} handlers - { onMessage, onOpen, onClose, onError }
     * @returns {WebSocket}
     */
    connect(handlers = {}) {
      const token = StorageManager.getToken();
      const url = `${CONFIG.WS_URL}?token=${encodeURIComponent(token || '')}`;
      const ws = new WebSocket(url);

      ws.onopen = (ev) => { if (handlers.onOpen) handlers.onOpen(ev); };
      ws.onclose = (ev) => { if (handlers.onClose) handlers.onClose(ev); };
      ws.onerror = (ev) => { if (handlers.onError) handlers.onError(ev); };
      ws.onmessage = (ev) => {
        if (!handlers.onMessage) return;
        let data = ev.data;
        try { data = JSON.parse(ev.data); } catch (_) { /* رشته‌ی خام */ }
        handlers.onMessage(data);
      };

      return ws;
    },
  };

  // --------------------------------------------------------------
  // Directory
  // --------------------------------------------------------------
  // یک محدودیت واقعی در بک‌اند فعلی: GET /api/chats فقط
  // {id, type, pinned, archived, unreadCount, lastMessageAt} را برمی‌گرداند
  // و نه اعضای چت خصوصی (peer). برای گروه‌ها بک‌اند اکنون
  // GET /api/groups/by-chat/{chatId} را دارد که نام و اعضای واقعی گروه
  // را برای همه‌ی اعضا (نه فقط سازنده) برمی‌گرداند.
  //
  // برای چت‌های خصوصی هنوز از یک کش محلی (localStorage) استفاده می‌شود که
  // در لحظه‌ی ساخت چت (Api.Chats.getOrCreatePrivate) ارتباط
  // chatId -> peerUserId را ثبت می‌کند. اگر چتی در کش نباشد، تلاش می‌کنیم
  // با نگاه به فرستنده‌ی اولین پیام آن را حدس بزنیم؛ در غیر این صورت با
  // یک برچسب عمومی نمایش داده می‌شود.
  const Directory = (() => {
    const LS_KEY = 'wc_chat_directory_v1';

    function loadMap() {
      try {
        return JSON.parse(localStorage.getItem(LS_KEY)) || {};
      } catch (_) {
        return {};
      }
    }
    function saveMap(map) {
      try { localStorage.setItem(LS_KEY, JSON.stringify(map)); } catch (_) { /* ignore */ }
    }

    function linkPrivateChat(chatId, peerUserId) {
      const map = loadMap();
      map[chatId] = { kind: 'PRIVATE', peerUserId };
      saveMap(map);
    }

    function linkGroupChat(chatId, groupId) {
      const map = loadMap();
      map[chatId] = { kind: 'GROUP', groupId };
      saveMap(map);
    }

    function getLink(chatId) {
      const map = loadMap();
      return map[chatId] || null;
    }

    // کش ساده‌ی پروفایل کاربران در حافظه (برای جلوگیری از فراخوانی تکراری)
    const userCache = new Map();
    async function getUserProfile(userId) {
      if (!userId) return null;
      if (userCache.has(userId)) return userCache.get(userId);
      try {
        const profile = await Users.getProfile(userId);
        userCache.set(userId, profile);
        return profile;
      } catch (_) {
        return null;
      }
    }

    const groupCache = new Map();
    async function getGroupInfo(groupId) {
      if (!groupId) return null;
      if (groupCache.has(groupId)) return groupCache.get(groupId);
      try {
        const group = await Groups.get(groupId);
        groupCache.set(groupId, group);
        return group;
      } catch (_) {
        return null;
      }
    }

    // کش گروه بر اساس chatId (کلید متفاوت از کش بالا چون chatId != groupId)
    const groupByChatCache = new Map();
    async function getGroupInfoByChat(chatId) {
      if (!chatId) return null;
      if (groupByChatCache.has(chatId)) return groupByChatCache.get(chatId);
      try {
        const group = await Groups.getByChatId(chatId);
        groupByChatCache.set(chatId, group);
        groupCache.set(group.id, group);
        return group;
      } catch (_) {
        return null;
      }
    }

    // تلاش برای حدس زدن طرف مقابل چت خصوصی از روی فرستنده‌ی پیام‌ها
    async function guessPeerFromMessages(chatId, myUserId) {
      try {
        const messages = await Chats.getMessages(chatId);
        const other = messages.find(m => m.senderId && m.senderId !== myUserId);
        return other ? other.senderId : null;
      } catch (_) {
        return null;
      }
    }

    /**
     * یک آبجکت چت خام از /api/chats را با اطلاعات peer/group غنی‌سازی می‌کند
     * تا شکل خروجی نزدیک به چیزی باشد که رابط کاربری انتظار دارد.
     */
    async function enrichChat(rawChat, myUserId) {
      const chat = { ...rawChat };

      if (chat.type === 'SAVED_MESSAGES') {
        return chat;
      }

      const link = getLink(chat.id);

      if (chat.type === 'PRIVATE') {
        let peerId = link && link.kind === 'PRIVATE' ? link.peerUserId : null;
        if (!peerId) {
          peerId = await guessPeerFromMessages(chat.id, myUserId);
          if (peerId) linkPrivateChat(chat.id, peerId);
        }
        if (peerId) {
          const profile = await getUserProfile(peerId);
          chat.peer = profile
            ? {
              id: profile.id,
              name: profile.username,
              isOnline: profile.isOnline,
              lastSeenAt: profile.lastSeenAt,
              profilePicPath: profile.profilePicPath,
            }
            : { id: peerId, name: peerId, isOnline: false };
        } else {
          chat.peer = { id: '', name: '؟', isOnline: false };
          chat.unresolved = true;
        }
        return chat;
      }

      if (chat.type === 'GROUP') {
        // اول از بک‌اند (منبع درست و مشترک بین همه‌ی اعضا) پرسیده می‌شود
        let group = await getGroupInfoByChat(chat.id);
        if (!group) {
          // در صورت خطای شبکه، از نگاشت محلی (اگر موجود بود) به‌عنوان جایگزین استفاده می‌شود
          const link = getLink(chat.id);
          const groupId = link && link.kind === 'GROUP' ? link.groupId : null;
          group = groupId ? await getGroupInfo(groupId) : null;
        }
        if (group) {
          chat.group = {
            id: group.id,
            name: group.name,
            memberCount: group.memberCount,
            description: group.description,
            members: group.members,
            picturePath: group.picturePath,
          };
        } else {
          chat.group = { id: chat.id, name: 'گروه', memberCount: 0 };
          chat.unresolved = true;
        }
        return chat;
      }

      return chat;
    }

    async function enrichChatList(rawChats, myUserId) {
      return Promise.all(rawChats.map(c => enrichChat(c, myUserId)));
    }

    return {
      linkPrivateChat,
      linkGroupChat,
      getLink,
      getUserProfile,
      getGroupInfo,
      enrichChat,
      enrichChatList,
      clearUserCache: () => userCache.clear(),
      clearGroupCache: () => { groupCache.clear(); groupByChatCache.clear(); },
    };
  })();

  return {
    Auth,
    Chats,
    Groups,
    Contacts,
    Users,
    Settings,
    Reactions,
    Media,
    Search,
    Stories,
    Realtime,
    Directory,
    ApiError,
  };
})();
