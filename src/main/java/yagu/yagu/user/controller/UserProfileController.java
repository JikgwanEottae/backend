package yagu.yagu.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.user.service.UserProfileService;

import java.util.Map;

@RestController
@RequestMapping("/api/images/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping(path = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeProfileImage(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("isImageRemoved") boolean isImageRemoved
    ) {
        Long userId = principal.getUser().getId();
        String url = userProfileService.changeProfileImage(userId, file, isImageRemoved);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("url", url);

        String msg = isImageRemoved ? "프로필 이미지가 제거되었습니다." : "프로필 이미지가 변경되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(body, msg));
    }
}