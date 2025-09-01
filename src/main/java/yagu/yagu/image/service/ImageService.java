package yagu.yagu.image.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    String upload(MultipartFile file);

    /**
     * 퍼블릭 URL을 받아 스토리지에서 해당 파일을 삭제한다.
     * 존재하지 않아도 예외 없이 무시한다.
     */
    void deleteByUrl(String publicUrl);
}
