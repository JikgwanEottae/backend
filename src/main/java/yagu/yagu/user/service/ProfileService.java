package yagu.yagu.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepo;

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        int updated = userRepo.updateNicknameById(userId, nickname);
        if (updated != 1) {
            throw new IllegalStateException("닉네임 업데이트 실패: userId=" + userId);
        }
    }

    @Transactional
    public void updateFavoriteTeam(Long userId, String favoriteTeam) {
        int updated = userRepo.updateFavoriteTeamById(userId, favoriteTeam);
        if (updated != 1) {
            throw new IllegalStateException("응원팀 업데이트 실패: userId=" + userId);
        }
    }
}
