// صفحه ثبت‌نام

(function () {
  AuthGuard.redirectIfAuthed();
  I18N.apply();
  UiEffects.initParticles();
  UiEffects.enableTilt(document.getElementById('authCard'));
  UiEffects.enableLogoSpin(document.getElementById('authLogo'));
  UiEffects.setupPasswordToggle(document.getElementById('signupEyeBtn1'), document.getElementById('signupPassword'));
  UiEffects.setupPasswordToggle(document.getElementById('signupEyeBtn2'), document.getElementById('confirmPassword'));

  const panel = document.getElementById('formPanel');
  const errorBanner = document.getElementById('errorBanner');
  const submitBtn = document.getElementById('signupSubmitBtn');
  const submitLabel = submitBtn.querySelector('span');
  const usernameInput = document.getElementById('signupUsername');
  const userIdInput = document.getElementById('signupUserId');
  const passwordInput = document.getElementById('signupPassword');
  const confirmInput = document.getElementById('confirmPassword');
  const strengthFill = document.getElementById('strengthFill');

  let stopTypewriter = null;
  function restartTypewriter() {
    if (stopTypewriter) stopTypewriter();
    stopTypewriter = UiEffects.startTypewriter(
      document.getElementById('typedTitle'),
      I18N.t('auth.signupTitles')
    );
  }
  restartTypewriter();

  document.getElementById('langToggleBtn').addEventListener('click', () => I18N.toggleLang());
  document.addEventListener('i18n:changed', () => {
    document.getElementById('langToggleBtn').textContent = I18N.t('common.langSwitch');
    restartTypewriter();
  });
  document.getElementById('langToggleBtn').textContent = I18N.t('common.langSwitch');

  // راهنمای زنده‌ی الزامات پسورد
  function updatePasswordRequirements() {
    const rules = Utils.checkPasswordRules(passwordInput.value, usernameInput.value);
    let metCount = 0;
    Object.entries(rules).forEach(([key, met]) => {
      const el = document.querySelector(`.auth-requirement[data-rule="${key}"]`);
      if (!el) return;
      el.classList.toggle('met', met);
      if (met) metCount++;
    });

    const pct = (metCount / Object.keys(rules).length) * 100;
    strengthFill.style.width = pct + '%';
    strengthFill.style.background =
      pct < 40 ? 'var(--state-error)' : pct < 100 ? 'var(--state-warning)' : 'var(--state-success)';

    return rules;
  }
  passwordInput.addEventListener('input', updatePasswordRequirements);
  usernameInput.addEventListener('input', updatePasswordRequirements);
  updatePasswordRequirements();

  // چک‌لیست پسورد فقط با فوکوس نمایش داده می‌شود
  const pwRequirementsPanel = document.getElementById('pwRequirements');
  passwordInput.addEventListener('focus', () => { pwRequirementsPanel.style.display = 'block'; });
  passwordInput.addEventListener('blur', () => {
    // اگر پسورد ناقص است چک‌لیست باز می‌ماند
    const rules = Utils.checkPasswordRules(passwordInput.value, usernameInput.value);
    const allValid = Object.values(rules).every(Boolean);
    if (allValid || !passwordInput.value) pwRequirementsPanel.style.display = 'none';
  });

  // نشانگر زنده‌ی مطابقت پسورد و تکرار آن
  const matchIndicator = document.getElementById('matchIndicator');
  const matchIcon = document.getElementById('matchIcon');
  const matchText = document.getElementById('matchText');

  function updateMatchIndicator() {
    const confirmVal = confirmInput.value;

    // تا تایپ نشده مخفی است
    if (!confirmVal) {
      matchIndicator.classList.remove('visible', 'is-match', 'is-no-match');
      return;
    }

    const isMatch = passwordInput.value === confirmVal;
    matchIndicator.classList.add('visible');
    matchIndicator.classList.toggle('is-match', isMatch);
    matchIndicator.classList.toggle('is-no-match', !isMatch);
    matchIcon.textContent = isMatch ? '✓' : '✗';
    matchText.textContent = isMatch ? I18N.t('auth.passwordsMatch') : I18N.t('auth.passwordsNoMatch');
  }

  confirmInput.addEventListener('input', updateMatchIndicator);
  passwordInput.addEventListener('input', updateMatchIndicator);
  document.addEventListener('i18n:changed', updateMatchIndicator);

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
      ? `<span class="spinner"></span> ${I18N.t('auth.signingUp')}`
      : I18N.t('auth.signUpBtn');
    UiEffects.setDisabled(panel, isLoading);
  }

  submitBtn.addEventListener('click', async (e) => {
    UiEffects.addRipple(submitBtn, e);
    hideError();

    const username = usernameInput.value.trim();
    const userId = userIdInput.value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmInput.value;

    if (!username || !userId || !password || !confirmPassword) {
      UiEffects.shake(panel);
      showError(I18N.t('auth.fillAllFields'));
      return;
    }
    if (password !== confirmPassword) {
      UiEffects.shake(panel);
      showError(I18N.t('auth.passwordMismatch'));
      return;
    }
    if (!Utils.isPasswordValid(password, username)) {
      UiEffects.shake(panel);
      showError(I18N.t('auth.genericSignupError'));
      return;
    }

    setLoading(true);
    try {
      const data = await Api.Auth.register(userId, username, password, confirmPassword);
      // ثبت‌نام موفق یعنی session هم ساخته شده
      StorageManager.saveSession(data.token, data.userId, false);

      document.getElementById('successOverlay').classList.add('show');
      setTimeout(() => { window.location.href = 'home.html'; }, 1200);

    } catch (err) {
      showError(err.message || I18N.t('auth.genericSignupError'));
      UiEffects.shake(panel);
      setLoading(false);
    }
  });
})();
