package repository.file;

import models.ReportedMessage;
import repository.ReportedMessageRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileReportedMessageRepository implements ReportedMessageRepository {
    private final Map<String, ReportedMessage> reports = new ConcurrentHashMap<>();
    private final File fold = new File("storage/reports");

    public FileReportedMessageRepository() {
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
            ReportedMessage report = readReportFromFile(file);
            if (report != null) {
                reports.put(report.getId(), report);
            }
        }
    }

    // خواندن یکی
    private ReportedMessage readReportFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String id = reader.readLine();
            String idmessage = reader.readLine();
            String idreporter = reader.readLine();
            String reason = reader.readLine();
            String reportedat = reader.readLine();
            reader.close();
            ReportedMessage report = new ReportedMessage();
            report.setId(fixEmpty(id));
            report.setMessageId(fixEmpty(idmessage));
            report.setReporterId(fixEmpty(idreporter));
            report.setReason(fixEmpty(reason));
            if (reportedat != null && !reportedat.equals("null")) {
                report.setReportedAt(LocalDateTime.parse(reportedat));
            }
            return report;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از خطای مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن
    private void saveFile(ReportedMessage report) {
        File file = new File(fold, report.getId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(report.getId()));
            writer.newLine();
            writer.write(safe(report.getMessageId()));
            writer.newLine();
            writer.write(safe(report.getReporterId()));
            writer.newLine();
            writer.write(safe(report.getReason()));
            writer.newLine();
            if (report.getReportedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(report.getReportedAt().toString());
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
    public void save(ReportedMessage report) {
        reports.put(report.getId(), report);
        saveFile(report);
    }

    @Override
    public void delete(String id) {
        reports.remove(id);
        File file = new File(fold, id + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public Optional<ReportedMessage> findById(String id) {
        return Optional.ofNullable(reports.get(id));
    }

    @Override
    public List<ReportedMessage> findAll() {
        List<ReportedMessage> res = new ArrayList<>();
        for (ReportedMessage report : reports.values()) {
            res.add(report);
        }
        return res;
    }

}