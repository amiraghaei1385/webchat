// صفحه گفتگوی جدید

(function () {
  AuthGuard.requireAuth();
  I18N.apply();

  const AVATAR_COLORS = [
    'linear-gradient(135deg,#00e6d8,#6a5cff)',
    'linear-gradient(135deg,#ff4d8d,#6a5cff)',
    'linear-gradient(135deg,#ffb020,#ff4d8d)',
    'linear-gradient(135deg,#00e6a8,#00e6d8)',
    'linear-gradient(135deg,#6a5cff,#00e6d8)',
  ];

  // تب‌ها
  const contactTabBtn = document.getElementById('contactTabBtn');
  const groupTabBtn = document.getElementById('groupTabBtn');
  const contactTabPanel = document.getElementById('contactTabPanel');
  const groupTabPanel = document.getElementById('groupTabPanel');

  function activateTab(tab) {
    const isContact = tab === 'contact';
    contactTabBtn.classList.toggle('active', isContact);
    groupTabBtn.classList.toggle('active', !isContact);
    contactTabPanel.classList.toggle('active', isContact);
    groupTabPanel.classList.toggle('active', !isContact);
  }
  contactTabBtn.addEventListener('click', () => activateTab('contact'));
  groupTabBtn.addEventListener('click', () => activateTab('group'));

  function colorIndexFromId(id) {
    if (!id) return 0;
    let hash = 0;
    for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
    return hash % AVATAR_COLORS.length;
  }

  // افزودن مخاطب
  document.getElementById('addContactBtn').addEventListener('click', async () => {
    const userId = document.getElementById('contactIdInput').value.trim();
    if (!userId) return;

    try {
      // چک می‌کنیم کاربر واقعاً وجود دارد
      await Api.Users.getProfile(userId);
    } catch (_) {
      Toast.show(I18N.t('newChat.userNotFound'), 'error');
      return;
    }

    try {
      await Api.Contacts.add(userId);
      Toast.show(I18N.t('newChat.contactAdded'));
      document.getElementById('contactIdInput').value = '';
      renderContacts();
    } catch (err) {
      Toast.show(err.message || I18N.t('newChat.userNotFound'), 'error');
    }
  });

  // ایجاد گروه
  document.getElementById('createGroupBtn').addEventListener('click', async () => {
    const name = document.getElementById('groupNameInput').value.trim();
    const groupId = document.getElementById('groupIdInput').value.trim();
    if (!name || !groupId) {
      Toast.show(I18N.t('auth.fillAllFields'), 'error');
      return;
    }
    try {
      const group = await Api.Groups.create(name, groupId);
      // ثبت ارتباط chatId -> groupId تا صفحه‌ی هوم بتواند نام گروه را نشان دهد
      Api.Directory.linkGroupChat(group.chatId, group.id);
      Toast.show(I18N.t('newChat.groupCreated'));
      setTimeout(() => { window.location.href = 'home.html'; }, 800);
    } catch (err) {
      Toast.show(err.message || I18N.t('newChat.groupCreated'), 'error');
    }
  });

  // لیست مخاطبین
  async function renderContacts() {
    const listEl = document.getElementById('contactsList');
    let contacts = [];
    try {
      contacts = await Api.Contacts.list();
    } catch (err) {
      listEl.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:12px 4px;">${err.message || I18N.t('newChat.noContacts')}</div>`;
      return;
    }

    if (!contacts.length) {
      listEl.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:12px 4px;">${I18N.t('newChat.noContacts')}</div>`;
      return;
    }

    // برای هر مخاطب پروفایل کامل (نام، وضعیت آنلاین) را می‌گیریم
    const profiles = await Promise.all(
      contacts.map(c => Api.Directory.getUserProfile(c.contactId))
    );

    listEl.innerHTML = contacts.map((c, i) => {
      const profile = profiles[i];
      const displayName = profile ? profile.username : c.contactId;
      const isOnline = profile ? profile.isOnline : false;
      const lastSeenAt = profile ? profile.lastSeenAt : null;
      const bg = AVATAR_COLORS[colorIndexFromId(c.contactId)];
      const initial = (displayName || '?').trim().charAt(0).toUpperCase();
      const status = isOnline
        ? I18N.t('home.onlineStatus')
        : `${I18N.t('home.lastSeenPrefix')} ${Utils.formatRelativeTime(lastSeenAt)}`;
      return `
        <div class="contact-row" data-user-id="${c.contactId}">
          <div class="avatar${isOnline ? ' is-online' : ''}" style="background:${bg};">${Utils.escapeHtml(initial)}</div>
          <div>
            <div class="contact-name">${Utils.escapeHtml(displayName)}</div>
            <div class="contact-status">${status}</div>
          </div>
        </div>
      `;
    }).join('');

    listEl.querySelectorAll('.contact-row').forEach(row => {
      row.addEventListener('click', async () => {
        const targetUserId = row.dataset.userId;
        try {
          const chat = await Api.Chats.getOrCreatePrivate(targetUserId);
          // ثبت ارتباط chatId -> peerUserId تا صفحه‌ی هوم بتواند نام طرف را نشان دهد
          Api.Directory.linkPrivateChat(chat.id, targetUserId);
          window.location.href = `chat.html?id=${encodeURIComponent(chat.id)}`;
        } catch (err) {
          Toast.show(err.message || 'خطا در باز کردن گفتگو', 'error');
        }
      });
    });
  }

  document.getElementById('backBtn').addEventListener('click', () => {
    window.location.href = 'home.html';
  });

  document.addEventListener('i18n:changed', renderContacts);

  renderContacts();
})();
