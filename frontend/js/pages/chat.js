// منطق صفحه چت (متصل به بک‌اند واقعی)

(function () {
  AuthGuard.requireAuth();
  I18N.apply();

  const params = new URLSearchParams(window.location.search);
  const chatId = params.get('id');
  const myUserId = StorageManager.getUserId();

  const AVATAR_COLORS = [
    'linear-gradient(135deg,#00e6d8,#6a5cff)',
    'linear-gradient(135deg,#ff4d8d,#6a5cff)',
    'linear-gradient(135deg,#ffb020,#ff4d8d)',
    'linear-gradient(135deg,#00e6a8,#00e6d8)',
    'linear-gradient(135deg,#6a5cff,#00e6d8)',
    'linear-gradient(135deg,#ff4d8d,#ffb020)',
    'linear-gradient(135deg,#00e6d8,#ffb020)',
  ];

  function colorIndexFromId(id) {
    if (!id) return 0;
    let hash = 0;
    for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
    return hash % AVATAR_COLORS.length;
  }

  const messagesEl = document.getElementById('chatMessages');
  const composerInput = document.getElementById('composerInput');
  const sendBtn = document.getElementById('sendBtn');
  const charCount = document.getElementById('charCount');
  const spamToast = document.getElementById('spamToast');

  let chatMeta = null;
  let messages = [];
  let pollingTimer = null;
  let wsConnection = null;
  let activeMessageId = null; // پیامی که منوی کانتکست برایش باز است
  let replyingToMessage = null; // پیامی که کاربر در حال پاسخ به آن است
  // کش نام فرستنده‌ها برای نمایش در گروه (senderId -> username)
  const senderNameCache = new Map();

  // گروه‌کش را برای این گروه پاک و chatMeta را دوباره از سرور می‌خواند
  // (برای مثال بعد از افزودن عضو تا لیست اعضا به‌روز شود)
  async function reloadChatMeta() {
    try {
      Api.Directory.clearGroupCache();
      const rawChats = await Api.Chats.list();
      const found = rawChats.find(c => c.id === chatId);
      if (found) chatMeta = await Api.Directory.enrichChat(found, myUserId);
    } catch (_) { /* غیرحیاتی */ }
  }

  // بارگذاری اولیه
  async function init() {
    if (!chatId) { window.location.href = 'home.html'; return; }

    try {
      const rawChats = await Api.Chats.list();
      const found = rawChats.find(c => c.id === chatId);
      if (!found) { window.location.href = 'home.html'; return; }
      chatMeta = await Api.Directory.enrichChat(found, myUserId);
    } catch (err) {
      Toast.show(err.message || 'خطا در دریافت گفتگو', 'error');
      window.location.href = 'home.html';
      return;
    }

    renderHeader();
    await loadMessages();
    try { await Api.Chats.markAsRead(chatId); } catch (_) { /* غیرحیاتی */ }
    startPolling();
    connectRealtime();
  }

  function initials(name) { return (name || '?').trim().charAt(0).toUpperCase(); }

  function chatDisplayName() {
    if (chatMeta.type === 'SAVED_MESSAGES') return I18N.t('home.savedMessages');
    if (chatMeta.type === 'GROUP') return chatMeta.group.name;
    return chatMeta.peer.name;
  }

  function avatarBg() {
    const id = chatMeta.type === 'GROUP' ? chatMeta.group.id : (chatMeta.peer ? chatMeta.peer.id : chatId);
    return AVATAR_COLORS[colorIndexFromId(id)];
  }

  // اتصال زنده برای دریافت پیام‌های جدید بدون نیاز به رفرش
  function connectRealtime() {
    wsConnection = Api.Realtime.connect({
      onMessage: (data) => {
        // بک‌اند ساختار دقیق پیام broadcast شده را مشخص نکرده،
        // پس فقط از آن به‌عنوان علامت "چیزی تغییر کرده" استفاده می‌کنیم
        // و پیام‌های چت جاری را دوباره از سرور می‌گیریم.
        loadMessages(true);
      },
    });
  }
  window.addEventListener('beforeunload', () => {
    if (wsConnection) wsConnection.close();
  });

  function renderHeader() {
    const name = chatDisplayName();
    document.getElementById('headerName').textContent = name;
    document.title = name;

    const avatarEl = document.getElementById('headerAvatar');
    avatarEl.style.backgroundImage = '';
    avatarEl.style.background = chatMeta.type === 'SAVED_MESSAGES' ? 'var(--gradient-aurora)' : avatarBg();
    avatarEl.textContent = chatMeta.type === 'SAVED_MESSAGES' ? '🔖' : initials(name);
    avatarEl.classList.toggle('is-online', chatMeta.type === 'PRIVATE' && chatMeta.peer.isOnline);

    if (chatMeta.type === 'GROUP' && chatMeta.group.picturePath) {
      Api.Groups.getAvatarBlobUrl(chatMeta.group.id).then(blobUrl => {
        if (blobUrl) {
          avatarEl.textContent = '';
          avatarEl.style.background = `center/cover no-repeat url(${blobUrl})`;
        }
      }).catch(() => {});
    } else if (chatMeta.type === 'PRIVATE' && chatMeta.peer.profilePicPath) {
      Api.Users.getAvatarBlobUrl(chatMeta.peer.id).then(blobUrl => {
        if (blobUrl) {
          avatarEl.textContent = '';
          avatarEl.style.background = `center/cover no-repeat url(${blobUrl})`;
        }
      }).catch(() => {});
    }

    const statusEl = document.getElementById('headerStatus');
    if (chatMeta.type === 'GROUP') {
      statusEl.textContent = `${chatMeta.group.memberCount} ${I18N.t('chat.membersCount')}`;
      statusEl.classList.remove('is-online');
    } else if (chatMeta.type === 'PRIVATE') {
      if (chatMeta.peer.isOnline) {
        statusEl.textContent = I18N.t('home.onlineStatus');
        statusEl.classList.add('is-online');
      } else {
        statusEl.textContent = `${I18N.t('home.lastSeenPrefix')} ${Utils.formatRelativeTime(chatMeta.peer.lastSeenAt)}`;
        statusEl.classList.remove('is-online');
      }
    } else {
      statusEl.textContent = '';
    }
  }

  // بارگذاری و رندر پیام‌ها
  let renderedMessageIds = new Set();

  async function loadMessages(preserveScroll = false) {
    try {
      messages = await Api.Chats.getMessages(chatId);
    } catch (err) {
      Toast.show(err.message || 'خطا در دریافت پیام‌ها', 'error');
      return;
    }
    // فرستنده‌های ناشناخته را برای نمایش نام در چت‌های گروهی بارگذاری می‌کنیم
    if (chatMeta && chatMeta.type === 'GROUP') {
      const unknownSenders = [...new Set(messages.map(m => m.senderId))]
        .filter(id => id && id !== myUserId && !senderNameCache.has(id));
      await Promise.all(unknownSenders.map(async (id) => {
        const profile = await Api.Directory.getUserProfile(id);
        senderNameCache.set(id, profile ? profile.username : id);
      }));
    }
    renderMessages(preserveScroll);
  }

  function formatTime(iso) {
    const d = new Date(iso);
    return d.toLocaleTimeString(I18N.getLang() === 'fa' ? 'fa-IR' : 'en-US', { hour: '2-digit', minute: '2-digit' });
  }

  function renderMessages(preserveScroll = false) {
    const wasAtBottom = !preserveScroll ||
      (messagesEl.scrollHeight - messagesEl.scrollTop - messagesEl.clientHeight < 60);

    let html = '';
    let lastSenderShown = null;
    for (const msg of messages) {
      const mine = msg.senderId === myUserId;
      const showSenderLabel = chatMeta.type === 'GROUP' && !mine && lastSenderShown !== msg.senderId;
      lastSenderShown = msg.senderId;

      const bubbleContent = msg.isDeleted
        ? `<span>${I18N.t('chat.deletedMessagePlaceholder')}</span>`
        : msg.hasMedia
          ? `<div class="msg-media msg-media-loading" data-media-for="${msg.id}">${I18N.t('chat.loadingMedia')}</div>`
          : Utils.escapeHtml(msg.content);

      // بک‌اند فیلد isEdited ندارد؛ اگر editedAt پر باشد یعنی ویرایش شده
      const editedTag = (!msg.isDeleted && msg.editedAt) ? ` <span style="opacity:.65;">${I18N.t('chat.editedLabel')}</span>` : '';

      // 🔲 بک‌اند فعلی مفهوم «پاسخ به پیام» (replyToMessageId) را در مدل Message ندارد
      const replyRefHtml = '';

      // ری‌اکشن‌ها به‌صورت جدا و ناهمگام بارگذاری می‌شوند (بخش پایین‌تر)
      const reactionsHtml = `<div class="msg-reactions" data-reactions-for="${msg.id}"></div>`;

      // فقط پیام جدید انیمیشن می‌گیرد
      const isNew = !renderedMessageIds.has(msg.id);
      const enterClass = isNew ? ' msg-row-enter' : '';

      html += `
        <div class="msg-row ${mine ? 'is-mine' : 'is-theirs'}${enterClass}" data-msg-id="${msg.id}" data-deleted="${msg.isDeleted}" data-mine="${mine}">
          <div class="msg-bubble ${msg.isDeleted ? 'is-deleted' : ''}">
            ${showSenderLabel ? `<div class="msg-sender-label">${Utils.escapeHtml(senderName(msg.senderId))}</div>` : ''}
            ${replyRefHtml}
            ${bubbleContent}
            <div class="msg-meta">${formatTime(msg.sentAt)}${editedTag}</div>
            ${reactionsHtml}
          </div>
        </div>
      `;
    }
    messagesEl.innerHTML = html;

    // ری‌اکشن‌های هر پیام را جدا از سرور می‌گیریم (بک‌اند آن را داخل پیام برنمی‌گرداند)
    messages.filter(m => !m.isDeleted).forEach(msg => loadReactionsFor(msg.id));

    // محتوای رسانه‌ای هر پیام را جدا از سرور می‌گیریم (بک‌اند فقط hasMedia/mediaMessageId را داخل پیام برمی‌گرداند)
    messages.filter(m => !m.isDeleted && m.hasMedia).forEach(msg => loadMediaFor(msg.id));

    messagesEl.querySelectorAll('[data-scroll-to]').forEach(el => {
      el.addEventListener('click', (e) => {
        e.stopPropagation();
        const target = messagesEl.querySelector(`[data-msg-id="${el.dataset.scrollTo}"]`);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'center' });
          target.classList.add('msg-row-highlight');
          setTimeout(() => target.classList.remove('msg-row-highlight'), 1200);
        }
      });
    });

    messagesEl.querySelectorAll('.msg-row').forEach(row => {
      if (row.dataset.deleted === 'true') return;
      row.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        openMessageMenu(row.dataset.msgId, row.dataset.mine === 'true', e);
      });
      row.addEventListener('dblclick', (e) => {
        e.preventDefault();
        openReactionPicker(row.dataset.msgId, e);
      });
    });

    // نکته: کلیک روی بج‌های ری‌اکشن داخل loadReactionsFor سیم‌کشی می‌شود
    // چون آن بج‌ها بعداً و به‌صورت ناهمگام رندر می‌شوند.

    // ثبت پیام‌های دیده‌شده
    renderedMessageIds = new Set(messages.map(m => m.id));

    if (wasAtBottom) messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function senderName(senderId) {
    if (senderId === myUserId) return I18N.t('chat.you') || 'شما';
    return senderNameCache.get(senderId) || senderId;
  }

  // کش ری‌اکشن‌های کاربر فعلی برای هر پیام (msgId -> emoji) تا بدانیم کدام بج "mine" است
  const myReactionCache = new Map();

  // خلاصه‌ی ری‌اکشن‌های یک پیام را از سرور می‌گیرد و بج‌ها را رندر می‌کند
  async function loadReactionsFor(messageId) {
    const container = messagesEl.querySelector(`[data-reactions-for="${messageId}"]`);
    if (!container) return;
    let summary = {};
    let allReactions = [];
    try {
      [summary, allReactions] = await Promise.all([
        Api.Reactions.getSummary(messageId),
        Api.Reactions.getAll(messageId),
      ]);
    } catch (_) {
      return;
    }
    const mine = allReactions.find(r => r.userId === myUserId);
    if (mine) myReactionCache.set(messageId, mine.emoji);
    else myReactionCache.delete(messageId);

    const myEmoji = myReactionCache.get(messageId);
    const badges = Object.entries(summary).map(([emoji, count]) => {
      const mineClass = emoji === myEmoji ? ' is-mine' : '';
      return `
        <button class="msg-reaction-badge${mineClass}" data-msg-id="${messageId}" data-emoji="${emoji}">
          <span>${emoji}</span>
          <span class="msg-reaction-badge-count">${count}</span>
        </button>
      `;
    }).join('');

    container.innerHTML = badges;
    container.querySelectorAll('.msg-reaction-badge').forEach(badge => {
      badge.addEventListener('click', async (e) => {
        e.stopPropagation();
        await applyReaction(badge.dataset.msgId, badge.dataset.emoji);
      });
    });
  }

  // کش نتیجه‌ی دانلود رسانه‌ی هر پیام (msgId -> {mediaType, blobUrl, ...}) تا در رندرهای بعدی دوباره دانلود نشود
  const mediaCache = new Map();

  function formatFileSize(bytes) {
    if (!bytes || bytes <= 0) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  function base64ToBlobUrl(base64, mimeType) {
    const byteChars = atob(base64);
    const byteNumbers = new Array(byteChars.length);
    for (let i = 0; i < byteChars.length; i++) {
      byteNumbers[i] = byteChars.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: mimeType || 'application/octet-stream' });
    return URL.createObjectURL(blob);
  }

  function buildMediaHtml(info, blobUrl) {
    const caption = info.caption
      ? `<div class="msg-media-caption">${Utils.escapeHtml(info.caption)}</div>` : '';
    if (info.mediaType === 'IMAGE' || info.mediaType === 'STICKER') {
      return `<div class="msg-media msg-media-image"><img src="${blobUrl}" alt="${Utils.escapeHtml(info.originalFileName || '')}"></div>${caption}`;
    }
    if (info.mediaType === 'VIDEO') {
      return `<div class="msg-media msg-media-video"><video src="${blobUrl}" controls></video></div>${caption}`;
    }
    if (info.mediaType === 'AUDIO' || info.mediaType === 'VOICE') {
      return `<div class="msg-media msg-media-audio"><audio src="${blobUrl}" controls></audio></div>${caption}`;
    }
    // DOCUMENT و هر نوع دیگر: کارت دانلود فایل
    return `
      <div class="msg-media msg-media-file" data-download-file>
        <div class="msg-media-file-icon">📄</div>
        <div>
          <div class="msg-media-file-name">${Utils.escapeHtml(info.originalFileName || 'file')}</div>
          <div class="msg-media-file-size">${formatFileSize(info.fileSizeBytes)}</div>
        </div>
      </div>${caption}
    `;
  }

  // محتوای واقعی یک پیام رسانه‌ای (عکس/فیلم/صوت/فایل) را می‌گیرد و در حباب مربوطه نشان می‌دهد
  async function loadMediaFor(messageId) {
    const container = messagesEl.querySelector(`[data-media-for="${messageId}"]`);
    if (!container) return;

    if (mediaCache.has(messageId)) {
      const cached = mediaCache.get(messageId);
      container.outerHTML = buildMediaHtml(cached.info, cached.blobUrl);
      wireDownloadClick(messageId);
      return;
    }

    try {
      const [info, downloadRes] = await Promise.all([
        Api.Media.getInfo(chatId, messageId),
        Api.Media.download(chatId, messageId),
      ]);
      const blobUrl = base64ToBlobUrl(downloadRes.fileBase64, downloadRes.mimeType || info.mimeType);
      mediaCache.set(messageId, { info, blobUrl });
      const freshContainer = messagesEl.querySelector(`[data-media-for="${messageId}"]`);
      if (freshContainer) {
        freshContainer.outerHTML = buildMediaHtml(info, blobUrl);
        wireDownloadClick(messageId);
      }
    } catch (_) {
      const freshContainer = messagesEl.querySelector(`[data-media-for="${messageId}"]`);
      if (freshContainer) freshContainer.textContent = I18N.t('chat.mediaLoadError');
    }
  }

  // کلیک روی کارت فایل (غیر تصویر/ویدیو/صوت) باعث دانلود فایل در مرورگر می‌شود
  function wireDownloadClick(messageId) {
    const row = messagesEl.querySelector(`[data-msg-id="${messageId}"]`);
    if (!row) return;
    const card = row.querySelector('[data-download-file]');
    if (!card) return;
    card.addEventListener('click', (e) => {
      e.stopPropagation();
      const cached = mediaCache.get(messageId);
      if (!cached) return;
      const a = document.createElement('a');
      a.href = cached.blobUrl;
      a.download = cached.info.originalFileName || 'file';
      document.body.appendChild(a);
      a.click();
      a.remove();
    });
  }

  // ارسال پیام (با محافظت ضداسپم و محدودیت طول)
  function updateSendButtonState() {
    const len = composerInput.value.length;
    sendBtn.disabled = len === 0 || len > CONFIG.MAX_MESSAGE_LENGTH;
    charCount.textContent = len > CONFIG.MAX_MESSAGE_LENGTH - 200 ? `${len}/${CONFIG.MAX_MESSAGE_LENGTH}` : '';
    charCount.classList.toggle('warn', len > CONFIG.MAX_MESSAGE_LENGTH * 0.9 && len <= CONFIG.MAX_MESSAGE_LENGTH);
    charCount.classList.toggle('over', len > CONFIG.MAX_MESSAGE_LENGTH);
  }

  composerInput.addEventListener('input', () => {
    composerInput.style.height = 'auto';
    composerInput.style.height = Math.min(composerInput.scrollHeight, 120) + 'px';
    updateSendButtonState();
  });

  composerInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      trySend();
    }
  });

  sendBtn.addEventListener('click', trySend);

  function showSpamToast() {
    spamToast.textContent = I18N.t('chat.spamWarning');
    spamToast.classList.add('show');
    setTimeout(() => spamToast.classList.remove('show'), 1800);
  }

  async function trySend() {
    const content = composerInput.value.trim();
    if (!content || content.length > CONFIG.MAX_MESSAGE_LENGTH) return;

    if (!SpamGuard.canSend()) {
      showSpamToast();
      return;
    }
    SpamGuard.recordSend();

    // 🔲 بک‌اند فعلی مفهوم «پاسخ به پیام» (replyToMessageId) را پشتیبانی نمی‌کند
    composerInput.value = '';
    composerInput.style.height = 'auto';
    updateSendButtonState();
    cancelReply();

    try {
      await Api.Chats.sendMessage(chatId, content);
      await loadMessages();
    } catch (err) {
      Toast.show(err.message || 'خطا در ارسال پیام', 'error');
    }
  }

  // منوی پیوست رسانه (عکس/فیلم/صوت/فایل)
  const attachBtn = document.getElementById('attachBtn');
  const attachMenu = document.getElementById('attachMenu');

  function closeAttachMenu() { attachMenu.classList.remove('open'); }
  attachBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    attachMenu.classList.toggle('open');
  });
  document.addEventListener('click', closeAttachMenu);
  attachMenu.addEventListener('click', (e) => e.stopPropagation());

  const attachTargets = [
    ['attachImageItem', 'fileInputImage'],
    ['attachVideoItem', 'fileInputVideo'],
    ['attachAudioItem', 'fileInputAudio'],
    ['attachFileItem', 'fileInputFile'],
  ];
  attachTargets.forEach(([itemId, inputId]) => {
    document.getElementById(itemId).addEventListener('click', () => {
      closeAttachMenu();
      document.getElementById(inputId).click();
    });
  });

  // نگاشت هر ورودی فایل به mediaType مورد انتظار بک‌اند
  const MEDIA_TYPE_BY_INPUT = {
    fileInputImage: 'IMAGE',
    fileInputVideo: 'VIDEO',
    fileInputAudio: 'AUDIO',
    fileInputFile: 'DOCUMENT',
  };

  function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        // نتیجه‌ی readAsDataURL شکل "data:<mime>;base64,XXXX" دارد؛ فقط بخش base64 لازم است
        const result = reader.result || '';
        const commaIdx = result.indexOf(',');
        resolve(commaIdx === -1 ? result : result.slice(commaIdx + 1));
      };
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  [...document.querySelectorAll('#fileInputImage, #fileInputVideo, #fileInputAudio, #fileInputFile')]
    .forEach(input => {
      input.addEventListener('change', async () => {
        const file = input.files && input.files[0];
        if (!file) return;
        const mediaType = MEDIA_TYPE_BY_INPUT[input.id] || 'DOCUMENT';
        try {
          Toast.show(file.name);
          const fileBase64 = await readFileAsBase64(file);
          await Api.Media.upload(chatId, {
            fileBase64,
            originalFileName: file.name,
            mimeType: file.type || 'application/octet-stream',
            mediaType,
            caption: '',
          });
          await loadMessages();
        } catch (err) {
          Toast.show(err.message || 'خطا در آپلود فایل', 'error');
        } finally {
          input.value = '';
        }
      });
    });

  // منوی کانتکست روی پیام
  const contextMenu = document.getElementById('msgContextMenu');

  function openMessageMenu(msgId, isMine, event) {
    activeMessageId = msgId;
    // ریپلای همیشه در دسترس است؛ ویرایش/حذف فقط برای پیام خودم، گزارش فقط برای پیام دیگران
    contextMenu.querySelector('[data-action="edit"]').style.display = isMine ? 'flex' : 'none';
    contextMenu.querySelector('[data-action="delete"]').style.display = isMine ? 'flex' : 'none';
    contextMenu.querySelector('[data-action="report"]').style.display = isMine ? 'none' : 'flex';

    const menuWidth = 180;
    const left = Math.min(event.clientX, window.innerWidth - menuWidth - 12);
    const top = Math.min(event.clientY, window.innerHeight - 200);
    contextMenu.style.top = Math.max(12, top) + 'px';
    contextMenu.style.insetInlineStart = Math.max(12, left) + 'px';
    contextMenu.classList.add('open');
  }
  function closeMessageMenu() { contextMenu.classList.remove('open'); }

  document.addEventListener('click', (e) => {
    if (!contextMenu.contains(e.target)) closeMessageMenu();
  });
  contextMenu.addEventListener('click', (e) => e.stopPropagation());

  contextMenu.querySelectorAll('.msg-context-item').forEach(item => {
    item.addEventListener('click', () => handleContextAction(item.dataset.action));
  });

  // نوار انتخاب ریاکشن (با دابل‌کلیک روی پیام باز می‌شود)
  const reactionPicker = document.getElementById('reactionPicker');
  let activeReactionMessageId = null;

  function openReactionPicker(msgId, event) {
    activeReactionMessageId = msgId;
    const pickerWidth = 230;
    const left = Math.min(event.clientX - pickerWidth / 2, window.innerWidth - pickerWidth - 12);
    const top = Math.max(12, event.clientY - 60);
    reactionPicker.style.top = top + 'px';
    reactionPicker.style.insetInlineStart = Math.max(12, left) + 'px';
    reactionPicker.classList.add('open');
  }
  function closeReactionPicker() { reactionPicker.classList.remove('open'); }

  document.addEventListener('click', (e) => {
    if (!reactionPicker.contains(e.target)) closeReactionPicker();
  });
  reactionPicker.addEventListener('click', (e) => e.stopPropagation());

  reactionPicker.querySelectorAll('.reaction-picker-emoji').forEach(btn => {
    btn.addEventListener('click', async () => {
      closeReactionPicker();
      await applyReaction(activeReactionMessageId, btn.dataset.emoji);
    });
  });

  async function applyReaction(msgId, emoji) {
    try {
      await Api.Reactions.toggle(msgId, emoji);
      await loadReactionsFor(msgId);
    } catch (err) {
      Toast.show(err.message || 'خطا در ثبت ری‌اکشن', 'error');
    }
  }

  function handleContextAction(action) {
    closeMessageMenu();
    const msg = messages.find(m => m.id === activeMessageId);
    if (!msg) return;

    if (action === 'reply') {
      // 🔲 بک‌اند فعلی فیلد replyToMessageId را در مدل Message ندارد،
      // پس این قابلیت را غیرفعال نگه می‌داریم تا چیزی به کاربر دروغ نگوید
      Toast.show(I18N.t('common.notSupportedByServer') || 'این قابلیت هنوز در سرور پشتیبانی نمی‌شود');
    } else if (action === 'edit') {
      startEditMessage(msg);
    } else if (action === 'delete') {
      openBackdropAnd('deleteConfirmModal');
    } else if (action === 'report') {
      openBackdropAnd('reportModal');
    }
  }

  // نوار پاسخ (فعلاً غیرفعال چون بک‌اند از آن پشتیبانی نمی‌کند؛ توابع نگه داشته شده‌اند برای سازگاری)
  const replyPreviewBar = document.getElementById('replyPreviewBar');
  const replyPreviewText = document.getElementById('replyPreviewText');

  function cancelReply() {
    replyingToMessage = null;
    replyPreviewBar.style.display = 'none';
  }

  document.getElementById('replyPreviewCloseBtn').addEventListener('click', cancelReply);

  function startEditMessage(msg) {
    composerInput.value = msg.content;
    composerInput.focus();
    updateSendButtonState();
    composerInput.dataset.editingId = msg.id;

    sendBtn.onclick = async () => {
      const newContent = composerInput.value.trim();
      if (!newContent) return;
      try {
        await Api.Chats.editMessage(chatId, msg.id, newContent);
        composerInput.value = '';
        delete composerInput.dataset.editingId;
        sendBtn.onclick = trySend;
        await loadMessages();
      } catch (err) {
        Toast.show(err.message || 'خطا در ویرایش پیام', 'error');
      }
    };
  }

  // مودال‌ها (حذف / گزارش)
  const backdrop = document.getElementById('modalBackdrop');
  const deleteModal = document.getElementById('deleteConfirmModal');
  const reportModal = document.getElementById('reportModal');
  const addMemberModal = document.getElementById('addMemberModal');

  function openBackdropAnd(modalId) {
    backdrop.classList.add('open');
    document.getElementById(modalId).classList.add('open');
  }
  function closeAllModals() {
    backdrop.classList.remove('open');
    deleteModal.classList.remove('open');
    reportModal.classList.remove('open');
    addMemberModal.classList.remove('open');
  }
  backdrop.addEventListener('click', closeAllModals);
  document.getElementById('cancelAddMemberBtn').addEventListener('click', closeAllModals);

  document.getElementById('cancelDeleteBtn').addEventListener('click', closeAllModals);
  document.getElementById('confirmDeleteBtn').addEventListener('click', async () => {
    if (activeMessageId) {
      try {
        await Api.Chats.deleteMessage(chatId, activeMessageId);
        await loadMessages();
      } catch (err) {
        Toast.show(err.message || 'خطا در حذف پیام', 'error');
      }
    }
    closeAllModals();
  });

  document.getElementById('cancelReportBtn').addEventListener('click', closeAllModals);
  document.getElementById('confirmReportBtn').addEventListener('click', async () => {
    const reason = document.getElementById('reportReasonInput').value.trim();
    closeAllModals();
    if (!activeMessageId) return;
    try {
      await Api.Chats.reportMessage(chatId, activeMessageId, reason);
      Toast.show(I18N.t('chat.reportSent'));
    } catch (err) {
      Toast.show(err.message || 'خطا در ارسال گزارش', 'error');
    }
    document.getElementById('reportReasonInput').value = '';
  });

  // پنل اطلاعات چت (کشویی)
  const infoPanel = document.getElementById('chatInfoPanel');
  const changeGroupAvatarBtn = document.getElementById('changeGroupAvatarBtn');
  const groupAvatarFileInput = document.getElementById('groupAvatarFileInput');

  // نمایش آواتار پنل اطلاعات: اگر عکس گروه/کاربر وجود داشته باشد نمایش داده می‌شود،
  // در غیر این صورت حرف اول یا ایموجی نمایش داده می‌شود (مشابه renderAvatar در settings.js)
  async function renderInfoAvatar() {
    const infoAvatarEl = document.getElementById('infoAvatar');
    infoAvatarEl.style.backgroundImage = '';
    infoAvatarEl.style.background = chatMeta.type === 'SAVED_MESSAGES' ? 'var(--gradient-aurora)' : avatarBg();
    infoAvatarEl.textContent = chatMeta.type === 'SAVED_MESSAGES' ? '🔖' : initials(chatDisplayName());

    if (chatMeta.type === 'GROUP' && chatMeta.group.picturePath) {
      try {
        const blobUrl = await Api.Groups.getAvatarBlobUrl(chatMeta.group.id);
        if (blobUrl) {
          infoAvatarEl.textContent = '';
          infoAvatarEl.style.background = `center/cover no-repeat url(${blobUrl})`;
        }
      } catch (_) {
        // در صورت خطا حرف اول نمایش داده‌شده باقی می‌ماند
      }
    } else if (chatMeta.type === 'PRIVATE' && chatMeta.peer.profilePicPath) {
      try {
        const blobUrl = await Api.Users.getAvatarBlobUrl(chatMeta.peer.id);
        if (blobUrl) {
          infoAvatarEl.textContent = '';
          infoAvatarEl.style.background = `center/cover no-repeat url(${blobUrl})`;
        }
      } catch (_) {
        // در صورت خطا حرف اول نمایش داده‌شده باقی می‌ماند
      }
    }
  }

  function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result || '';
        const commaIdx = result.indexOf(',');
        resolve(commaIdx === -1 ? result : result.slice(commaIdx + 1));
      };
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  changeGroupAvatarBtn.addEventListener('click', () => {
    groupAvatarFileInput.click();
  });

  groupAvatarFileInput.addEventListener('change', async () => {
    const file = groupAvatarFileInput.files && groupAvatarFileInput.files[0];
    if (!file || chatMeta.type !== 'GROUP') return;
    try {
      const fileBase64 = await readFileAsBase64(file);
      const updatedGroup = await Api.Groups.uploadPicture(chatMeta.group.id, fileBase64, file.name);
      chatMeta.group.picturePath = updatedGroup.picturePath;
      await renderInfoAvatar();
      Toast.show(I18N.t('settings.changesSaved'));
    } catch (err) {
      Toast.show(err.message || 'خطا در آپلود عکس گروه', 'error');
    } finally {
      groupAvatarFileInput.value = '';
    }
  });

  function openInfoPanel() {
    changeGroupAvatarBtn.style.display = chatMeta.type === 'GROUP' ? 'flex' : 'none';
    renderInfoAvatar();
    document.getElementById('infoName').textContent = chatDisplayName();

    const subEl = document.getElementById('infoSub');
    const userIdEl = document.getElementById('infoUserId');
    const actionsEl = document.getElementById('infoActionsSection');
    const membersEl = document.getElementById('infoMembersSection');
    const mutualGroupsEl = document.getElementById('infoMutualGroupsSection');

    if (chatMeta.type === 'GROUP') {
      subEl.textContent = `${chatMeta.group.memberCount} ${I18N.t('chat.membersCount')}`;
      userIdEl.textContent = '@' + chatMeta.group.id;
      userIdEl.style.display = 'block';
      mutualGroupsEl.style.display = 'none';
      actionsEl.innerHTML = `
        <button class="side-panel-item" id="addMemberBtn"><span class="side-panel-item-icon" data-icon="userPlus"></span><span>${I18N.t('chat.addMember')}</span></button>
        <button class="side-panel-item" id="editGroupBtn"><span class="side-panel-item-icon" data-icon="edit"></span><span>${I18N.t('chat.editGroupInfo')}</span></button>
        <button class="side-panel-item" id="historyBtn"><span class="side-panel-item-icon" data-icon="clock"></span><span>${I18N.t('chat.history')}</span></button>
        <button class="side-panel-item danger" id="leaveGroupBtn"><span class="side-panel-item-icon" data-icon="logout"></span><span>${I18N.t('chat.leaveGroup')}</span></button>
      `;
      membersEl.style.display = 'block';
      membersEl.innerHTML =
        `<div class="settings-section-label" style="padding:8px 12px;">${I18N.t('chat.membersCount')}</div>` +
        (chatMeta.group.members || []).map(buildMemberRowHtml).join('');
      injectNetVibeIcons();
      wireGroupActions();
    } else if (chatMeta.type === 'PRIVATE') {
      subEl.textContent = chatMeta.peer.isOnline
        ? I18N.t('home.onlineStatus')
        : `${I18N.t('home.lastSeenPrefix')} ${Utils.formatRelativeTime(chatMeta.peer.lastSeenAt)}`;
      userIdEl.textContent = '@' + chatMeta.peer.id;
      userIdEl.style.display = 'block';
      actionsEl.innerHTML = `
        <button class="side-panel-item" id="addContactBtn"><span class="side-panel-item-icon" data-icon="userPlus"></span><span>${I18N.t('chat.addContact')}</span></button>
        <button class="side-panel-item" id="historyBtn"><span class="side-panel-item-icon" data-icon="clock"></span><span>${I18N.t('chat.history')}</span></button>
        <button class="side-panel-item danger" id="blockUserBtn"><span class="side-panel-item-icon" data-icon="userBlocked"></span><span>${I18N.t('chat.blockUser')}</span></button>
      `;
      membersEl.style.display = 'none';
      injectNetVibeIcons();
      wirePrivateActions();
      renderMutualGroups(chatMeta.peer.id);
    } else {
      subEl.textContent = '';
      userIdEl.style.display = 'none';
      mutualGroupsEl.style.display = 'none';
      actionsEl.innerHTML = '';
      membersEl.style.display = 'none';
    }

    document.getElementById('historyBtn')?.addEventListener('click', () => {
      closeInfoPanel();
      openHistoryPanel();
    });

    backdrop.classList.add('open');
    infoPanel.classList.add('open');
  }

  async function renderMutualGroups(peerId) {
    const mutualGroupsEl = document.getElementById('infoMutualGroupsSection');
    let groups = [];
    try {
      groups = await Api.Groups.getMutual(peerId);
    } catch (_) {
      groups = [];
    }

    if (!groups.length) {
      mutualGroupsEl.style.display = 'none';
      return;
    }

    mutualGroupsEl.style.display = 'block';
    mutualGroupsEl.innerHTML =
      `<div class="settings-section-label" style="padding:8px 12px;">${I18N.t('chat.mutualGroups')}</div>` +
      groups.map(g => {
        const bg = AVATAR_COLORS[(g.avatarColor || 0) % AVATAR_COLORS.length];
        const initial = (g.name || '?').trim().charAt(0).toUpperCase();
        return `
          <div class="side-panel-member-row" data-mutual-chat-id="${g.chatId}" style="cursor:pointer;">
            <div class="avatar" style="background:${bg};">${Utils.escapeHtml(initial)}</div>
            <div style="flex:1;">
              <div style="font-size:14px; font-weight:600;">${Utils.escapeHtml(g.name)}</div>
              <div style="font-size:12px; color:var(--text-muted);">${g.memberCount} ${I18N.t('chat.membersCount')}</div>
            </div>
          </div>
        `;
      }).join('');

    mutualGroupsEl.querySelectorAll('[data-mutual-chat-id]').forEach(row => {
      row.addEventListener('click', () => {
        window.location.href = `chat.html?id=${encodeURIComponent(row.dataset.mutualChatId)}`;
      });
    });
  }

  function buildMemberRowHtml(member) {
    const bg = AVATAR_COLORS[(member.avatarColor || 0) % AVATAR_COLORS.length];
    const initial = (member.name || '?').trim().charAt(0).toUpperCase();
    const onlineClass = member.isOnline ? ' is-online' : '';
    return `
      <div class="side-panel-member-row">
        <div class="avatar${onlineClass}" style="background:${bg};">${Utils.escapeHtml(initial)}</div>
        <div style="flex:1;">
          <div style="font-size:14px; font-weight:600;">${Utils.escapeHtml(member.name)}${member.isOwner ? ' 👑' : ''}</div>
        </div>
      </div>
    `;
  }

  async function openAddMemberModal() {
    const listEl = document.getElementById('addMemberList');
    listEl.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:12px 4px;">${I18N.t('common.loading') || '...'}</div>`;
    backdrop.classList.add('open');
    document.getElementById('addMemberModal').classList.add('open');

    let contacts = [];
    try {
      contacts = await Api.Contacts.list();
    } catch (err) {
      listEl.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:12px 4px;">${err.message || I18N.t('newChat.noContacts')}</div>`;
      return;
    }

    const existingIds = new Set((chatMeta.group.members || []).map(m => m.id));
    const candidates = contacts.filter(c => !existingIds.has(c.contactId));

    if (!candidates.length) {
      listEl.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:12px 4px;">${I18N.t('newChat.noContacts')}</div>`;
      return;
    }

    const profiles = await Promise.all(candidates.map(c => Api.Directory.getUserProfile(c.contactId)));

    listEl.innerHTML = candidates.map((c, i) => {
      const profile = profiles[i];
      const displayName = profile ? profile.username : c.contactId;
      const bg = AVATAR_COLORS[((profile && profile.avatarColor) || 0) % AVATAR_COLORS.length];
      const initial = (displayName || '?').trim().charAt(0).toUpperCase();
      return `
        <div class="side-panel-member-row" data-user-id="${c.contactId}" style="cursor:pointer;">
          <div class="avatar" style="background:${bg};">${Utils.escapeHtml(initial)}</div>
          <div style="flex:1;">
            <div style="font-size:14px; font-weight:600;">${Utils.escapeHtml(displayName)}</div>
          </div>
        </div>
      `;
    }).join('');

    listEl.querySelectorAll('[data-user-id]').forEach(row => {
      row.addEventListener('click', async () => {
        const userId = row.dataset.userId;
        try {
          await Api.Groups.addMember(chatMeta.group.id, userId);
          Toast.show(I18N.t('chat.memberAdded'));
          closeAllModals();
          await reloadChatMeta();
          openInfoPanel();
        } catch (err) {
          Toast.show(err.message || 'خطا در افزودن عضو', 'error');
        }
      });
    });
  }

  function wireGroupActions() {
    document.getElementById('addMemberBtn').addEventListener('click', async () => {
      await openAddMemberModal();
    });
    document.getElementById('editGroupBtn').addEventListener('click', async () => {
      // 🔲 فرم ویرایش اطلاعات گروه هنوز پیاده نشده؛ نیازمند endpoint PUT /groups/{id}
      const newName = window.prompt(I18N.t('chat.editGroupInfo'), chatMeta.group.name);
      if (newName === null) return;
      const trimmedName = newName.trim();
      if (!trimmedName) return;
      try {
        await Api.Groups.edit(chatMeta.group.id, trimmedName, chatMeta.group.description || '');
        chatMeta.group.name = trimmedName;
        document.getElementById('infoName').textContent = chatDisplayName();
        Toast.show(I18N.t('settings.changesSaved'));
      } catch (err) {
        Toast.show(err.message || 'خطا در ویرایش گروه', 'error');
      }
    });
    document.getElementById('leaveGroupBtn').addEventListener('click', async () => {
      // 🔲 نیازمند endpoint ترک گروه در بک‌اند
      try {
        await Api.Groups.leave(chatMeta.group.id);
        closeInfoPanel();
        window.location.href = 'home.html';
      } catch (err) {
        Toast.show(err.message || 'خطا در ترک گروه', 'error');
      }
    });
  }

  function wirePrivateActions() {
    document.getElementById('addContactBtn').addEventListener('click', async () => {
      // 🔲 نسخه‌ی واقعی: await Api.Contacts.add(chatMeta.peer.id)
      try {
        await Api.Contacts.add(chatMeta.peer.id);
        Toast.show(I18N.t('newChat.contactAdded'));
      } catch (err) {
        Toast.show(err.message || 'خطا در افزودن مخاطب', 'error');
      }
    });
    document.getElementById('blockUserBtn').addEventListener('click', async () => {
      // 🔲 نیازمند endpoint بلاک کردن کاربر در بک‌اند
      try {
        await Api.Contacts.block(chatMeta.peer.id);
        closeInfoPanel();
        Toast.show(I18N.t('settings.blockedUsers'));
      } catch (err) {
        Toast.show(err.message || 'خطا در بلاک کردن کاربر', 'error');
      }
    });
  }

  function closeInfoPanel() {
    backdrop.classList.remove('open');
    infoPanel.classList.remove('open');
  }
  document.getElementById('closeInfoPanelBtn').addEventListener('click', closeInfoPanel);
  document.getElementById('headerInfo').addEventListener('click', openInfoPanel);

  // بستن پنل با کلیک روی بک‌دراپ
  backdrop.addEventListener('click', () => { if (infoPanel.classList.contains('open')) closeInfoPanel(); });

  // پنل تاریخچه‌ی پیام‌های ویرایش‌شده و حذف‌شده
  const historyPanel = document.getElementById('historyPanel');
  const historyListSection = document.getElementById('historyListSection');

  // نام ویرایش‌کننده را از کش می‌گیرد، و اگر نبود مستقیم از سرور می‌خواند
  async function resolveEditorName(editorId) {
    if (editorId === myUserId) return I18N.t('chat.you') || 'شما';
    if (senderNameCache.has(editorId)) return senderNameCache.get(editorId);
    const profile = await Api.Directory.getUserProfile(editorId);
    const name = profile ? profile.username : editorId;
    senderNameCache.set(editorId, name);
    return name;
  }

  function formatFullTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleString(I18N.getLang() === 'fa' ? 'fa-IR' : 'en-US', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
    });
  }

  async function openHistoryPanel() {
    historyListSection.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:16px 12px;">${I18N.t('chat.loading')}</div>`;
    backdrop.classList.add('open');
    historyPanel.classList.add('open');

    let history = [];
    try {
      history = await Api.Chats.getChatHistory(chatId);
    } catch (err) {
      historyListSection.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:16px 12px;">${err.message || I18N.t('chat.noHistory')}</div>`;
      return;
    }

    if (!history.length) {
      historyListSection.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:16px 12px;">${I18N.t('chat.noHistory')}</div>`;
      return;
    }

    const enriched = await Promise.all(history.map(async (h) => ({
      ...h,
      editorName: await resolveEditorName(h.editorId),
    })));

    // جدیدترین رویداد بالا نمایش داده می‌شود
    enriched.sort((a, b) => new Date(b.editedAt) - new Date(a.editedAt));

    historyListSection.innerHTML = enriched.map((h) => {
      const label = h.isDeletion ? I18N.t('chat.historyDeleted') : I18N.t('chat.historyEdited');
      const badgeClass = h.isDeletion ? 'danger' : '';
      return `
        <div class="history-entry" style="padding:12px; border-bottom:1px solid var(--border-subtle);">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
            <span class="history-entry-badge ${badgeClass}" style="font-size:12px; font-weight:600;">${label}</span>
            <span style="font-size:11px; color:var(--text-muted);">${formatFullTime(h.editedAt)}</span>
          </div>
          <div style="font-size:13px; color:var(--text-muted); margin-bottom:4px;">${Utils.escapeHtml(h.editorName)}</div>
          <div style="font-size:14px; white-space:pre-wrap; word-break:break-word;">${Utils.escapeHtml(h.contentBefore)}</div>
        </div>
      `;
    }).join('');
  }

  function closeHistoryPanel() {
    backdrop.classList.remove('open');
    historyPanel.classList.remove('open');
  }
  document.getElementById('closeHistoryPanelBtn').addEventListener('click', closeHistoryPanel);
  backdrop.addEventListener('click', () => { if (historyPanel.classList.contains('open')) closeHistoryPanel(); });

  // منوی سه‌نقطه‌ی هدر
  const chatDropdown = document.getElementById('chatDropdownMenu');
  const muteToggleItem = document.getElementById('muteToggleItem');
  const muteToggleLabel = document.getElementById('muteToggleLabel');

  function updateMuteMenuState() {
    const muted = !!chatMeta.muted;
    muteToggleItem.classList.toggle('is-muted', muted);
    muteToggleLabel.textContent = muted ? I18N.t('chat.unmuteNotifications') : I18N.t('chat.muteNotifications');
  }

  function closeDropdown() { chatDropdown.classList.remove('open'); }
  document.getElementById('chatMenuBtn').addEventListener('click', (e) => {
    e.stopPropagation();
    updateMuteMenuState();
    chatDropdown.classList.toggle('open');
  });
  document.addEventListener('click', closeDropdown);
  chatDropdown.addEventListener('click', (e) => e.stopPropagation());

  muteToggleItem.addEventListener('click', async () => {
    closeDropdown();
    const newMuted = !chatMeta.muted;
    try {
      await Api.Chats.setMuted(chatId, newMuted);
      chatMeta.muted = newMuted;
      Toast.show(I18N.t(newMuted ? 'chat.chatMuted' : 'chat.chatUnmuted'));
    } catch (err) {
      Toast.show(err.message || 'خطا در بی‌صدا کردن گفتگو', 'error');
    }
  });

  document.getElementById('archiveChatItem').addEventListener('click', async () => {
    closeDropdown();
    const newArchived = !chatMeta.archived;
    try {
      await Api.Chats.setArchived(chatId, newArchived);
      chatMeta.archived = newArchived;
      Toast.show(I18N.t(newArchived ? 'home.archiveAction' : 'home.unarchiveAction'));
    } catch (err) {
      Toast.show(err.message || 'خطا در آرشیو کردن گفتگو', 'error');
    }
  });

  // جستجو در پیام‌های همین گفتگو
  const chatSearchBar = document.getElementById('chatSearchBar');
  const chatSearchInput = document.getElementById('chatSearchInput');
  const chatSearchResults = document.getElementById('chatSearchResults');
  const chatSearchCloseBtn = document.getElementById('chatSearchCloseBtn');

  function openChatSearch() {
    chatSearchBar.style.display = 'flex';
    chatSearchResults.style.display = 'none';
    chatSearchResults.innerHTML = '';
    chatSearchInput.value = '';
    chatSearchInput.focus();
  }

  function closeChatSearch() {
    chatSearchBar.style.display = 'none';
    chatSearchResults.style.display = 'none';
    chatSearchResults.innerHTML = '';
    chatSearchInput.value = '';
  }

  function highlightMatch(text, query) {
    const escaped = Utils.escapeHtml(text);
    if (!query) return escaped;
    const idx = escaped.toLowerCase().indexOf(query.toLowerCase());
    if (idx === -1) return escaped;
    return escaped.slice(0, idx)
      + `<span class="chat-search-highlight">${escaped.slice(idx, idx + query.length)}</span>`
      + escaped.slice(idx + query.length);
  }

  async function runChatSearch() {
    const query = chatSearchInput.value.trim();
    if (!query) {
      chatSearchResults.style.display = 'none';
      chatSearchResults.innerHTML = '';
      return;
    }

    let results = [];
    try {
      results = await Api.Search.messagesInChat(chatId, query);
    } catch (err) {
      chatSearchResults.style.display = 'block';
      chatSearchResults.innerHTML = `<div class="chat-search-result-empty">${err.message || 'خطا در جستجو'}</div>`;
      return;
    }

    chatSearchResults.style.display = 'block';
    if (!results.length) {
      chatSearchResults.innerHTML = `<div class="chat-search-result-empty">${I18N.t('chat.noSearchResults') || 'نتیجه‌ای یافت نشد'}</div>`;
      return;
    }

    chatSearchResults.innerHTML = results.map(r => `
      <div class="chat-search-result-row" data-msg-id="${r.id}">
        ${chatMeta.type === 'GROUP' ? `<div class="chat-search-result-sender">${Utils.escapeHtml(senderName(r.senderId))}</div>` : ''}
        <div class="chat-search-result-text">${highlightMatch(r.content, query)}</div>
      </div>
    `).join('');

    chatSearchResults.querySelectorAll('[data-msg-id]').forEach(row => {
      row.addEventListener('click', () => {
        const target = messagesEl.querySelector(`[data-msg-id="${row.dataset.msgId}"]`);
        closeChatSearch();
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'center' });
          target.classList.add('msg-row-highlight');
          setTimeout(() => target.classList.remove('msg-row-highlight'), 1200);
        } else {
          Toast.show(I18N.t('chat.messageNotLoaded') || 'این پیام در تاریخچه‌ی بارگذاری‌شده نیست');
        }
      });
    });
  }

  chatSearchInput.addEventListener('input', Utils.debounce(runChatSearch, 250));
  chatSearchCloseBtn.addEventListener('click', closeChatSearch);

  document.getElementById('searchInChatItem').addEventListener('click', () => {
    closeDropdown();
    openChatSearch();
  });

  // بازگشت
  document.getElementById('backBtn').addEventListener('click', () => {
    window.location.href = 'home.html';
  });

  // پولینگ دوره‌ای پیام‌های جدید
  function startPolling() {
    pollingTimer = setInterval(async () => {
      await loadMessages(true);
    }, CONFIG.POLLING_INTERVAL_MS);
  }
  window.addEventListener('beforeunload', () => clearInterval(pollingTimer));

  document.addEventListener('i18n:changed', () => {
    renderHeader();
    renderMessages(true);
  });

  init();
})();
