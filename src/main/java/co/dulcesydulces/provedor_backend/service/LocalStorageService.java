package co.dulcesydulces.provedor_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir:uploads/perfiles}")
    private String uploadDir;

    @Override
    public String storeProfileImage(MultipartFile file, String userCode) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        if (!isImage(ext)) throw new IOException("Tipo de archivo no permitido");
        String filename = UUID.randomUUID() + "." + ext;
        Path userPath = Paths.get(uploadDir, userCode);
        Files.createDirectories(userPath);
        Path dest = userPath.resolve(filename);
        file.transferTo(dest);
        return filename;
    }

    @Override
    public boolean deleteProfileImage(String filename, String userCode) {
        if (filename == null) return false;
        Path filePath = Paths.get(uploadDir, userCode, filename);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getProfileImageUrl(String filename, String userCode) {
        if (filename == null) return null;
        return "/uploads/perfiles/" + userCode + "/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private boolean isImage(String ext) {
        return ext.matches("png|jpg|jpeg|gif|webp");
    }
}
