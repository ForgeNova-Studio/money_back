package com.moneyflow.domain.couple;

import com.moneyflow.domain.accountbook.*;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.JoinCoupleRequest;
import com.moneyflow.dto.response.CoupleResponse;
import com.moneyflow.dto.response.InviteResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * 커플 서비스
 *
 * 커플 연동 관련 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoupleService {

    private final CoupleRepository coupleRepository;
    private final UserRepository userRepository;
    private final AccountBookRepository accountBookRepository;
    private final AccountBookMemberRepository accountBookMemberRepository;

    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_DAYS = 7;

    /**
     * 초대 코드 생성
     *
     * @param userId 초대를 생성하는 사용자 ID
     * @return 초대 코드 정보
     */
    @Transactional
    public InviteResponse generateInviteCode(UUID userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        // 이미 커플에 속해있는지 확인
        Optional<Couple> existingCouple = coupleRepository.findByUserId(userId);
        if (existingCouple.isPresent()) {
            Couple couple = existingCouple.get();

            // 이미 연동이 완료된 경우
            if (couple.isLinked()) {
                throw new BusinessException("이미 커플 연동이 완료되었습니다");
            }

            // 기존 초대 코드가 유효한 경우 재사용
            if (!couple.isCodeExpired()) {
                return InviteResponse.builder()
                        .inviteCode(couple.getInviteCode())
                        .expiresAt(couple.getCodeExpiresAt())
                        .message("기존 초대 코드가 있습니다")
                        .build();
            }

            // 만료된 초대 코드 삭제 (새로 생성)
            coupleRepository.delete(couple);
        }

        // 새 초대 코드 생성
        String inviteCode = generateUniqueInviteCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(CODE_EXPIRY_DAYS);

        // Couple 엔티티 생성
        Couple couple = Couple.builder()
                .user1(user)
                .inviteCode(inviteCode)
                .codeExpiresAt(expiresAt)
                .build();

        coupleRepository.save(couple);

        log.info("초대 코드 생성 완료: userId={}, inviteCode={}", userId, inviteCode);

        return InviteResponse.builder()
                .inviteCode(inviteCode)
                .expiresAt(expiresAt)
                .message("초대 코드가 생성되었습니다")
                .build();
    }

    /**
     * 커플 가입 (초대 코드 입력)
     *
     * @param userId  가입하는 사용자 ID
     * @param request 가입 요청 (초대 코드 포함)
     * @return 커플 정보
     */
    @Transactional
    public CoupleResponse joinCouple(UUID userId, JoinCoupleRequest request) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        // 이미 커플에 속해있는지 확인
        if (coupleRepository.existsByUserId(userId)) {
            throw new BusinessException("이미 커플에 가입되어 있습니다");
        }

        // 초대 코드로 커플 조회
        Couple couple = coupleRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 초대 코드입니다"));

        // 초대 코드 만료 확인
        if (couple.isCodeExpired()) {
            throw new BusinessException("초대 코드가 만료되었습니다");
        }

        // 이미 연동이 완료된 경우
        if (couple.isLinked()) {
            throw new BusinessException("이미 다른 사용자가 가입한 초대 코드입니다");
        }

        // 본인이 생성한 초대 코드인 경우
        if (couple.getUser1().getUserId().equals(userId)) {
            throw new BusinessException("본인이 생성한 초대 코드로는 가입할 수 없습니다");
        }

        // 커플 연동 완료
        couple.setUser2(user);
        couple.setLinkedAt(LocalDateTime.now());
        couple.setInviteCode(null); // 초대 코드 제거 (보안)
        couple.setCodeExpiresAt(null);

        coupleRepository.save(couple);

        // 기본 생활비 장부 자동 생성
        createDefaultAccountBook(couple);

        log.info("커플 가입 완료: userId={}, coupleId={}", userId, couple.getCoupleId());

        return toCoupleResponse(couple);
    }

    /**
     * 커플 연동 해제
     *
     * @param userId 연동 해제를 요청하는 사용자 ID
     */
    @Transactional
    public void unlinkCouple(UUID userId) {
        // 사용자가 속한 커플 조회
        Couple couple = coupleRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("커플 정보를 찾을 수 없습니다"));

        // 커플 가계부 비활성화 (데이터 보존)
        deactivateCoupleAccountBooks(couple);

        // 커플 삭제
        coupleRepository.delete(couple);

        log.info("커플 연동 해제 완료: userId={}, coupleId={}", userId, couple.getCoupleId());
    }

    /**
     * 커플 연동 해제 시 커플 가계부 비활성화
     * 데이터는 보존하고 isActive만 false로 변경
     */
    private void deactivateCoupleAccountBooks(Couple couple) {
        List<AccountBook> coupleAccountBooks = accountBookRepository
                .findByCouple_CoupleIdAndIsActiveTrue(couple.getCoupleId());

        for (AccountBook accountBook : coupleAccountBooks) {
            accountBook.deactivate();
            accountBookRepository.save(accountBook);
            log.info("커플 가계부 비활성화: accountBookId={}", accountBook.getAccountBookId());
        }
    }

    /**
     * 현재 사용자의 커플 정보 조회
     *
     * @param userId 사용자 ID
     * @return 커플 정보 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<CoupleResponse> getCurrentCouple(UUID userId) {
        return coupleRepository.findByUserId(userId)
                .map(this::toCoupleResponse);
    }

    /**
     * 유니크한 초대 코드 생성
     *
     * @return 6자리 영숫자 초대 코드
     */
    private String generateUniqueInviteCode() {
        Random random = new Random();
        String inviteCode;

        // 중복되지 않는 코드가 생성될 때까지 반복
        do {
            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                int index = random.nextInt(INVITE_CODE_CHARS.length());
                code.append(INVITE_CODE_CHARS.charAt(index));
            }
            inviteCode = code.toString();
        } while (coupleRepository.findByInviteCode(inviteCode).isPresent());

        return inviteCode;
    }

    /**
     * Couple 엔티티를 CoupleResponse로 변환
     *
     * @param couple Couple 엔티티
     * @return CoupleResponse
     */
    private CoupleResponse toCoupleResponse(Couple couple) {
        CoupleResponse.UserInfo user1Info = CoupleResponse.UserInfo.builder()
                .userId(couple.getUser1().getUserId())
                .nickname(couple.getUser1().getNickname())
                .email(couple.getUser1().getEmail())
                .build();

        CoupleResponse.UserInfo user2Info = null;
        if (couple.getUser2() != null) {
            user2Info = CoupleResponse.UserInfo.builder()
                    .userId(couple.getUser2().getUserId())
                    .nickname(couple.getUser2().getNickname())
                    .email(couple.getUser2().getEmail())
                    .build();
        }

        return CoupleResponse.builder()
                .coupleId(couple.getCoupleId())
                .user1(user1Info)
                .user2(user2Info)
                .inviteCode(couple.getInviteCode())
                .codeExpiresAt(couple.getCodeExpiresAt())
                .linked(couple.isLinked())
                .linkedAt(couple.getLinkedAt())
                .createdAt(couple.getCreatedAt())
                .build();
    }

    /**
     * 커플 연동 시 기본 생활비 장부 자동 생성
     */
    private void createDefaultAccountBook(Couple couple) {
        User user1 = couple.getUser1();
        User user2 = couple.getUser2();

        // 기본 생활비 장부 생성
        AccountBook accountBook = AccountBook.builder()
                .name("우리의 생활비")
                .bookType(BookType.COUPLE_LIVING)
                .couple(couple)
                .memberCount(2)
                .description("커플 공동 생활비 장부")
                .createdBy(user1)
                .build();

        accountBook = accountBookRepository.save(accountBook);

        // user1을 OWNER로 추가
        AccountBookMember member1 = AccountBookMember.builder()
                .id(new AccountBookMemberId(accountBook.getAccountBookId(), user1.getUserId()))
                .accountBook(accountBook)
                .user(user1)
                .role(MemberRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();
        accountBookMemberRepository.save(member1);

        // user2를 MEMBER로 추가
        AccountBookMember member2 = AccountBookMember.builder()
                .id(new AccountBookMemberId(accountBook.getAccountBookId(), user2.getUserId()))
                .accountBook(accountBook)
                .user(user2)
                .role(MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        accountBookMemberRepository.save(member2);

        log.info("기본 생활비 장부 생성 완료: coupleId={}, accountBookId={}",
                couple.getCoupleId(), accountBook.getAccountBookId());
    }
}
