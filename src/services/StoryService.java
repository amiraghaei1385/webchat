package services;

import models.Contact;
import models.Story;
import repository.ContactRepository;
import repository.StoryRepository;
import repository.UserRepository;
import utils.FileUtil;
import utils.IdGenerator;
import utils.PathUtil;
import java.io.File;
import java.util.*;

// مدیریت استوری‌ها
public class StoryService {

    // ح حجم فایل استوری
    private static final long MAX_filesize = 20L * 1024 * 1024;
    private final StoryRepository storyrepo;
    private final UserRepository userrepo;
    private final ContactRepository contactrepo;

    public StoryService(StoryRepository storyRepository, UserRepository userRepository,
            ContactRepository contactRepository) {
        this.storyrepo = storyRepository;
        this.userrepo = userRepository;
        this.contactrepo = contactRepository;
    }

    // انتشار استوری متنی
    public Story createTextStory(String idowner, String text) {
        if (userrepo.findById(idowner).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (text.length() > 1024) {
            throw new IllegalArgumentException("Story text is too long (max 1024 characters).");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Story text cannot be empty.");
        }
        String idstory = IdGenerator.generate();
        Story story = new Story(idstory, idowner, Story.MediaType.TEXT, null, text);
        storyrepo.save(story);
        return story;
    }

    // انتشار استوری رسانه‌ای
    public Story createMediaStory(String idowner, byte[] filebytes, String orgfilename,
            String mimeType, Story.MediaType mediaType, String caption) {
        if (userrepo.findById(idowner).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (filebytes.length > MAX_filesize) {
            throw new IllegalArgumentException("File size exceeds the maximum allowed limit (20 MB).");
        }
        if (filebytes == null || filebytes.length == 0) {
            throw new IllegalArgumentException("File is empty.");
        }
        if (caption != null && caption.length() > 1024) {
            throw new IllegalArgumentException("Caption is too long (max 1024 characters).");
        }
        if (mediaType == null || mediaType == Story.MediaType.TEXT) {
            throw new IllegalArgumentException("Media type must be IMAGE or VIDEO.");
        }
        String extension = PathUtil.extractExtension(orgfilename);
        String idstory = IdGenerator.generate();
        File filepath = PathUtil.mediaFile("stories_" + idowner, idstory, extension);
        FileUtil.writeBytesAtomic(filepath, filebytes);
        Story story = new Story(idstory, idowner, mediaType, filepath.getPath(), caption);
        storyrepo.save(story);
        return story;
    }

    // استوری‌های فعال یک کاربر
    public List<Story> getActiveStoriesByOwner(String idowner) {
        List<Story> result = new ArrayList<>();
        for (Story story : storyrepo.findByOwnerId(idowner)) {
            if (!story.isDeleted() && !story.isExpired()) {
                result.add(story);
            }
        }
        result.sort(new java.util.Comparator<Story>() {
            public int compare(Story a, Story b) {
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            }
        });
        return result;
    }

    // فید استوری‌های خود کاربر و مخاطبینش
    public List<StoryFeedGroup> getHomeFeed(String idrequester) {
        if (userrepo.findById(idrequester).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        // آیدی افرادی که استوریشون باید نشون داده بشه
        List<String> idrelevantowner = new ArrayList<>();
        idrelevantowner.add(idrequester);
        for (Contact contact : contactrepo.findByOwnerId(idrequester)) {
            if (contact.isContact() && !contact.isBlocked()) {
                idrelevantowner.add(contact.getContactId());
            }
        }
        Map<String, List<Story>> grouped = new LinkedHashMap<>();
        for (Story story : storyrepo.findAll()) {
            if (!idrelevantowner.contains(story.getOwnerId())) {
                continue;
            }
            if (story.isDeleted() || story.isExpired()) {
                continue;
            }
            List<Story> ownerlist = grouped.get(story.getOwnerId());
            if (ownerlist == null) {
                ownerlist = new ArrayList<>();
                grouped.put(story.getOwnerId(), ownerlist);
            }
            ownerlist.add(story);
        }
        List<StoryFeedGroup> feed = new ArrayList<>();
        for (String ownerId : idrelevantowner) {
            List<Story> ownerStories = grouped.get(ownerId);
            if (ownerStories == null || ownerStories.isEmpty()) {
                continue;
            }
            ownerStories.sort(new java.util.Comparator<Story>() {
                public int compare(Story a, Story b) {
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                }
            });
            boolean hasunseen = false;
            for (Story story : ownerStories) {
                if (!story.getViewerIds().contains(idrequester)) {
                    hasunseen = true;
                    break;
                }
            }
            feed.add(new StoryFeedGroup(ownerId, ownerStories, hasunseen));
        }
        return feed;
    }

    // دانلود فایل رسانه استوری
    public byte[] downloadStoryMedia(String idstory) {
        Optional<Story> optstory = storyrepo.findById(idstory);
        if (optstory.isEmpty()) {
            throw new IllegalArgumentException("Story not found.");
        }
        Story story = optstory.get();
        if (story.getFilePath() == null) {
            throw new IllegalStateException("This story has no media file.");
        }
        if (story.isDeleted() || story.isExpired()) {
            throw new IllegalStateException("This story is no longer available.");
        }
        byte[] data = FileUtil.readBytesOrNull(new File(story.getFilePath()));
        if (data == null) {
            throw new IllegalStateException("Story media file is missing from storage.");
        }
        return data;
    }

    // ثبت بازدید استوری
    public Story markAsViewed(String idstory, String idviewer) {
        Optional<Story> optstory = storyrepo.findById(idstory);
        if (optstory.isEmpty()) {
            throw new IllegalArgumentException("Story not found.");
        }
        Story story = optstory.get();
        if (story.isDeleted() || story.isExpired()) {
            throw new IllegalStateException("This story is no longer available.");
        }
        story.addViewer(idviewer);
        storyrepo.update(story);
        return story;
    }

    // پاکسازی استوری‌های منقضی
    public void purgeExpiredStories() {
        for (Story story : storyrepo.findAll()) {
            if (!story.isDeleted() && story.isExpired()) {
                if (story.getFilePath() != null) {
                    FileUtil.delete(new File(story.getFilePath()));
                }
                story.setDeleted(true);
                storyrepo.update(story);
            }
        }
    }

    // حذف استوری توسط صاحب آن
    public void deleteStory(String storyId, String requesterId) {
        Optional<Story> optstory = storyrepo.findById(storyId);
        if (optstory.isEmpty()) {
            throw new IllegalArgumentException("Story not found.");
        }
        Story story = optstory.get();
        if (story.isDeleted()) {
            throw new IllegalStateException("Story is already deleted.");
        }
        if (!story.getOwnerId().equals(requesterId)) {
            throw new IllegalStateException("You can only delete your own stories.");
        }
        if (story.getFilePath() != null) {
            FileUtil.delete(new File(story.getFilePath()));
        }
        story.setDeleted(true);
        storyrepo.update(story);
    }

    // گروه‌بندی استوری‌ها بر اساس صاحب آن‌ها در فید هوم
    public static class StoryFeedGroup {
        private final String idowner;
        private final List<Story> story;
        private final boolean hasunseen;

        public StoryFeedGroup(String ownerId, List<Story> stories, boolean hasUnseen) {
            this.idowner = ownerId;
            this.story = stories;
            this.hasunseen = hasUnseen;
        }

        public boolean isHasUnseen() {
            return hasunseen;
        }

        public String getOwnerId() {
            return idowner;
        }

        public List<Story> getStories() {
            return story;
        }

    }
}