package com.moneyflow.talmo.controller;

import com.moneyflow.talmo.dto.*;
import com.moneyflow.talmo.service.TalmoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/talmo")
@RequiredArgsConstructor
public class TalmoController {

    private final TalmoService talmoService;

    // ===== 유저 =====

    @PostMapping("/users")
    public ResponseEntity<TalmoUserResponse> createUser(@Valid @RequestBody TalmoUserRequest request) {
        return ResponseEntity.ok(talmoService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<TalmoUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody TalmoUserRequest request) {
        return ResponseEntity.ok(talmoService.updateUserName(id, request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<TalmoUserResponse>> getUsers() {
        return ResponseEntity.ok(talmoService.getAllUsers());
    }

    // ===== 기록 =====

    @PostMapping("/records")
    public ResponseEntity<TalmoRecordResponse> createRecord(@Valid @RequestBody TalmoRecordRequest request) {
        return ResponseEntity.ok(talmoService.createRecord(request));
    }

    @GetMapping("/records")
    public ResponseEntity<List<TalmoRecordResponse>> getRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(talmoService.getRecords(userId, limit));
    }

    @GetMapping("/records/today")
    public ResponseEntity<TalmoTodayResponse> getTodaySummary() {
        return ResponseEntity.ok(talmoService.getTodaySummary());
    }

    // ===== 문제 =====

    @PostMapping("/problems")
    public ResponseEntity<TalmoProblemResponse> createProblem(@Valid @RequestBody TalmoProblemRequest request) {
        return ResponseEntity.ok(talmoService.createProblem(request));
    }

    @GetMapping("/problems")
    public ResponseEntity<List<TalmoProblemResponse>> getProblems(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(talmoService.getProblems(userId, limit));
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<TalmoProblemResponse> getProblem(@PathVariable Long id) {
        return ResponseEntity.ok(talmoService.getProblem(id));
    }
}
