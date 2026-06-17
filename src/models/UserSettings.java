package models;
// تنظیمات رابط کاربری مربوط به حساب کاربری.
public class UserSettings {

    private String userId; // کلید خارجی به User
    private boolean darkMode;

    public UserSettings() {}

    public UserSettings(String userId) {
        this.userId = userId;
        this.darkMode = false;
    }

    // Getters
    public String getUserId() { return userId; }
    public boolean isDarkMode() { return darkMode; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    @Override
    public String toString() {
        return "UserSettings{userId='" + userId + "', darkMode=" + darkMode + "}";
    }
}