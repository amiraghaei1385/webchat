// صفحه تنظیمات (متصل به بک‌اند واقعی)

(function () {
  AuthGuard.requireAuth();
  I18N.apply();

  const usernameInput = document.getElementById('usernameInput');
  const bioInput = document.getElementById('bioInput');
  const emailInput = document.getElementById('emailInput');
  const birthdateInput = document.getElementById('birthdateInput');
  const userIdInput = document.getElementById('userIdInput');
  const profileAvatar = document.getElementById('profileAvatar');

  // بک‌دراپ مشترک مودال‌ها
  const backdrop = document.getElementById('modalBackdrop');

  // نمایش آواتار: اگر عکس پروفایل وجود داشته باشد نمایش داده می‌شود،
  // در غیر این صورت حرف اول نام کاربری نمایش داده می‌شود
  async function renderAvatar(user) {
    profileAvatar.style.backgroundImage = '';
    profileAvatar.textContent = (user.username || '?').trim().charAt(0).toUpperCase();
    profileAvatar.style.background = 'var(--gradient-aurora)';
    if (user.profilePicPath) {
      try {
        const blobUrl = await Api.Users.getAvatarBlobUrl(user.id);
        if (blobUrl) {
          profileAvatar.textContent = '';
          profileAvatar.style.background = `center/cover no-repeat url(${blobUrl})`;
        }
      } catch (_) {
        // در صورت خطا حرف اول نمایش داده‌شده باقی می‌ماند
      }
    }
  }

  // بارگذاری پروفایل از بک‌اند واقعی
  async function loadProfile() {
    try {
      const myUserId = StorageManager.getUserId();
      const user = await Api.Users.getProfile(myUserId);
      usernameInput.value = user.username;
      bioInput.value = user.bio || '';
      emailInput.value = user.email || '';
      birthdateInput.value = user.birthDate || '';
      userIdInput.value = user.id;
      await renderAvatar(user);
    } catch (err) {
      Toast.show(err.message || 'خطا در دریافت پروفایل', 'error');
    }
  }

  document.getElementById('saveProfileBtn').addEventListener('click', async () => {
    const newUserId = userIdInput.value.trim();
    if (!newUserId) {
      Toast.show(I18N.t('auth.fillAllFields'), 'error');
      return;
    }
    try {
      const newUsername = usernameInput.value.trim();
      await Api.Users.updateUsername(newUsername);

      const newEmail = emailInput.value.trim();
      if (newEmail) await Api.Users.updateEmail(newEmail);
      else await Api.Users.removeEmail().catch(() => {});

      const newBirthDate = birthdateInput.value;
      if (newBirthDate) await Api.Users.updateBirthDate(newBirthDate);
      else await Api.Users.removeBirthDate().catch(() => {});

      const newBio = bioInput.value.trim();
      await Api.Users.updateBio(newBio);

      // تغییر userId آخرین قدم است چون کلید کاربر را عوض می‌کند
      await Api.Users.updateUserId(newUserId);
      localStorage.setItem(CONFIG.STORAGE_KEYS.USER_ID, newUserId);

      Toast.show(I18N.t('settings.changesSaved'));
    } catch (err) {
      Toast.show(err.message || 'خطا در ذخیره تغییرات', 'error');
    }
  });

  const avatarFileInput = document.getElementById('avatarFileInput');

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

  document.getElementById('changeAvatarBtn').addEventListener('click', () => {
    avatarFileInput.click();
  });

  avatarFileInput.addEventListener('change', async () => {
    const file = avatarFileInput.files && avatarFileInput.files[0];
    if (!file) return;
    try {
      const fileBase64 = await readFileAsBase64(file);
      const updatedUser = await Api.Users.uploadProfilePicture(fileBase64, file.name);
      await renderAvatar(updatedUser);
      Toast.show(I18N.t('settings.changesSaved'));
    } catch (err) {
      Toast.show(err.message || 'خطا در آپلود عکس پروفایل', 'error');
    } finally {
      avatarFileInput.value = '';
    }
  });

  // زبان
  // زبان (منتقل‌شده از صفحه اصلی)
  const languageRow = document.getElementById('languageRow');
  const languageValue = document.getElementById('languageValue');

  function refreshLanguageValue() {
    languageValue.textContent = I18N.getLang() === 'fa' ? 'فارسی' : 'English';
  }
  languageRow.addEventListener('click', () => {
    I18N.toggleLang();
    refreshLanguageValue();
  });
  refreshLanguageValue();

  // تم روشن/تیره
  const themeToggleInput = document.getElementById('themeToggleInput');
  themeToggleInput.checked = StorageManager.getTheme() === 'light';
  document.documentElement.setAttribute('data-theme', StorageManager.getTheme());

  themeToggleInput.addEventListener('change', () => {
    StorageManager.setTheme(themeToggleInput.checked ? 'light' : 'dark');
  });

  // رنگ پس‌زمینه (theme-init.js مقدار اولیه را در بارگذاری صفحه اعمال کرده)
  const colorSwatchGroup = document.getElementById('colorSwatchGroup');
  const savedPalette = StorageManager.getBackgroundColor();

  colorSwatchGroup.querySelectorAll('.color-swatch').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.color === savedPalette);
    btn.addEventListener('click', () => {
      StorageManager.setBackgroundColor(btn.dataset.color);
      colorSwatchGroup.querySelectorAll('.color-swatch').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });

  // اندازه‌ی فونت پیام‌ها
  const fontSizeSwitcher = document.getElementById('fontSizeSwitcher');
  const savedFontSize = StorageManager.getFontSize();

  fontSizeSwitcher.querySelectorAll('.font-size-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.size === savedFontSize);
    btn.addEventListener('click', () => {
      StorageManager.setFontSize(btn.dataset.size);
      fontSizeSwitcher.querySelectorAll('.font-size-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });

  // کاربران مسدود شده
  const AVATAR_COLORS = [
    'linear-gradient(135deg,#00e6d8,#6a5cff)',
    'linear-gradient(135deg,#ff4d8d,#6a5cff)',
    'linear-gradient(135deg,#ffb020,#ff4d8d)',
    'linear-gradient(135deg,#00e6a8,#00e6d8)',
    'linear-gradient(135deg,#6a5cff,#00e6d8)',
  ];
  const blockedPanel = document.getElementById('blockedPanel');
  const blockedListSection = document.getElementById('blockedListSection');
  const blockedCountValue = document.getElementById('blockedCountValue');

  async function refreshBlockedCount() {
    try {
      const list = await Api.Contacts.listBlocked();
      blockedCountValue.textContent = list.length;
    } catch (err) {
      blockedCountValue.textContent = '0';
    }
  }

  async function renderBlockedList() {
    const list = await Api.Contacts.listBlocked();
    if (!list.length) {
      blockedListSection.innerHTML = `<div style="color:var(--text-muted); font-size:13.5px; padding:16px 12px;">${I18N.t('settings.noBlockedUsers')}</div>`;
      return;
    }
    const enriched = await Promise.all(list.map(async (c, i) => {
      const profile = await Api.Directory.getUserProfile(c.contactId);
      return {
        id: c.contactId,
        name: profile ? profile.username : c.contactId,
        avatarColor: i,
      };
    }));
    blockedListSection.innerHTML = enriched.map((u, i) => {
      const bg = AVATAR_COLORS[(u.avatarColor || i) % AVATAR_COLORS.length];
      const initial = (u.name || '?').trim().charAt(0).toUpperCase();
      return `
        <div class="blocked-user-row" data-user-id="${u.id}">
          <div class="avatar" style="background:${bg};">${Utils.escapeHtml(initial)}</div>
          <div class="blocked-user-row-name">${Utils.escapeHtml(u.name)}</div>
          <button class="unblock-btn" data-user-id="${u.id}">${I18N.t('settings.unblockAction')}</button>
        </div>
      `;
    }).join('');

    blockedListSection.querySelectorAll('.unblock-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await Api.Contacts.unblock(btn.dataset.userId);
          await renderBlockedList();
          await refreshBlockedCount();
        } catch (err) {
          Toast.show(err.message || 'خطا در آنبلاک کردن کاربر', 'error');
        }
      });
    });
  }

  document.getElementById('blockedUsersRow').addEventListener('click', async () => {
    await renderBlockedList();
    backdrop.classList.add('open');
    blockedPanel.classList.add('open');
  });
  document.getElementById('closeBlockedPanelBtn').addEventListener('click', () => {
    backdrop.classList.remove('open');
    blockedPanel.classList.remove('open');
  });
  backdrop.addEventListener('click', () => {
    if (blockedPanel.classList.contains('open')) blockedPanel.classList.remove('open');
  });

  refreshBlockedCount();

  // خروج از حساب
  const logoutModal = document.getElementById('logoutModal');
  const deleteAccountModal = document.getElementById('deleteAccountModal');

  function closeAllModals() {
    backdrop.classList.remove('open');
    logoutModal.classList.remove('open');
    deleteAccountModal.classList.remove('open');
  }
  backdrop.addEventListener('click', closeAllModals);

  document.getElementById('logoutRow').addEventListener('click', () => {
    backdrop.classList.add('open');
    logoutModal.classList.add('open');
  });
  document.getElementById('cancelLogoutBtn').addEventListener('click', closeAllModals);
  document.getElementById('confirmLogoutBtn').addEventListener('click', async () => {
    try { await Api.Auth.logout(); } catch (_) { /* در صورت خطای شبکه هم خروج محلی انجام می‌شود */ }
    StorageManager.clearSession();
    window.location.href = 'login.html';
  });

  // حذف حساب
  document.getElementById('deleteAccountRow').addEventListener('click', () => {
    backdrop.classList.add('open');
    deleteAccountModal.classList.add('open');
    document.getElementById('deleteConfirmInput').value = '';
  });
  document.getElementById('cancelDeleteAccountBtn').addEventListener('click', closeAllModals);
  document.getElementById('confirmDeleteAccountBtn').addEventListener('click', async () => {
    const typed = document.getElementById('deleteConfirmInput').value.trim();
    const expected = I18N.t('settings.deleteConfirmWord');
    if (typed !== expected) {
      Toast.show(I18N.t('auth.fillAllFields'), 'error');
      return;
    }
    try {
      await Api.Users.deleteAccount();
      StorageManager.clearSession();
      window.location.href = 'login.html';
    } catch (err) {
      Toast.show(err.message || 'خطا در حذف حساب', 'error');
    }
  });

  document.getElementById('backBtn').addEventListener('click', () => {
    window.location.href = 'home.html';
  });

  document.addEventListener('i18n:changed', refreshLanguageValue);

  loadProfile();
})();
