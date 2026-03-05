package com.moneyflow.talmo.service;

import com.moneyflow.talmo.domain.TalmoProblem;
import com.moneyflow.talmo.domain.TalmoRecord;
import com.moneyflow.talmo.domain.TalmoUser;
import com.moneyflow.talmo.dto.*;
import com.moneyflow.talmo.repository.TalmoProblemRepository;
import com.moneyflow.talmo.repository.TalmoRecordRepository;
import com.moneyflow.talmo.repository.TalmoUserRepository;
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
public class TalmoService {

    private final TalmoUserRepository userRepository;
    private final TalmoRecordRepository recordRepository;
    private final TalmoProblemRepository problemRepository;

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
                .tags(request.getTags())
                .build();

        problemRepository.save(problem);
        return TalmoProblemResponse.from(problem);
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
                .map(TalmoProblemResponse::from)
                .collect(Collectors.toList());
    }

    public TalmoProblemResponse getProblem(Long id) {
        TalmoProblem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + id));
        return TalmoProblemResponse.from(problem);
    }
}
