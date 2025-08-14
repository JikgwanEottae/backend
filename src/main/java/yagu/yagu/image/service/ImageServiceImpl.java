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
import java.net.URI;
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

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    @Override
    public String upload(MultipartFile file) {
        // 1) 유효성 검사
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
            // 2) 키 생성 (UUID.ext)
            String original = Objects.requireNonNull(file.getOriginalFilename(), "filename");
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String key = UUID.randomUUID() + ext;

            // 3) 업로드 (비공개, OAC로 접근)
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            // 4) CloudFront URL 반환
            return "https://" + cloudFrontDomain + "/" + key;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "파일 읽기에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;

        try {
            // URL에서 key 추출
            String path = URI.create(url).getPath();
            if (path == null || path.length() <= 1) return;
            String key = path.startsWith("/") ? path.substring(1) : path;

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception ignored) {
        }
    }
}