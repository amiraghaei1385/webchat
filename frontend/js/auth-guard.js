// محافظت از صفحات بر اساس وضعیت لاگین

const AuthGuard = {
  requireAuth() {
    if (!StorageManager.isLoggedIn()) {
      window.location.href = 'login.html';
    }
  },
  redirectIfAuthed() {
    if (StorageManager.isLoggedIn()) {
      window.location.href = 'home.html';
    }
  },
};
