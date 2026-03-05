package com.moneyflow.talmo.scheduler;

import com.moneyflow.talmo.domain.TalmoRecord;
import com.moneyflow.talmo.domain.TalmoUser;
import com.moneyflow.talmo.repository.TalmoProblemRepository;
import com.moneyflow.talmo.repository.TalmoRecordRepository;
import com.moneyflow.talmo.repository.TalmoUserRepository;
import com.moneyflow.talmo.service.KakaoMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 코테 알림 스케줄러
 * - 매일 오전 10시, 오후 3시에 미완료 유저에게 카카오 메시지 발송
 * - 타이핑 연습 OR 코테 문제 중 하나라도 안 했으면 알림
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TalmoScheduler {

    private final TalmoUserRepository userRepository;
    private final TalmoRecordRepository recordRepository;
    private final TalmoProblemRepository problemRepository;
    private final KakaoMessageService kakaoMessageService;

    // ===== 자극 메시지 템플릿 =====

    private static final List<String> MSG_MORNING_NOBODY = List.of(
            "☀️ 좋은 아침! 아직 아무도 시작 안 했어. 1등 자리 비어있다 👑",
            "🌅 새로운 하루! 오늘 첫 스타트 누가 끊을까? 🚀",
            "☀️ 아침이다! 아직 0명 참여… 지금이 찬스 💪");

    private static final List<String> MSG_MORNING_REMINDER = List.of(
            "☀️ 좋은 아침! 오늘 코테 한 문제 어때? 💪",
            "🌅 일어났으면 코테 한 문제! 아직 안 했잖아 😏",
            "☀️ 아침 루틴에 코테 추가! 오늘도 화이팅 🔥",
            "🌅 커피 마셨으면 코테도 한 문제 ☕");

    private static final List<String> MSG_AFTERNOON_SOME_DID = List.of(
            "🔥 벌써 몇 명은 끝냈는데… 너도 할 거지?",
            "📈 참여자 늘어나는 중! 너는 언제 시작해?",
            "👀 선두 그룹이 생겼다! 따라갈 거야?");

    private static final List<String> MSG_AFTERNOON_COMPARE = List.of(
            "😏 %s은(는) 벌써 완료했는데… 너는?",
            "👀 %s은(는) 이미 끝냈다! 지금 시작해볼까?",
            "🔥 현재 1등 %s! 바꿀 사람?");

    private static final List<String> MSG_ONLY_YOU_LEFT = List.of(
            "😱 다들 했는데… 너만 남았다!",
            "👀 마지막 1명 남음… 설마 너?",
            "😂 진짜 너만 안 했어! 빨리 해~",
            "👀 마지막 남은 사람 발견");

    private static final List<String> MSG_STREAK_ACTIVE = List.of(
            "🔥 현재 %d일 연속 달성 중! 끊지 마!",
            "📈 streak %d일 유지 중! 오늘도 이어가자!",
            "💪 %d일 연속 코테 성공! 오늘도 가자!",
            "🏆 %d일 연속 기록 중! 멈추면 아깝다!",
            "🚀 %d일 streak 진행 중! 오늘도 한 문제!");

    private static final List<String> MSG_STREAK_ZERO = List.of(
            "💀 연속 기록 0일… 오늘부터 다시 시작!",
            "🌱 새로운 streak 시작할 기회!",
            "🚀 오늘이 streak 1일차 될 수도 있다!",
            "📈 오늘부터 다시 streak 쌓아보자!",
            "🔥 오늘 풀면 streak 시작!");

    // ===== 스케줄 =====

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void morningReminder() {
        log.info("🌅 오전 알림 스케줄러 실행");
        checkAndNotify("morning");
    }

    @Scheduled(cron = "0 0 15 * * *", zone = "Asia/Seoul")
    public void afternoonReminder() {
        log.info("🌇 오후 알림 스케줄러 실행");
        checkAndNotify("afternoon");
    }

    public void checkAndNotify(String timeSlot) {
        // 1. 카카오 연동된 전체 유저 조회
        List<TalmoUser> kakaoUsers = userRepository.findByKakaoRefreshTokenIsNotNull();
        if (kakaoUsers.isEmpty()) {
            log.info("카카오 연동 유저 없음, 스킵");
            return;
        }

        // 2. 오늘 기록 확인
        LocalDateTime startOfDay = LocalDate.now().atTime(LocalTime.MIN);
        List<TalmoRecord> todayRecords = recordRepository.findTodayRecords(startOfDay);

        Set<Long> typingDoneUserIds = todayRecords.stream()
                .map(r -> r.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> problemDoneUserIds = problemRepository.findTodayProblems(startOfDay).stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        // 3. 하나라도 안 한 유저 필터
        List<TalmoUser> pendingUsers = kakaoUsers.stream()
                .filter(u -> !typingDoneUserIds.contains(u.getId()) || !problemDoneUserIds.contains(u.getId()))
                .toList();

        Set<String> completedNames = kakaoUsers.stream()
                .filter(u -> typingDoneUserIds.contains(u.getId()) && problemDoneUserIds.contains(u.getId()))
                .map(TalmoUser::getName)
                .collect(Collectors.toSet());

        // 4. 스트릭 계산
        Map<Long, Integer> streaks = calculateStreaks(kakaoUsers);

        // 5. 메시지 발송
        int sentCount = 0;
        for (TalmoUser user : pendingUsers) {
            String message = buildMessage(user, timeSlot, completedNames, pendingUsers.size(),
                    kakaoUsers.size(), streaks.getOrDefault(user.getId(), 0),
                    typingDoneUserIds.contains(user.getId()),
                    problemDoneUserIds.contains(user.getId()));

            if (kakaoMessageService.sendMessage(user, message)) {
                sentCount++;
            }
        }

        log.info("알림 발송 완료 - 전체: {}명, 미완료: {}명, 발송 성공: {}명",
                kakaoUsers.size(), pendingUsers.size(), sentCount);
    }

    private String buildMessage(TalmoUser user, String timeSlot,
            Set<String> completedNames, int pendingCount, int totalCount,
            int streak, boolean didTyping, boolean didProblem) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        // 메인 메시지
        if ("morning".equals(timeSlot)) {
            if (completedNames.isEmpty()) {
                sb.append(pickRandom(MSG_MORNING_NOBODY, random));
            } else {
                sb.append(pickRandom(MSG_MORNING_REMINDER, random));
            }
        } else {
            // 오후 — 상황별 메시지
            if (pendingCount == 1) {
                sb.append(pickRandom(MSG_ONLY_YOU_LEFT, random));
            } else if (!completedNames.isEmpty()) {
                String doneUser = completedNames.iterator().next();
                String template = pickRandom(MSG_AFTERNOON_COMPARE, random);
                sb.append(String.format(template, doneUser));
            } else {
                sb.append(pickRandom(MSG_AFTERNOON_SOME_DID, random));
            }
        }

        // 무엇을 안 했는지 표시
        sb.append("\n\n");
        if (!didTyping && !didProblem) {
            sb.append("📋 오늘 할 일:\n❌ 타이핑 연습\n❌ 코테 문제");
        } else if (!didTyping) {
            sb.append("📋 오늘 할 일:\n❌ 타이핑 연습\n✅ 코테 문제");
        } else {
            sb.append("📋 오늘 할 일:\n✅ 타이핑 연습\n❌ 코테 문제");
        }

        // 스트릭 표시
        if (streak > 0) {
            String template = pickRandom(MSG_STREAK_ACTIVE, random);
            sb.append("\n\n").append(String.format(template, streak));
        } else {
            sb.append("\n\n").append(pickRandom(MSG_STREAK_ZERO, random));
        }

        return sb.toString();
    }

    /**
     * 각 유저의 연속 달성 일수 계산
     * - 타이핑과 코테 모두 한 날만 카운트
     */
    private Map<Long, Integer> calculateStreaks(List<TalmoUser> users) {
        Map<Long, Integer> result = new HashMap<>();

        for (TalmoUser user : users) {
            int streak = 0;
            LocalDate checkDate = LocalDate.now().minusDays(1); // 어제부터 체크

            while (true) {
                LocalDateTime dayStart = checkDate.atTime(LocalTime.MIN);
                LocalDateTime dayEnd = checkDate.atTime(LocalTime.MAX);

                boolean hadTyping = recordRepository.existsByUserIdAndCompletedAtBetween(
                        user.getId(), dayStart, dayEnd);
                boolean hadProblem = problemRepository.existsByUserIdAndCreatedAtBetween(
                        user.getId(), dayStart, dayEnd);

                if (hadTyping || hadProblem) {
                    streak++;
                    checkDate = checkDate.minusDays(1);
                } else {
                    break;
                }
            }

            result.put(user.getId(), streak);
        }

        return result;
    }

    private String pickRandom(List<String> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }
}
