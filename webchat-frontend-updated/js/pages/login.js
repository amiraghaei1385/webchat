// صفحه ورود

(function () {
  AuthGuard.redirectIfAuthed();
  I18N.apply();
  UiEffects.initParticles();
  UiEffects.enableTilt(document.getElementById('authCard'));
  UiEffects.enableLogoSpin(document.getElementById('authLogo'));
  UiEffects.setupPasswordToggle(
    document.getElementById('loginEyeBtn'),
    document.getElementById('loginPassword')
  );

  const panel = document.getElementById('formPanel');
  const errorBanner = document.getElementById('errorBanner');
  const submitBtn = document.getElementById('loginSubmitBtn');
  const submitLabel = submitBtn.querySelector('span');

  let stopTypewriter = null;
  function restartTypewriter() {
    if (stopTypewriter) stopTypewriter();
    stopTypewriter = UiEffects.startTypewriter(
      document.getElementById('typedTitle'),
      I18N.t('auth.loginTitles')
    );
  }
  restartTypewriter();

  document.getElementById('langToggleBtn').addEventListener('click', () => {
    I18N.toggleLang();
  });
  document.addEventListener('i18n:changed', () => {
    document.getElementById('langToggleBtn').textContent = I18N.t('common.langSwitch');
    restartTypewriter();
  });
  document.getElementById('langToggleBtn').textContent = I18N.t('common.langSwitch');

  function showError(message) {
    errorBanner.textContent = message;
    errorBanner.classList.add('show');
  }
  function hideError() {
    errorBanner.classList.remove('show');
    errorBanner.textContent = '';
  }

  function setLoading(isLoading) {
    submitBtn.classList.toggle('is-loading', isLoading);
    submitLabel.innerHTML = isLoading
      ? `<span class="spinner"></span> ${I18N.t('auth.signingIn')}`
      : I18N.t('auth.signInBtn');
    UiEffects.setDisabled(panel, isLoading);
  }

  submitBtn.addEventListener('click', async (e) => {
    UiEffects.addRipple(submitBtn, e);
    hideError();

    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const rememberMe = document.getElementById('rememberMe').checked;

    if (!username || !password) {
      UiEffects.shake(panel);
      showError(I18N.t('auth.fillAllFields'));
      return;
    }

    setLoading(true);
    try {
      const data = await Api.Auth.login(username, password, rememberMe);
      StorageManager.saveSession(data.token, data.userId, rememberMe);

      document.getElementById('successOverlay').classList.add('show');
      setTimeout(() => { window.location.href = 'home.html'; }, 1200);

    } catch (err) {
      const msg = err.status === 403
        ? I18N.t('auth.accountLocked')
        : (err.message || I18N.t('auth.genericLoginError'));
      showError(msg);
      UiEffects.shake(panel);
      setLoading(false);
    }
  });

  // ==========================================================================
  // فراموشی رمز عبور
  // چون این سامانه ایمیل ندارد، بک‌اند یک کد بازیابی (token) و یک رمز موقت
  // را مستقیماً در پاسخ برمی‌گرداند؛ کاربر همان‌ها را برای تنظیم رمز جدید
  // به فرم بعدی وارد می‌کند (هر دو تا ۱۵ دقیقه معتبرند).
  // ==========================================================================

  const modalBackdrop = document.getElementById('modalBackdrop');
  const forgotModal = document.getElementById('forgotPasswordModal');
  const resetModal = document.getElementById('resetPasswordModal');

  const forgotUsernameInput = document.getElementById('forgotUsernameInput');
  const forgotPasswordError = document.getElementById('forgotPasswordError');
  const forgotSubmitBtn = document.getElementById('submitForgotPasswordBtn');
  const forgotSubmitLabel = forgotSubmitBtn.querySelector('span');

  const resetPasswordError = document.getElementById('resetPasswordError');
  const resetTokenDisplay = document.getElementById('resetTokenDisplay');
  const resetTempPasswordDisplay = document.getElementById('resetTempPasswordDisplay');
  const resetTokenInput = document.getElementById('resetTokenInput');
  const resetTempPasswordInput = document.getElementById('resetTempPasswordInput');
  const resetNewPasswordInput = document.getElementById('resetNewPasswordInput');
  const resetConfirmPasswordInput = document.getElementById('resetConfirmPasswordInput');
  const resetStrengthFill = document.getElementById('resetStrengthFill');
  const resetPwRequirementsPanel = document.getElementById('resetPwRequirements');
  const resetMatchIndicator = document.getElementById('resetMatchIndicator');
  const resetMatchIcon = document.getElementById('resetMatchIcon');
  const resetMatchText = document.getElementById('resetMatchText');
  const resetSubmitBtn = document.getElementById('submitResetPasswordBtn');
  const resetSubmitLabel = resetSubmitBtn.querySelector('span');

  // نام کاربری‌ای که کد برایش گرفته شده؛ برای بررسی قانون «رمز نباید شامل نام کاربری باشد»
  let resetUsernameContext = '';

  function closeAllAuthModals() {
    modalBackdrop.classList.remove('open');
    forgotModal.classList.remove('open');
    resetModal.classList.remove('open');
  }
  modalBackdrop.addEventListener('click', closeAllAuthModals);

  function showForgotError(message) {
    forgotPasswordError.textContent = message;
    forgotPasswordError.classList.add('show');
  }
  function hideForgotError() {
    forgotPasswordError.classList.remove('show');
    forgotPasswordError.textContent = '';
  }
  function showResetError(message) {
    resetPasswordError.textContent = message;
    resetPasswordError.classList.add('show');
  }
  function hideResetError() {
    resetPasswordError.classList.remove('show');
    resetPasswordError.textContent = '';
  }

  document.getElementById('forgotPasswordLink').addEventListener('click', (e) => {
    e.preventDefault();
    hideForgotError();
    forgotUsernameInput.value = document.getElementById('loginUsername').value.trim();
    modalBackdrop.classList.add('open');
    forgotModal.classList.add('open');
    forgotUsernameInput.focus();
  });

  document.getElementById('cancelForgotPasswordBtn').addEventListener('click', closeAllAuthModals);
  document.getElementById('cancelResetPasswordBtn').addEventListener('click', closeAllAuthModals);

  // مرحله ۱: درخواست کد بازیابی با نام کاربری
  forgotSubmitBtn.addEventListener('click', async () => {
    hideForgotError();
    const username = forgotUsernameInput.value.trim();

    if (!username) {
      UiEffects.shake(forgotModal);
      showForgotError(I18N.t('auth.fillAllFields'));
      return;
    }

    forgotSubmitBtn.disabled = true;
    forgotSubmitLabel.innerHTML = `<span class="spinner"></span> ${I18N.t('auth.forgotPasswordSubmitting')}`;
    try {
      const result = await Api.Auth.forgotPassword(username);
      resetUsernameContext = username;

      resetTokenDisplay.textContent = result.token;
      resetTempPasswordDisplay.textContent = result.tempPassword;
      resetTokenInput.value = result.token;
      resetTempPasswordInput.value = result.tempPassword;
      resetNewPasswordInput.value = '';
      resetConfirmPasswordInput.value = '';
      hideResetError();
      updateResetPasswordRequirements();
      updateResetMatchIndicator();

      forgotModal.classList.remove('open');
      resetModal.classList.add('open');
    } catch (err) {
      showForgotError(err.message || I18N.t('auth.genericForgotPasswordError'));
      UiEffects.shake(forgotModal);
    } finally {
      forgotSubmitBtn.disabled = false;
      forgotSubmitLabel.textContent = I18N.t('auth.forgotPasswordSubmitBtn');
    }
  });

  // کپی کد بازیابی/رمز موقت (چون ایمیلی در کار نیست، این‌ها تنها راه کاربر برای نگه‌داشتنشان است)
  function copyToClipboard(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(
        () => Toast.show(I18N.t('auth.forgotPasswordCopiedToast')),
        () => legacyCopy(text)
      );
    } else {
      legacyCopy(text);
    }
  }
  function legacyCopy(text) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
      document.execCommand('copy');
      Toast.show(I18N.t('auth.forgotPasswordCopiedToast'));
    } catch (_) {
      // در بدترین حالت کاربر می‌تواند خودش متن را انتخاب و کپی کند
    }
    document.body.removeChild(textarea);
  }
  document.getElementById('copyTokenBtn').addEventListener('click', () => copyToClipboard(resetTokenDisplay.textContent));
  document.getElementById('copyTempPasswordBtn').addEventListener('click', () => copyToClipboard(resetTempPasswordDisplay.textContent));

  UiEffects.setupPasswordToggle(document.getElementById('resetEyeBtn1'), resetNewPasswordInput);
  UiEffects.setupPasswordToggle(document.getElementById('resetEyeBtn2'), resetConfirmPasswordInput);

  // راهنمای زنده‌ی الزامات پسورد جدید (دقیقاً مشابه signup.js)
  function updateResetPasswordRequirements() {
    const rules = Utils.checkPasswordRules(resetNewPasswordInput.value, resetUsernameContext);
    let metCount = 0;
    Object.entries(rules).forEach(([key, met]) => {
      const el = resetPwRequirementsPanel.querySelector(`.auth-requirement[data-rule="${key}"]`);
      if (!el) return;
      el.classList.toggle('met', met);
      if (met) metCount++;
    });

    const pct = (metCount / Object.keys(rules).length) * 100;
    resetStrengthFill.style.width = pct + '%';
    resetStrengthFill.style.background =
      pct < 40 ? 'var(--state-error)' : pct < 100 ? 'var(--state-warning)' : 'var(--state-success)';

    return rules;
  }
  resetNewPasswordInput.addEventListener('input', updateResetPasswordRequirements);
  resetNewPasswordInput.addEventListener('focus', () => { resetPwRequirementsPanel.style.display = 'block'; });
  resetNewPasswordInput.addEventListener('blur', () => {
    const rules = Utils.checkPasswordRules(resetNewPasswordInput.value, resetUsernameContext);
    const allValid = Object.values(rules).every(Boolean);
    if (allValid || !resetNewPasswordInput.value) resetPwRequirementsPanel.style.display = 'none';
  });

  // نشانگر زنده‌ی مطابقت پسورد جدید و تکرار آن
  function updateResetMatchIndicator() {
    const confirmVal = resetConfirmPasswordInput.value;
    if (!confirmVal) {
      resetMatchIndicator.classList.remove('visible', 'is-match', 'is-no-match');
      return;
    }
    const isMatch = resetNewPasswordInput.value === confirmVal;
    resetMatchIndicator.classList.add('visible');
    resetMatchIndicator.classList.toggle('is-match', isMatch);
    resetMatchIndicator.classList.toggle('is-no-match', !isMatch);
    resetMatchIcon.textContent = isMatch ? '✓' : '✗';
    resetMatchText.textContent = isMatch ? I18N.t('auth.passwordsMatch') : I18N.t('auth.passwordsNoMatch');
  }
  resetConfirmPasswordInput.addEventListener('input', updateResetMatchIndicator);
  resetNewPasswordInput.addEventListener('input', updateResetMatchIndicator);
  document.addEventListener('i18n:changed', updateResetMatchIndicator);

  // مرحله ۲: تایید نهایی با کد + رمز موقت + رمز جدید
  resetSubmitBtn.addEventListener('click', async () => {
    hideResetError();

    const token = resetTokenInput.value.trim();
    const tempPassword = resetTempPasswordInput.value;
    const newPassword = resetNewPasswordInput.value;
    const confirmNewPassword = resetConfirmPasswordInput.value;

    if (!token || !tempPassword || !newPassword || !confirmNewPassword) {
      UiEffects.shake(resetModal);
      showResetError(I18N.t('auth.fillAllFields'));
      return;
    }
    if (newPassword !== confirmNewPassword) {
      UiEffects.shake(resetModal);
      showResetError(I18N.t('auth.passwordMismatch'));
      return;
    }
    if (!Utils.isPasswordValid(newPassword, resetUsernameContext)) {
      UiEffects.shake(resetModal);
      showResetError(I18N.t('auth.genericResetPasswordError'));
      return;
    }

    resetSubmitBtn.disabled = true;
    resetSubmitLabel.innerHTML = `<span class="spinner"></span> ${I18N.t('auth.resetPasswordSubmitting')}`;
    try {
      await Api.Auth.resetPassword(token, tempPassword, newPassword, confirmNewPassword);
      closeAllAuthModals();
      Toast.show(I18N.t('auth.resetPasswordSuccessToast'));

      // برای راحتی کاربر، نام کاربری در فرم ورود از قبل پر می‌شود
      document.getElementById('loginUsername').value = resetUsernameContext;
      document.getElementById('loginPassword').value = '';
      document.getElementById('loginPassword').focus();
    } catch (err) {
      showResetError(err.message || I18N.t('auth.genericResetPasswordError'));
      UiEffects.shake(resetModal);
    } finally {
      resetSubmitBtn.disabled = false;
      resetSubmitLabel.textContent = I18N.t('auth.resetPasswordSubmitBtn');
    }
  });
})();
