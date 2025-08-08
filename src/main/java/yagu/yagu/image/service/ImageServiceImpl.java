package yagu.yagu.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Override
    public String upload(MultipartFile file) {
        // 유효성 검사
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "업로드할 파일이 없습니다.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        String contentType = file.getContentType();
        if (!List.of("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        try {
            // 키 생성
            String original = Objects.requireNonNull(file.getOriginalFilename());
            String ext = original.substring(original.lastIndexOf('.'));
            String key = UUID.randomUUID().toString() + ext;

            // 업로드 요청
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            // 퍼블릭 URL 반환
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "파일 읽기에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void deleteByUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        // URL에서 key 추출: https://{bucket}.s3.{region}.amazonaws.com/{key}
        int idx = publicUrl.indexOf("amazonaws.com/");
        if (idx < 0 || idx + 14 >= publicUrl.length()) {
            return; // 예상치 못한 URL 포맷은 무시
        }
        String key = publicUrl.substring(idx + "amazonaws.com/".length());
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception ignored) {
            // 존재하지 않거나 권한 문제 등은 무시 (정합성 우선)
        }
    }
}