// صفحه اصلی (متصل به بک‌اند واقعی)

(function () {
  AuthGuard.requireAuth();
  I18N.apply();
  UiEffects.initParticles(document.body, 14);

  const AVATAR_COLORS = [
    'linear-gradient(135deg,#00e6d8,#6a5cff)',
    'linear-gradient(135deg,#ff4d8d,#6a5cff)',
    'linear-gradient(135deg,#ffb020,#ff4d8d)',
    'linear-gradient(135deg,#00e6a8,#00e6d8)',
    'linear-gradient(135deg,#6a5cff,#00e6d8)',
    'linear-gradient(135deg,#ff4d8d,#ffb020)',
    'linear-gradient(135deg,#00e6d8,#ffb020)',
  ];

  // بک‌اند رنگ آواتار نمی‌فرستد، پس از روی آیدی یک عدد پایدار می‌سازیم
  function colorIndexFromId(id) {
    if (!id) return 0;
    let hash = 0;
    for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
    return hash % AVATAR_COLORS.length;
  }

  const chatListEl = document.getElementById('chatList');
  const searchInput = document.getElementById('searchInput');
  const archiveCountEl = document.getElementById('archiveCount');

  let allChats = [];
  const myUserId = StorageManager.getUserId();

  // بارگذاری چت‌ها از بک‌اند واقعی و غنی‌سازی با اطلاعات peer/group
  async function loadChats() {
    try {
      const rawChats = await Api.Chats.list();
      allChats = await Api.Directory.enrichChatList(rawChats, myUserId);
    } catch (err) {
      Toast.show(err.message || 'خطا در دریافت چت‌ها', 'error');
      allChats = [];
    }
    render();
  }

  // کمکی‌ها
  function initials(name) {
    return (name || '?').trim().charAt(0).toUpperCase();
  }

  function displayName(chat) {
    if (chat.type === 'SAVED_MESSAGES') return I18N.t('home.savedMessages');
    if (chat.type === 'GROUP') return chat.group.name;
    return chat.peer.name;
  }

  function avatarStyle(chat) {
    const id = chat.type === 'GROUP' ? chat.group.id : (chat.peer ? chat.peer.id : chat.id);
    return AVATAR_COLORS[colorIndexFromId(id)];
  }

  function isOnline(chat) {
    return chat.type === 'PRIVATE' && chat.peer.isOnline;
  }

  function buildAvatarHtml(chat) {
    if (chat.type === 'SAVED_MESSAGES') {
      return `<div class="avatar chat-row-avatar" style="background:var(--gradient-aurora);">🔖</div>`;
    }
    const bg = avatarStyle(chat);
    const letter = initials(displayName(chat));
    const onlineClass = isOnline(chat) ? ' is-online' : '';
    return `<div class="avatar chat-row-avatar${onlineClass}" style="background:${bg};">${Utils.escapeHtml(letter)}</div>`;
  }

  function buildRowHtml(chat) {
    const name = Utils.escapeHtml(displayName(chat));
    const preview = Utils.escapeHtml(chat.lastMessagePreview || '');
    const time = Utils.formatRelativeTime(chat.lastMessageAt);
    const pinIcon = chat.pinned ? `<span class="chat-row-pin-icon">📌</span>` : '';
    const unread = chat.unreadCount > 0
      ? `<span class="badge-count">${chat.unreadCount > 99 ? '99+' : chat.unreadCount}</span>`
      : '';
    const memberSuffix = chat.type === 'GROUP'
      ? ` · ${chat.group.memberCount} ${I18N.t('home.membersCount')}`
      : '';

    return `
      <div class="chat-row" data-chat-id="${chat.id}" data-pinned="${chat.pinned}" data-archived="${chat.archived}">
        ${buildAvatarHtml(chat)}
        <div class="chat-row-body">
          <div class="chat-row-top">
            ${pinIcon}
            <span class="chat-row-name">${name}</span>
            <span class="chat-row-time">${time}</span>
          </div>
          <div class="chat-row-preview"><span>${preview}${memberSuffix}</span></div>
        </div>
        <div class="chat-row-meta">
          ${unread}
          <div class="chat-row-menu-wrap">
            <button class="chat-row-menu-btn" data-row-menu-btn="${chat.id}"><span data-icon="moreVertical"></span></button>
          </div>
        </div>
      </div>
    `;
  }

  // کش آدرس‌های blob عکس‌ها تا هر آواتار فقط یک‌بار در طول عمر صفحه دانلود شود
  const avatarBlobCache = new Map(); // key: "user:<id>" یا "group:<id>" -> blob URL

  // عکس واقعی کاربر/گروه را می‌گیرد (در صورت وجود) و روی آیکون دایره‌ای مربوطه می‌نشاند؛
  // اگر عکسی ثبت نشده باشد یا دانلود آن شکست بخورد، همان آواتار رنگی/حرف اول باقی می‌ماند
  async function hydrateAvatar(chat) {
    if (chat.type === 'SAVED_MESSAGES') return;

    const isGroup = chat.type === 'GROUP';
    const picturePath = isGroup ? chat.group.picturePath : (chat.peer && chat.peer.profilePicPath);
    if (!picturePath) return;

    const entityId = isGroup ? chat.group.id : chat.peer.id;
    const cacheKey = `${isGroup ? 'group' : 'user'}:${entityId}`;

    try {
      let blobUrl = avatarBlobCache.get(cacheKey);
      if (!blobUrl) {
        blobUrl = isGroup
          ? await Api.Groups.getAvatarBlobUrl(entityId)
          : await Api.Users.getAvatarBlobUrl(entityId);
        if (!blobUrl) return;
        avatarBlobCache.set(cacheKey, blobUrl);
      }

      const row = chatListEl.querySelector(`.chat-row[data-chat-id="${chat.id}"] .chat-row-avatar`);
      if (row) {
        row.textContent = '';
        row.style.background = `center/cover no-repeat url(${blobUrl})`;
      }
    } catch (_) {
      // در صورت خطا آواتار رنگی/حرف اول همچنان نمایش داده می‌شود
    }
  }

  function hydrateVisibleAvatars(visibleChats) {
    visibleChats.forEach(hydrateAvatar);
  }

  function render() {
    const query = (searchInput.value || '').trim().toLowerCase();

    const visible = allChats.filter(c => !c.archived && (
      !query || displayName(c).toLowerCase().includes(query)
    ));
    const archivedCount = allChats.filter(c => c.archived).length;

    archiveCountEl.style.display = archivedCount > 0 ? 'inline-flex' : 'none';
    archiveCountEl.textContent = archivedCount > 99 ? '99+' : archivedCount;

    // سنجاق‌شده‌ها اول
    const pinned = visible.filter(c => c.pinned).sort(byRecency);
    const others = visible.filter(c => !c.pinned).sort(byRecency);

    if (pinned.length === 0 && others.length === 0) {
      chatListEl.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">💬</div>
          <div data-i18n="home.emptyState">${I18N.t('home.emptyState')}</div>
        </div>`;
      return;
    }

    let html = '';
    if (pinned.length) {
      html += `<div class="chat-list-section-label">${I18N.t('home.pinned')}</div>`;
      html += pinned.map(buildRowHtml).join('');
    }
    html += others.map(buildRowHtml).join('');
    chatListEl.innerHTML = html;

    // اتصال کلیک به هر ردیف
    chatListEl.querySelectorAll('.chat-row').forEach(row => {
      row.addEventListener('click', () => openChat(row.dataset.chatId));
    });
    chatListEl.querySelectorAll('.chat-row-menu-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        openRowMenu(btn);
      });
    });

    injectNetVibeIcons();
    hydrateVisibleAvatars([...pinned, ...others]);
  }

  function byRecency(a, b) {
    return new Date(b.lastMessageAt) - new Date(a.lastMessageAt);
  }

  function openChat(chatId) {
    window.location.href = `chat.html?id=${encodeURIComponent(chatId)}`;
  }

  // رویدادها
  searchInput.addEventListener('input', Utils.debounce(render, 120));

  // منوی سه‌نقطه
  const menuBtn = document.getElementById('menuBtn');
  const dropdownMenu = document.getElementById('dropdownMenu');

  menuBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    dropdownMenu.classList.toggle('open');
  });
  document.addEventListener('click', () => dropdownMenu.classList.remove('open'));
  dropdownMenu.addEventListener('click', (e) => e.stopPropagation());

  document.getElementById('settingsMenuItem').addEventListener('click', () => {
    window.location.href = 'settings.html';
  });

  // پیام‌های ذخیره‌شده
  // پیام‌های ذخیره‌شده همیشه در لیست چت‌های کاربر وجود دارد
  document.getElementById('savedMessagesMenuItem').addEventListener('click', () => {
    const saved = allChats.find(c => c.type === 'SAVED_MESSAGES');
    if (saved) window.location.href = `chat.html?id=${encodeURIComponent(saved.id)}`;
  });

  // تغییر حالت شب/روز
  const themeMenuIcon = document.getElementById('themeMenuIcon');
  const themeMenuLabel = document.getElementById('themeMenuLabel');

  function refreshThemeMenuItem() {
    const isLight = StorageManager.getTheme() === 'light';
    themeMenuIcon.textContent = isLight ? '☀️' : '🌙';
    themeMenuLabel.textContent = isLight ? I18N.t('settings.themeLight') : I18N.t('settings.themeDark');
  }

  document.getElementById('themeMenuItem').addEventListener('click', (e) => {
    e.stopPropagation();
    const next = StorageManager.getTheme() === 'light' ? 'dark' : 'light';
    StorageManager.setTheme(next);
    refreshThemeMenuItem();
  });

  // اعمال تم ذخیره‌شده هنگام بارگذاری صفحه
  document.documentElement.setAttribute('data-theme', StorageManager.getTheme());
  refreshThemeMenuItem();

  // نمایش/پنهان‌سازی آرشیو با کشیدن به پایین
  const archiveReveal = document.getElementById('archiveReveal');
  let archiveRevealed = false;
  let touchStartY = null;

  function setArchiveRevealed(value) {
    archiveRevealed = value;
    archiveReveal.classList.toggle('revealed', value);
  }

  chatListEl.addEventListener('touchstart', (e) => {
    touchStartY = chatListEl.scrollTop <= 0 ? e.touches[0].clientY : null;
  }, { passive: true });

  chatListEl.addEventListener('touchmove', (e) => {
    if (touchStartY === null) return;
    const delta = e.touches[0].clientY - touchStartY;
    if (delta > 45 && !archiveRevealed) setArchiveRevealed(true);
    if (delta < -20 && archiveRevealed) setArchiveRevealed(false);
  }, { passive: true });

  chatListEl.addEventListener('wheel', (e) => {
    if (chatListEl.scrollTop <= 0 && e.deltaY < -8 && !archiveRevealed) {
      setArchiveRevealed(true);
    } else if (archiveRevealed && e.deltaY > 8) {
      setArchiveRevealed(false);
    }
  }, { passive: true });

  document.getElementById('archiveRow').addEventListener('click', () => {
    window.location.href = 'archive.html';
  });

  document.getElementById('newChatFab').addEventListener('click', () => {
    window.location.href = 'new-chat.html';
  });

  // منوی کانتکست هر ردیف چت (سنجاق/آرشیو/حذف)
  const rowMenu = document.getElementById('chatRowMenu');
  const rowMenuPinLabel = document.getElementById('rowMenuPinLabel');
  const rowMenuArchiveLabel = document.getElementById('rowMenuArchiveLabel');
  let activeRowChatId = null;

  function closeRowMenu() { rowMenu.classList.remove('open'); }

  function openRowMenu(btn) {
    const chatId = btn.dataset.rowMenuBtn;
    activeRowChatId = chatId;
    const chat = allChats.find(c => c.id === chatId);
    if (!chat) return;

    rowMenuPinLabel.textContent = chat.pinned ? I18N.t('home.unpinAction') : I18N.t('home.pinAction');
    rowMenuArchiveLabel.textContent = chat.archived ? I18N.t('home.unarchiveAction') : I18N.t('home.archiveAction');

    const rect = btn.getBoundingClientRect();
    rowMenu.style.top = Math.min(rect.bottom + 6, window.innerHeight - 160) + 'px';
    rowMenu.style.insetInlineEnd = (window.innerWidth - rect.right) + 'px';
    rowMenu.classList.add('open');
    btn.classList.add('open');
  }

  document.addEventListener('click', () => {
    closeRowMenu();
    document.querySelectorAll('.chat-row-menu-btn.open').forEach(b => b.classList.remove('open'));
  });
  rowMenu.addEventListener('click', (e) => e.stopPropagation());

  document.getElementById('rowMenuPinItem').addEventListener('click', async () => {
    closeRowMenu();
    const chat = allChats.find(c => c.id === activeRowChatId);
    if (!chat) return;
    const newPinned = !chat.pinned;
    try {
      await Api.Chats.setPinned(chat.id, newPinned);
      chat.pinned = newPinned;
      Toast.show(I18N.t(chat.pinned ? 'home.chatPinned' : 'home.chatUnpinned'));
      render();
    } catch (err) {
      Toast.show(err.message || 'خطا در بروزرسانی', 'error');
    }
  });

  document.getElementById('rowMenuArchiveItem').addEventListener('click', async () => {
    closeRowMenu();
    const chat = allChats.find(c => c.id === activeRowChatId);
    if (!chat) return;
    const newArchived = !chat.archived;
    try {
      await Api.Chats.setArchived(chat.id, newArchived);
      chat.archived = newArchived;
      Toast.show(I18N.t(chat.archived ? 'home.archiveAction' : 'home.unarchiveAction'));
      render();
    } catch (err) {
      Toast.show(err.message || 'خطا در بروزرسانی', 'error');
    }
  });

  document.getElementById('rowMenuDeleteItem').addEventListener('click', () => {
    closeRowMenu();
    // 🔲 بک‌اند فعلی هیچ endpoint ای برای حذف گفتگو ندارد (فقط آرشیو/پین دارد)
    // پس این عملیات فقط در نمای محلی مخفی می‌کند، نه در سرور
    Toast.show(I18N.t('common.notSupportedByServer') || 'این عملیات هنوز در سرور پشتیبانی نمی‌شود');
  });

  document.addEventListener('i18n:changed', render);
  document.addEventListener('i18n:changed', () => StoriesFeature.render(document.getElementById('storiesRow')));

  loadChats();
  StoriesFeature.render(document.getElementById('storiesRow'));
})();
