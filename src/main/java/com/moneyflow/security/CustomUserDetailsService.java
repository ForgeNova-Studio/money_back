package com.moneyflow.security;

import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
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
        private final UserAuthRepository userAuthRepository;

        /**
         * 이메일로 사용자 조회 (Spring Security 로그인용)
         * 
         * 핵심 변경: UserAuth 테이블에서 EMAIL provider만 조회합니다.
         * 소셜 로그인 사용자는 EMAIL provider 레코드가 없으므로 비밀번호 로그인이 불가능합니다.
         */
        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                // EMAIL provider로 인증 정보 조회
                UserAuth emailAuth = userAuthRepository.findByUserEmailAndProvider(email, AuthProvider.EMAIL)
                                .orElseThrow(() -> new UsernameNotFoundException("이메일 계정이 없습니다: " + email));

                User user = emailAuth.getUser();

                return new org.springframework.security.core.userdetails.User(
                                user.getUserId().toString(),
                                emailAuth.getPasswordHash(),
                                new ArrayList<>());
        }

        /**
         * 사용자 ID로 사용자 조회 (JWT 토큰 검증용)
         */
        public UserDetails loadUserById(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

                // EMAIL provider 인증 정보가 있는지 확인
                UserAuth emailAuth = userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL)
                                .orElse(null);

                // EMAIL 인증 정보가 있으면 해당 비밀번호 사용, 없으면 빈 비밀번호 (JWT 검증용이므로)
                String password = emailAuth != null ? emailAuth.getPasswordHash() : "";

                return new org.springframework.security.core.userdetails.User(
                                user.getUserId().toString(),
                                password,
                                new ArrayList<>());
        }
}
