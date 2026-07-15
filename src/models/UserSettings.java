package models;

// تنظیمات
public class UserSettings {

    private String iduser;
    private boolean darkmode;
    private String backgroundimagepath;
    private boolean notifenabled;
    private boolean notifpreviewenable;
    private String language;

    public UserSettings() {
    }

    public UserSettings(String iduser) {
        this.iduser = iduser;
        this.darkmode = false;
        this.notifenabled = true;
        this.notifpreviewenable = true;
    }

    // Getters
    public String getUserId() {
        return iduser;
    }

    public boolean isDarkMode() {
        return darkmode;
    }

    // Setters
    public void setUserId(String iduser) {
        this.iduser = iduser;
    }

    public void setDarkMode(boolean darkmode) {
        this.darkmode = darkmode;
    }

    public String getBackgroundImagePath() {
        return backgroundimagepath;
    }

    public boolean isNotificationsEnabled() {
        return notifenabled;
    }

    public boolean isNotificationPreviewEnabled() {
        return notifpreviewenable;
    }

    public String getLanguage() {
        return language;
    }

    public void setBackgroundImagePath(String path) {
        this.backgroundimagepath = path;
    }

    public void setNotificationsEnabled(boolean v) {
        this.notifenabled = v;
    }

    public void setNotificationPreviewEnabled(boolean v) {
        this.notifpreviewenable = v;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "UserSettings{userId='" + iduser + "', darkMode=" + darkmode + "}";
    }
}