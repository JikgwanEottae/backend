package yagu.yagu.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
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
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 최대 5MB까지 허용됩니다.");
        }
        String contentType = file.getContentType();
        if (!List.of("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
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
            throw new UncheckedIOException("파일 읽기 실패", e);
        }
    }
}