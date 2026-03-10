package com.moneyflow.talmo.service;

import com.moneyflow.talmo.domain.TalmoProblem;
import com.moneyflow.talmo.domain.TalmoProblemAnalysis;
import com.moneyflow.talmo.domain.TalmoRecord;
import com.moneyflow.talmo.domain.TalmoUser;
import com.moneyflow.talmo.config.TalmoAdminPolicy;
import com.moneyflow.talmo.dto.*;
import com.moneyflow.talmo.repository.TalmoProblemAnalysisRepository;
import com.moneyflow.talmo.repository.TalmoProblemRepository;
import com.moneyflow.talmo.repository.TalmoRecordRepository;
import com.moneyflow.talmo.repository.TalmoUserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TalmoService {

    private final TalmoUserRepository userRepository;
    private final TalmoRecordRepository recordRepository;
    private final TalmoProblemRepository problemRepository;
    private final TalmoProblemAnalysisRepository problemAnalysisRepository;
    private final KakaoMessageService kakaoMessageService;
    private final TalmoAdminPolicy talmoAdminPolicy;

    // ===== 유저 =====

    @Transactional
    public TalmoUserResponse createUser(TalmoUserRequest request) {
        if (userRepository.existsByName(request.getName())) {
            // 이미 존재하면 기존 유저 반환
            TalmoUser existing = userRepository.findByName(request.getName())
                    .orElseThrow();
            return TalmoUserResponse.from(existing);
        }

        TalmoUser user = TalmoUser.builder()
                .name(request.getName())
                .build();
        userRepository.save(user);
        return TalmoUserResponse.from(user);
    }

    @Transactional
    public TalmoUserResponse updateUserName(Long userId, TalmoUserRequest request) {
        TalmoUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userId));

        // 변경할 이름이 이미 다른 유저가 사용 중인지 체크
        userRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다: " + request.getName());
            }
        });

        user.updateName(request.getName());
        return TalmoUserResponse.from(user);
    }

    public List<TalmoUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(TalmoUserResponse::from)
                .collect(Collectors.toList());
    }

    // ===== 기록 =====

    @Transactional
    public TalmoRecordResponse createRecord(TalmoRecordRequest request) {
        TalmoUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + request.getUserId()));

        TalmoRecord record = TalmoRecord.builder()
                .user(user)
                .topic(request.getTopic())
                .timeDisplay(request.getTime())
                .timeMs(request.getTimeMs())
                .errors(request.getErrors())
                .taskCount(request.getTaskCount())
                .build();

        recordRepository.save(record);
        return TalmoRecordResponse.from(record);
    }

    public List<TalmoRecordResponse> getRecords(Long userId, Integer limit) {
        List<TalmoRecord> records;

        if (userId != null) {
            records = recordRepository.findByUserIdOrderByCompletedAtDesc(userId);
        } else {
            records = recordRepository.findAllWithUser();
        }

        if (limit != null && limit > 0) {
            records = records.stream().limit(limit).collect(Collectors.toList());
        }

        return records.stream()
                .map(TalmoRecordResponse::from)
                .collect(Collectors.toList());
    }

    public TalmoTodayResponse getTodaySummary() {
        LocalDateTime startOfDay = LocalDate.now().atTime(LocalTime.MIN);
        List<TalmoRecord> todayRecords = recordRepository.findTodayRecords(startOfDay);

        Set<String> completedUsers = todayRecords.stream()
                .map(r -> r.getUser().getName())
                .collect(Collectors.toSet());

        List<String> allUsers = userRepository.findAll().stream()
                .map(TalmoUser::getName)
                .collect(Collectors.toList());

        List<String> pendingUsers = allUsers.stream()
                .filter(name -> !completedUsers.contains(name))
                .collect(Collectors.toList());

        return TalmoTodayResponse.builder()
                .completedUsers(List.copyOf(completedUsers))
                .pendingUsers(pendingUsers)
                .build();
    }

    // ===== 문제 =====

    @Transactional
    public TalmoProblemResponse createProblem(TalmoProblemRequest request) {
        TalmoUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + request.getUserId()));

        TalmoProblem problem = TalmoProblem.builder()
                .user(user)
                .title(request.getTitle())
                .source(request.getSource())
                .difficulty(request.getDifficulty())
                .problemUrl(request.getProblemUrl())
                .description(request.getDescription())
                .ioExample(request.getIoExample())
                .ioExplanation(request.getIoExplanation())
                .solutionCode(request.getSolutionCode())
                .solutionNote(request.getSolutionNote())
                .timeComplexity(request.getTimeComplexity())
                .spaceComplexity(request.getSpaceComplexity())
                .complexityReason(request.getComplexityReason())
                .complexityConfidence(request.getComplexityConfidence())
                .tags(request.getTags())
                .build();

        problemRepository.save(problem);
        notifyProblemSolved(problem);
        return toProblemResponse(problem);
    }

    @Transactional
    public TalmoProblemResponse updateProblem(Long problemId, TalmoProblemRequest request) {
        TalmoProblem problem = getProblemWithUser(problemId);

        if (!problem.getUser().getId().equals(request.getUserId())) {
            throw new IllegalArgumentException("본인이 등록한 풀이만 수정할 수 있습니다.");
        }

        problem.updateProblem(
                request.getTitle(),
                request.getSource(),
                request.getDifficulty(),
                request.getProblemUrl(),
                request.getDescription(),
                request.getIoExample(),
                request.getIoExplanation(),
                request.getSolutionCode(),
                request.getSolutionNote(),
                request.getTimeComplexity(),
                request.getSpaceComplexity(),
                request.getComplexityReason(),
                request.getComplexityConfidence(),
                request.getTags()
        );

        return toProblemResponse(problem);
    }

    public List<TalmoProblemResponse> getProblems(Long userId, Integer limit) {
        List<TalmoProblem> problems;

        if (userId != null) {
            problems = problemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            problems = problemRepository.findAllWithUser();
        }

        if (limit != null && limit > 0) {
            problems = problems.stream().limit(limit).collect(Collectors.toList());
        }

        return problems.stream()
                .map(this::toProblemResponse)
                .collect(Collectors.toList());
    }

    public TalmoProblemResponse getProblem(Long id) {
        TalmoProblem problem = getProblemWithUser(id);
        return toProblemResponse(problem);
    }

    public List<TalmoProblemResponse> getAdminProblems(Long adminUserId, String status, Integer limit) {
        TalmoUser admin = getAdminUser(adminUserId);
        List<TalmoProblem> problems = problemRepository.findAllWithUser();

        return problems.stream()
                .map(this::toProblemResponse)
                .filter(problem -> {
                    if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
                        return true;
                    }
                    if ("pending".equalsIgnoreCase(status)) {
                        return problem.getLatestAnalysis() == null;
                    }
                    return status.equalsIgnoreCase(
                            problem.getLatestAnalysis() == null ? "pending" : problem.getLatestAnalysis().getAnalysisStatus()
                    );
                })
                .limit(limit != null && limit > 0 ? limit : problems.size())
                .collect(Collectors.toList());
    }

    public TalmoProblemAnalysisPromptResponse generateAnalysisPrompt(Long problemId, Long adminUserId) {
        getAdminUser(adminUserId);
        TalmoProblem problem = getProblemWithUser(problemId);

        String prompt = """
                [AI ANALYSIS REQUEST]

                solutionId
                %d

                solutionVersion
                %d

                problemId
                %d

                problemTitle
                %s

                problemDescription
                ----------------
                %s

                ioExample
                ----------------
                %s

                ioExplanation
                ----------------
                %s

                user
                ----------------
                userId: %d
                nickname: %s

                userSolutionCode
                ----------------
                %s

                request
                ----------------
                1. 시간복잡도(Time Complexity)
                2. 공간복잡도(Space Complexity)
                3. 현재 코드 접근 방식 설명
                4. 더 효율적인 접근 가능 여부 (YES / NO)
                5. 가능하다면 더 나은 알고리즘 접근 방식 설명
                6. 개선 접근의 예상 시간복잡도
                7. 개선 접근의 예상 공간복잡도

                [AI ANALYSIS RESULT FORMAT]
                solutionId: %d
                solutionVersion: %d
                problemId: %d
                userId: %d
                timeComplexity:
                spaceComplexity:
                approachSummary:
                analysisText:
                improvementPossible:
                betterApproach:
                betterTimeComplexity:
                betterSpaceComplexity:
                """.formatted(
                problem.getId(),
                problem.getSolutionVersion(),
                problem.getId(),
                defaultText(problem.getTitle()),
                defaultText(problem.getDescription()),
                defaultText(problem.getIoExample()),
                defaultText(problem.getIoExplanation()),
                problem.getUser().getId(),
                problem.getUser().getName(),
                defaultText(problem.getSolutionCode()),
                problem.getId(),
                problem.getSolutionVersion(),
                problem.getId(),
                problem.getUser().getId()
        );

        return TalmoProblemAnalysisPromptResponse.builder()
                .problemId(problem.getId())
                .solutionVersion(problem.getSolutionVersion())
                .userId(problem.getUser().getId())
                .userName(problem.getUser().getName())
                .title(problem.getTitle())
                .prompt(prompt)
                .build();
    }

    @Transactional
    public TalmoProblemResponse saveProblemAnalysis(Long problemId, TalmoProblemAnalysisRequest request) {
        TalmoUser admin = getAdminUser(request.getAdminUserId());
        TalmoProblem problem = getProblemWithUser(problemId);

        int targetVersion = request.getSolutionVersion() != null
                ? request.getSolutionVersion()
                : problem.getSolutionVersion();

        if (!Integer.valueOf(problem.getSolutionVersion()).equals(targetVersion)) {
            throw new IllegalArgumentException("최신 풀이 버전과 맞지 않습니다. 새로고침 후 다시 시도해주세요.");
        }

        boolean improvementPossible = Boolean.TRUE.equals(request.getImprovementPossible());
        TalmoProblemAnalysis analysis = TalmoProblemAnalysis.builder()
                .problem(problem)
                .solutionVersion(targetVersion)
                .timeComplexity(request.getTimeComplexity())
                .spaceComplexity(request.getSpaceComplexity())
                .approachSummary(request.getApproachSummary())
                .analysisText(request.getAnalysisText())
                .improvementPossible(improvementPossible)
                .betterApproach(request.getBetterApproach())
                .betterTimeComplexity(request.getBetterTimeComplexity())
                .betterSpaceComplexity(request.getBetterSpaceComplexity())
                .promptSnapshot(request.getPromptSnapshot())
                .aiRawResponse(request.getAiRawResponse())
                .analysisStatus("SAVED")
                .notificationStatus("NOT_REQUIRED")
                .analyzedByName(admin.getName())
                .analyzedAt(LocalDateTime.now())
                .build();

        applyImprovementNotification(problem, analysis);
        problemAnalysisRepository.save(analysis);

        return toProblemResponse(problem);
    }

    private void notifyProblemSolved(TalmoProblem problem) {
        LocalDateTime startOfDay = LocalDate.now().atTime(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<TalmoUser> recipients = userRepository.findByKakaoRefreshTokenIsNotNull().stream()
                .filter(user -> !user.getId().equals(problem.getUser().getId()))
                .filter(user -> !problemRepository.existsByUserIdAndCreatedAtBetween(user.getId(), startOfDay, endOfDay))
                .toList();

        if (recipients.isEmpty()) {
            return;
        }

        String source = problem.getSource() == null || problem.getSource().isBlank()
                ? "코테 문제"
                : problem.getSource();

        String message = """
                👀 %s님이 방금 %s 문제를 풀었어요!

                📝 문제: %s
                %s

                아직 오늘 코테를 안 했다면 지금 한 문제 도전해봐요 🔥
                """.formatted(
                problem.getUser().getName(),
                source,
                problem.getTitle(),
                problem.getDifficulty() == null || problem.getDifficulty().isBlank()
                        ? ""
                        : "🏷️ 난이도: " + problem.getDifficulty());

        int sentCount = 0;
        for (TalmoUser recipient : recipients) {
            if (kakaoMessageService.sendMessage(recipient, message.trim())) {
                sentCount++;
            }
        }

        log.info("문제 등록 즉시 알림 발송 완료 - problemId: {}, recipients: {}, sent: {}",
                problem.getId(), recipients.size(), sentCount);
    }

    private void applyImprovementNotification(TalmoProblem problem, TalmoProblemAnalysis analysis) {
        if (!analysis.isImprovementPossible()) {
            analysis.markNotificationStatus("NOT_REQUIRED", null);
            return;
        }

        if (problemAnalysisRepository.existsByProblemIdAndSolutionVersionAndNotificationStatus(
                problem.getId(),
                analysis.getSolutionVersion(),
                "SENT")) {
            analysis.markNotificationStatus("SKIPPED_DUPLICATE", null);
            return;
        }

        if (!problem.getUser().hasKakaoToken()) {
            analysis.markNotificationStatus("SKIPPED_NO_KAKAO", null);
            return;
        }

        String message = """
                🧠 %s에 더 나은 방식이 있어요

                문제: %s
                추천 접근: %s
                개선 빅오: %s

                지금 다시 풀어보면서 개선 접근을 적용해보세요.
                """.formatted(
                getProblemAgeLabel(problem),
                problem.getTitle(),
                defaultText(analysis.getBetterApproach()),
                defaultText(analysis.getBetterTimeComplexity())
        );

        boolean sent = kakaoMessageService.sendMessage(problem.getUser(), message.trim());
        if (sent) {
            analysis.markNotificationStatus("SENT", LocalDateTime.now());
            return;
        }

        analysis.markNotificationStatus("FAILED", null);
    }

    private TalmoProblemResponse toProblemResponse(TalmoProblem problem) {
        TalmoProblemAnalysis latestAnalysis = problemAnalysisRepository
                .findTopByProblemIdAndSolutionVersionOrderByCreatedAtDesc(problem.getId(), problem.getSolutionVersion())
                .orElse(null);
        return TalmoProblemResponse.from(problem, TalmoProblemAnalysisResponse.from(latestAnalysis));
    }

    private TalmoProblem getProblemWithUser(Long problemId) {
        return problemRepository.findByIdWithUser(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + problemId));
    }

    private TalmoUser getAdminUser(Long adminUserId) {
        TalmoUser admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 유저를 찾을 수 없습니다: " + adminUserId));

        if (!talmoAdminPolicy.isAdmin(admin)) {
            throw new IllegalArgumentException("관리자 권한이 없습니다.");
        }
        return admin;
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "(없음)" : value;
    }

    private String getProblemAgeLabel(TalmoProblem problem) {
        LocalDate solvedDate = problem.getCreatedAt().toLocalDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(solvedDate, LocalDate.now());

        if (days <= 0) {
            return "오늘 푼 문제";
        }
        if (days == 1) {
            return "어제 푼 문제";
        }
        if (days < 7) {
            return days + "일 전에 푼 문제";
        }
        return "지난주에 푼 문제";
    }
}
