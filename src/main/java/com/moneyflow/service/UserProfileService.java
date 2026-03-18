package com.moneyflow.service;

import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final Pattern ALLOWED_NICKNAME = Pattern.compile("^[\\p{L}\\p{N} ._-]{1,50}$");

    private final UserRepository userRepository;

    @Transactional
    public UserInfoResponse updateNickname(UUID userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        String normalizedNickname = normalizeNickname(nickname);
        user.setNickname(normalizedNickname);

        return UserInfoResponse.from(user);
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null) {
            throw new BusinessException("닉네임은 필수입니다", ErrorCode.INVALID_NICKNAME);
        }

        String normalized = nickname.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException("닉네임은 필수입니다", ErrorCode.INVALID_NICKNAME);
        }
        if (normalized.length() > 50) {
            throw new BusinessException("닉네임은 50자 이하여야 합니다", ErrorCode.INVALID_NICKNAME);
        }
        if (!ALLOWED_NICKNAME.matcher(normalized).matches()) {
            throw new BusinessException(
                    "닉네임은 한글, 영문, 숫자, 공백, ., _, - 만 사용할 수 있습니다",
                    ErrorCode.INVALID_NICKNAME);
        }

        return normalized;
    }
}
