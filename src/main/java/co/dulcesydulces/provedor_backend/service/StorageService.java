package co.dulcesydulces.provedor_backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String storeProfileImage(MultipartFile file, String userCode) throws IOException;
    boolean deleteProfileImage(String filename, String userCode);
    String getProfileImageUrl(String filename, String userCode);
}
