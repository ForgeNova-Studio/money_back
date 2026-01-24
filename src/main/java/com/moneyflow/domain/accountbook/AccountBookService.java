package com.moneyflow.domain.accountbook;

import com.moneyflow.domain.couple.Couple;
import com.moneyflow.domain.couple.CoupleRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.CreateAccountBookRequest;
import com.moneyflow.dto.response.AccountBookResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 장부 서비스
 *
 * 장부 생성, 멤버 관리, 조회 등 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBookService {

        private final AccountBookRepository accountBookRepository;
        private final AccountBookMemberRepository accountBookMemberRepository;
        private final UserRepository userRepository;
        private final CoupleRepository coupleRepository;

        /**
         * 장부 생성
         */
        @Transactional
        public AccountBookResponse createAccountBook(UUID userId, CreateAccountBookRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

                // 커플 장부인 경우 커플 확인
                Couple couple = null;
                if (request.getCoupleId() != null) {
                        couple = coupleRepository.findById(request.getCoupleId())
                                        .orElseThrow(() -> new ResourceNotFoundException("커플 정보를 찾을 수 없습니다"));

                        if (!couple.isMember(userId)) {
                                throw new UnauthorizedException("해당 커플의 멤버가 아닙니다");
                        }
                }

                // 장부 생성
                AccountBook accountBook = AccountBook.builder()
                                .name(request.getName())
                                .bookType(request.getBookType())
                                .couple(couple)
                                .memberCount(request.getMemberCount() != null ? request.getMemberCount() : 2)
                                .description(request.getDescription())
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .createdBy(user)
                                .build();

                accountBook = accountBookRepository.save(accountBook);

                // 생성자를 OWNER로 추가
                AccountBookMember ownerMember = AccountBookMember.builder()
                                .id(new AccountBookMemberId(accountBook.getAccountBookId(), userId))
                                .accountBook(accountBook)
                                .user(user)
                                .role(MemberRole.OWNER)
                                .build();
                accountBookMemberRepository.save(ownerMember);

                // 커플 장부인 경우 파트너도 자동 추가
                if (couple != null && couple.isLinked()) {
                        UUID partnerId = couple.getUser1().getUserId().equals(userId)
                                        ? couple.getUser2().getUserId()
                                        : couple.getUser1().getUserId();

                        User partner = userRepository.findById(partnerId)
                                        .orElseThrow(() -> new ResourceNotFoundException("파트너를 찾을 수 없습니다"));

                        AccountBookMember partnerMember = AccountBookMember.builder()
                                        .id(new AccountBookMemberId(accountBook.getAccountBookId(), partnerId))
                                        .accountBook(accountBook)
                                        .user(partner)
                                        .role(MemberRole.MEMBER)
                                        .build();
                        accountBookMemberRepository.save(partnerMember);
                }

                log.info("장부 생성 완료: accountBookId={}, name={}", accountBook.getAccountBookId(), request.getName());

                return toResponse(accountBook);
        }

        /**
         * 신규 사용자 기본 장부 생성
         */
        @Transactional
        public void createDefaultAccountBookIfMissing(User user) {
                UUID userId = user.getUserId();
                if (accountBookRepository.existsByMemberUserIdAndBookType(userId, BookType.DEFAULT)) {
                        log.info("기본 장부가 이미 존재합니다: userId={}", userId);
                        return;
                }

                AccountBook accountBook = AccountBook.builder()
                                .name("내 가계부")
                                .bookType(BookType.DEFAULT)
                                .memberCount(1)
                                .description("기본 가계부")
                                .createdBy(user)
                                .build();

                accountBook = accountBookRepository.save(accountBook);

                AccountBookMember ownerMember = AccountBookMember.builder()
                                .id(new AccountBookMemberId(accountBook.getAccountBookId(), userId))
                                .accountBook(accountBook)
                                .user(user)
                                .role(MemberRole.OWNER)
                                .build();
                accountBookMemberRepository.save(ownerMember);

                log.info("기본 장부 생성 완료: accountBookId={}, userId={}", accountBook.getAccountBookId(), userId);
        }

        /**
         * 내 장부 목록 조회
         */
        @Transactional(readOnly = true)
        public List<AccountBookResponse> getMyAccountBooks(UUID userId) {
                List<AccountBook> accountBooks = accountBookRepository.findByMemberUserId(userId);
                return accountBooks.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * 장부 상세 조회
         */
        @Transactional(readOnly = true)
        public AccountBookResponse getAccountBook(UUID userId, UUID accountBookId) {
                AccountBook accountBook = accountBookRepository.findByIdAndMemberUserId(accountBookId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없거나 접근 권한이 없습니다"));

                return toResponse(accountBook);
        }

        /**
         * 멤버 추가
         */
        @Transactional
        public void addMember(UUID accountBookId, UUID inviterId, UUID newMemberUserId) {
                AccountBook accountBook = accountBookRepository.findById(accountBookId)
                                .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

                // 권한 확인 (OWNER만 멤버 추가 가능)
                if (!accountBook.isOwner(inviterId)) {
                        throw new UnauthorizedException("멤버를 추가할 권한이 없습니다");
                }

                // 이미 멤버인지 확인
                if (accountBookMemberRepository.existsByAccountBookAccountBookIdAndUserUserId(accountBookId,
                                newMemberUserId)) {
                        throw new BusinessException("이미 장부의 멤버입니다");
                }

                User newMember = userRepository.findById(newMemberUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

                AccountBookMember member = AccountBookMember.builder()
                                .id(new AccountBookMemberId(accountBookId, newMemberUserId))
                                .accountBook(accountBook)
                                .user(newMember)
                                .role(MemberRole.MEMBER)
                                .build();

                accountBookMemberRepository.save(member);

                // 멤버 수 업데이트
                long memberCount = accountBookMemberRepository.countByAccountBookId(accountBookId);
                accountBook.setMemberCount((int) memberCount);
                accountBookRepository.save(accountBook);

                log.info("멤버 추가 완료: accountBookId={}, newMemberId={}", accountBookId, newMemberUserId);
        }

        /**
         * 장부 비활성화 (삭제 대신)
         */
        @Transactional
        public void deactivateAccountBook(UUID userId, UUID accountBookId) {
                AccountBook accountBook = accountBookRepository.findById(accountBookId)
                                .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

                if (!accountBook.isOwner(userId)) {
                        throw new UnauthorizedException("장부를 삭제할 권한이 없습니다");
                }

                accountBook.deactivate();
                accountBookRepository.save(accountBook);

                log.info("장부 비활성화 완료: accountBookId={}", accountBookId);
        }

        /**
         * 장부 멤버 목록 조회
         */
        @Transactional(readOnly = true)
        public List<AccountBookResponse.MemberInfo> getMembers(UUID userId, UUID accountBookId) {
                // 권한 확인
                if (!accountBookMemberRepository.existsByAccountBookAccountBookIdAndUserUserId(accountBookId, userId)) {
                        throw new UnauthorizedException("장부의 멤버만 멤버 목록을 조회할 수 있습니다");
                }

                List<AccountBookMember> members = accountBookMemberRepository
                                .findByAccountBookAccountBookId(accountBookId);

                return members.stream()
                                .map(m -> AccountBookResponse.MemberInfo.builder()
                                                .userId(m.getUser().getUserId())
                                                .nickname(m.getUser().getNickname())
                                                .email(m.getUser().getEmail())
                                                .role(m.getRole().name())
                                                .joinedAt(m.getJoinedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        /**
         * 초기 잔액 수정
         */
        @Transactional
        public void updateInitialBalance(UUID userId, UUID accountBookId, java.math.BigDecimal initialBalance) {
                AccountBook accountBook = accountBookRepository.findByIdAndMemberUserId(accountBookId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없거나 접근 권한이 없습니다"));

                accountBook.setInitialBalance(initialBalance);
                accountBookRepository.save(accountBook);

                log.info("초기 잔액 수정 완료: accountBookId={}, initialBalance={}", accountBookId, initialBalance);
        }

        /**
         * Entity → Response DTO 변환
         */
        private AccountBookResponse toResponse(AccountBook accountBook) {
                List<AccountBookResponse.MemberInfo> memberInfos = accountBook.getMembers().stream()
                                .map(m -> AccountBookResponse.MemberInfo.builder()
                                                .userId(m.getUser().getUserId())
                                                .nickname(m.getUser().getNickname())
                                                .email(m.getUser().getEmail())
                                                .role(m.getRole().name())
                                                .joinedAt(m.getJoinedAt())
                                                .build())
                                .collect(Collectors.toList());

                return AccountBookResponse.builder()
                                .accountBookId(accountBook.getAccountBookId())
                                .name(accountBook.getName())
                                .bookType(accountBook.getBookType())
                                .coupleId(accountBook.getCouple() != null ? accountBook.getCouple().getCoupleId()
                                                : null)
                                .memberCount(accountBook.getMemberCount())
                                .description(accountBook.getDescription())
                                .startDate(accountBook.getStartDate())
                                .endDate(accountBook.getEndDate())
                                .isActive(accountBook.getIsActive())
                                .createdAt(accountBook.getCreatedAt())
                                .members(memberInfos)
                                .build();
        }
}
