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
    public void completeProfile(Long userId, String nickname) {
        User u = userRepo.findById(userId).orElseThrow();
        u.completeProfile(nickname);
    }
}
