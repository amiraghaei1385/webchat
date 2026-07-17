// اعمال تم/رنگ/فونت ذخیره‌شده در همه‌ی صفحات (جلوگیری از پرش رنگ در بارگذاری)

(function () {
  document.documentElement.setAttribute('data-theme', StorageManager.getTheme());
  document.documentElement.setAttribute('data-bg-palette', StorageManager.getBackgroundColor());
  document.documentElement.setAttribute('data-msg-font-size', StorageManager.getFontSize());
})();
