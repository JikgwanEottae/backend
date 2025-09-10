package yagu.yagu.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.image.service.ImageService;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepo;
    private final ImageService imageService;

    /** 프로필 이미지 교체: 새로 업로드하고, 기존 이미지가 있으면 삭제 */
    @Transactional
    public String changeProfileImage(Long userId, MultipartFile file, boolean isImageRemoved) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String oldUrl = user.getProfileImageUrl();
        String newUrl = oldUrl; // 기본은 기존 유지

        if (isImageRemoved) {
            // 이미지 제거
            user.updateProfileImage(null);
            if (oldUrl != null) {
                try { imageService.deleteByUrl(oldUrl); } catch (Exception ignore) {}
            }
            newUrl = null;
        } else if (file != null && !file.isEmpty()) {
            // 새 이미지 업로드
            newUrl = imageService.upload(file);
            user.updateProfileImage(newUrl);
            if (oldUrl != null) {
                try { imageService.deleteByUrl(oldUrl); } catch (Exception ignore) {}
            }
        }

        userRepo.save(user);
        return newUrl;
    }
}
