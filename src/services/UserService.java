package services;

import models.User;
import repository.UserRepository;
import utils.FileUtil;
import utils.PathUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.io.File;

// مدیریت اطلاعات کاربران
public class UserService {

    private final UserRepository userrepo;
    private static final Pattern emailpatt = Pattern
            .compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public UserService(UserRepository userrepo) {
        this.userrepo = userrepo;
    }

    // لیست تمام کاربران
    public List<User> findAll() {
        return userrepo.findAll();
    }

    // کاربر با نام کاربری
    public Optional<User> findByUsername(String user) {
        return userrepo.findByUsername(user);
    }

    // کاربر با آیدی
    public Optional<User> findById(String iduser) {
        return userrepo.findById(iduser);
    }

    // کاربر آنلاین میشه
    public void setOnline(String iduser) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            return;
        }
        User user = optuser.get();
        user.setOnline(true);
        userrepo.update(user);
    }

    // کاربر آفلاین میشه
    public void setOffline(String iduser) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            return;
        }
        User user = optuser.get();
        user.setOnline(false);
        user.setLastSeenAt(LocalDateTime.now());
        userrepo.update(user);
    }

    // تغییر آیدی کاربر
    public User updateUserId(String idcurrentuser, String newuser) {
        Optional<User> optuser = userrepo.findById(idcurrentuser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        if (!newuser.equals(idcurrentuser) && userrepo.findById(newuser).isPresent()) {
            throw new IllegalArgumentException("User ID already taken.");
        }
        if (newuser == null || newuser.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be empty.");
        }
        user.setId(newuser);
        userrepo.update(user);
        return user;
    }

    // تغییر عکس پروفایل
    public User updateProfilePicture(String iduser, String picturepath) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        user.setProfilePicPath(picturepath);
        userrepo.update(user);
        return user;
    }

    // تغییر نام نمایشی
    public User updateUsername(String iduser, String newuser) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        if (newuser == null || newuser.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        user.setUsername(newuser);
        userrepo.update(user);
        return user;
    }

    // آپلود و ذخیره‌ی عکس پروفایل
    public User saveProfilePictureFile(String iduser, byte[] filebytes, String originalFileName) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        File dir = new File("storage/avatars");
        if (!dir.exists())
            dir.mkdirs();
        String extension = PathUtil.extractExtension(originalFileName);
        if (extension.isEmpty())
            extension = "jpg";
        File file = new File(dir, iduser + "." + extension);
        FileUtil.writeBytesAtomic(file, filebytes);
        User user = optuser.get();
        user.setProfilePicPath(file.getPath());
        userrepo.update(user);
        return user;
    }

    // تغییر عکس پس‌زمینه
    public User updateBackgroundPicture(String iduser, String picturepath) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        user.setBackgroundPicPath(picturepath);
        userrepo.update(user);
        return user;
    }

    // حذف عکس پروفایل
    public User removeProfilePicture(String iduser) {
        return updateProfilePicture(iduser, null);
    }

    // حذف حساب کاربری
    public void deleteAccount(String iduser) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        user.setDeleted(true);
        user.setOnline(false);
        userrepo.update(user);
    }

    // حذف عکس پس‌زمینه
    public User removeBackgroundPicture(String iduser) {
        return updateBackgroundPicture(iduser, null);
    }

    // تغییر بیوگرافی
    public User updateBio(String iduser, String newbio) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        if (newbio != null && newbio.length() > 200) {
            throw new IllegalArgumentException("Bio too long (max 200 characters).");
        }
        user.setBio(newbio);
        userrepo.update(user);
        return user;
    }

    // تغییر ایمیل کاربر
    public User updateEmail(String iduser, String newemail) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        if (newemail == null || newemail.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }
        String trimmedEmail = newemail.trim();
        if (!emailpatt.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        // بررسی تکراری نبودن ایمیل
        for (User other : userrepo.findAll()) {
            if (!other.getId().equals(iduser) && trimmedEmail.equalsIgnoreCase(other.getEmail())) {
                throw new IllegalArgumentException("Email already in use.");
            }
        }
        user.setEmail(trimmedEmail);
        userrepo.update(user);
        return user;
    }

    // حذف تاریخ تولد
    public User removeBirthDate(String iduser) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        user.setBirthDate(null);
        userrepo.update(user);
        return user;
    }

    // حذف ایمیل کاربر
    public User removeEmail(String iduser) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        user.setEmail(null);
        userrepo.update(user);
        return user;
    }

    // تغییر تاریخ تولد
    public User updateBirthDate(String iduser, String newbirthdate) {
        Optional<User> optuser = userrepo.findById(iduser);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = optuser.get();
        if (newbirthdate == null || newbirthdate.isBlank()) {
            throw new IllegalArgumentException("Birth date cannot be empty.");
        }
        LocalDate parsedate;
        try {
            parsedate = LocalDate.parse(newbirthdate.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid birth date format. Use yyyy-MM-dd.");
        }
        if (parsedate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
        }
        user.setBirthDate(parsedate);
        userrepo.update(user);
        return user;
    }
}