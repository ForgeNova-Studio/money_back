package com.moneyflow.security;

import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 소셜 로그인 사용자는 passwordHash가 null이므로 {noop} prefix 사용 (Spring Security 공식 권장)
        // {noop}은 "no operation"을 의미하며, 패스워드 인코딩을 하지 않음을 명시
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "{noop}SOCIAL_LOGIN_USER";

        return new org.springframework.security.core.userdetails.User(
                user.getUserId().toString(),
                password,
                new ArrayList<>()
        );
    }

    public UserDetails loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 소셜 로그인 사용자는 passwordHash가 null이므로 {noop} prefix 사용 (Spring Security 공식 권장)
        // {noop}은 "no operation"을 의미하며, 패스워드 인코딩을 하지 않음을 명시
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "{noop}SOCIAL_LOGIN_USER";

        return new org.springframework.security.core.userdetails.User(
                user.getUserId().toString(),
                password,
                new ArrayList<>()
        );
    }
}
