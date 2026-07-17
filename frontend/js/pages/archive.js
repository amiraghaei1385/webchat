// صفحه آرشیو

(function () {
  AuthGuard.requireAuth();
  I18N.apply();

  const AVATAR_COLORS = [
    'linear-gradient(135deg,#00e6d8,#6a5cff)',
    'linear-gradient(135deg,#ff4d8d,#6a5cff)',
    'linear-gradient(135deg,#ffb020,#ff4d8d)',
    'linear-gradient(135deg,#00e6a8,#00e6d8)',
    'linear-gradient(135deg,#6a5cff,#00e6d8)',
    'linear-gradient(135deg,#ff4d8d,#ffb020)',
    'linear-gradient(135deg,#00e6d8,#ffb020)',
  ];

  const chatListEl = document.getElementById('chatList');
  const myUserId = StorageManager.getUserId();

  function initials(name) { return (name || '?').trim().charAt(0).toUpperCase(); }
  function displayName(chat) {
    if (chat.type === 'SAVED_MESSAGES') return I18N.t('home.savedMessages');
    if (chat.type === 'GROUP') return chat.group.name;
    return chat.peer.name;
  }
  function avatarBg(chat) {
    const idx = chat.type === 'GROUP' ? chat.group.avatarColor : (chat.peer ? chat.peer.avatarColor : 0);
    return AVATAR_COLORS[(idx || 0) % AVATAR_COLORS.length];
  }

  function buildRowHtml(chat) {
    const name = Utils.escapeHtml(displayName(chat));
    const preview = Utils.escapeHtml(chat.lastMessagePreview || '');
    const time = Utils.formatRelativeTime(chat.lastMessageAt);
    const onlineClass = chat.type === 'PRIVATE' && chat.peer.isOnline ? ' is-online' : '';

    return `
      <div class="chat-row" data-chat-id="${chat.id}">
        <div class="avatar chat-row-avatar${onlineClass}" style="background:${avatarBg(chat)};">${initials(name)}</div>
        <div class="chat-row-body">
          <div class="chat-row-top">
            <span class="chat-row-name">${name}</span>
            <span class="chat-row-time">${time}</span>
          </div>
          <div class="chat-row-preview"><span>${preview}</span></div>
        </div>
      </div>
    `;
  }

  async function render() {
    const rawChats = await Api.Chats.list();
    const allChats = await Api.Directory.enrichChatList(rawChats, myUserId);
    const archived = allChats.filter(c => c.archived).sort((a, b) => new Date(b.lastMessageAt) - new Date(a.lastMessageAt));

    if (!archived.length) {
      chatListEl.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">🗄️</div>
          <div>${I18N.t('home.noArchivedChats')}</div>
        </div>`;
      return;
    }

    chatListEl.innerHTML = archived.map(buildRowHtml).join('');
    chatListEl.querySelectorAll('.chat-row').forEach(row => {
      row.addEventListener('click', () => {
        window.location.href = `chat.html?id=${encodeURIComponent(row.dataset.chatId)}`;
      });
    });
  }

  document.getElementById('backBtn').addEventListener('click', () => {
    window.location.href = 'home.html';
  });

  document.addEventListener('i18n:changed', render);

  render();
})();
