package repository.file;

import models.Story;
import repository.StoryRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileStoryRepository implements StoryRepository {
    private final Map<String, Story> stories = new ConcurrentHashMap<>();
    private final File fold = new File("storage/stories");

    public FileStoryRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            Story story = readStoryFromFile(file);
            if (story != null) {
                stories.put(story.getId(), story);
            }
        }
    }

    // خواندن یکی
    private Story readStoryFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String idowner = reader.readLine();
            String mediatypeline = reader.readLine();
            String filepath = reader.readLine();
            String caption = reader.readLine();
            String createatline = reader.readLine();
            String expiresatline = reader.readLine();
            String deleteline = reader.readLine();
            String viewersline = reader.readLine();
            reader.close();
            Story story = new Story();
            story.setId(fixEmpty(id));
            story.setOwnerId(fixEmpty(idowner));
            if (mediatypeline != null && !mediatypeline.equals("null")) {
                story.setMediaType(Story.MediaType.valueOf(mediatypeline));
            }
            story.setFilePath(fixEmpty(filepath));
            story.setCaption(fixEmpty(caption));
            if (createatline != null && !createatline.equals("null")) {
                story.setCreatedAt(LocalDateTime.parse(createatline));
            }
            if (expiresatline != null && !expiresatline.equals("null")) {
                story.setExpiresAt(LocalDateTime.parse(expiresatline));
            }
            story.setDeleted(Boolean.parseBoolean(deleteline));
            Set<String> viewerIds = new HashSet<>();
            if (viewersline != null && !viewersline.equals("null") && !viewersline.isBlank()) {
                for (String viewerId : viewersline.split(",")) {
                    if (!viewerId.isBlank()) {
                        viewerIds.add(viewerId);
                    }
                }
            }
            story.setViewerIds(viewerIds);
            return story;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن
    private void saveFile(Story story) {
        File file = new File(fold, story.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(story.getId()));
            writer.newLine();
            writer.write(safe(story.getOwnerId()));
            writer.newLine();
            writer.write(story.getMediaType() == null ? safe(null) : story.getMediaType().name());
            writer.newLine();
            writer.write(safe(story.getFilePath()));
            writer.newLine();
            writer.write(safe(story.getCaption()));
            writer.newLine();
            if (story.getCreatedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(story.getCreatedAt().toString());
            }
            writer.newLine();
            if (story.getExpiresAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(story.getExpiresAt().toString());
            }
            writer.newLine();
            writer.write(String.valueOf(story.isDeleted()));
            writer.newLine();
            Set<String> viewerIds = story.getViewerIds();
            if (viewerIds == null || viewerIds.isEmpty()) {
                writer.write("");
            } else {
                writer.write(String.join(",", viewerIds));
            }
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // مقدار خالی برای نوشتن
    private String safe(String value) {
        if (value == null) {
            return "null";
        }
        return value;
    }

    @Override
    public void save(Story story) {
        stories.put(story.getId(), story);
        saveFile(story);
    }

    @Override
    public Optional<Story> findById(String id) {
        return Optional.ofNullable(stories.get(id));
    }

    @Override
    public List<Story> findAll() {
        return new ArrayList<>(stories.values());
    }

    @Override
    public void update(Story story) {
        stories.put(story.getId(), story);
        saveFile(story);
    }

    @Override
    public List<Story> findByOwnerId(String ownerId) {
        List<Story> result = new ArrayList<>();
        for (Story story : stories.values()) {
            if (story.getOwnerId().equals(ownerId)) {
                result.add(story);
            }
        }
        return result;
    }

    @Override
    public void delete(String id) {
        stories.remove(id);
        File file = new File(fold, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}