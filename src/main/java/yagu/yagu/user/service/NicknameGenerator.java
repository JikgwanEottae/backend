package yagu.yagu.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yagu.yagu.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {
    private static final String PREFIX = "직관어때";
    private static final SecureRandom RND = new SecureRandom();
    private final UserRepository userRepo;

    public String newRandomNickname() {
        int[] digitCandidates = {4, 5};
        for (int digits : digitCandidates) {
            int tries = 0;
            int max = (int) Math.pow(10, digits);
            while (tries++ < 10) {
                int num = RND.nextInt(max);
                String nick = PREFIX + String.format("%0" + digits + "d", num);
                if (!userRepo.existsByNicknameAndDeletedAtIsNull(nick)) {
                    return nick;
                }
            }
        }
        return PREFIX + Instant.now().toEpochMilli();
    }
}
