package com.moneyflow.domain.accountbook;

import com.moneyflow.dto.request.CreateAccountBookRequest;
import com.moneyflow.dto.response.AccountBookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 장부 API 컨트롤러
 *
 * 기능:
 * - 장부 생성 (커플 생활비, 여행 등)
 * - 장부 목록 조회
 * - 장부 상세 조회
 * - 멤버 관리
 *
 * 엔드포인트:
 * - POST /api/account-books - 장부 생성
 * - GET /api/account-books - 내 장부 목록
 * - GET /api/account-books/{id} - 장부 상세
 * - GET /api/account-books/{id}/members - 멤버 목록
 * - POST /api/account-books/{id}/members - 멤버 추가
 * - DELETE /api/account-books/{id} - 장부 비활성화
 */
@Tag(name = "AccountBook", description = "장부 관리 API")
@RestController
@RequestMapping("/api/account-books")
@RequiredArgsConstructor
public class AccountBookController {

        private final AccountBookService accountBookService;

        /**
         * 장부 생성
         */
        @Operation(summary = "장부 생성", description = "커플 생활비, 여행 등 새 장부를 생성합니다")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "생성 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패")
        })
        @PostMapping
        public ResponseEntity<AccountBookResponse> createAccountBook(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Valid @RequestBody CreateAccountBookRequest request) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                AccountBookResponse response = accountBookService.createAccountBook(userId, request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * 내 장부 목록 조회
         */
        @Operation(summary = "내 장부 목록", description = "사용자가 참여 중인 장부 목록을 조회합니다")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패")
        })
        @GetMapping
        public ResponseEntity<List<AccountBookResponse>> getMyAccountBooks(
                        @AuthenticationPrincipal UserDetails userDetails) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                List<AccountBookResponse> response = accountBookService.getMyAccountBooks(userId);
                return ResponseEntity.ok(response);
        }

        /**
         * 장부 상세 조회
         */
        @Operation(summary = "장부 상세 조회", description = "장부의 상세 정보를 조회합니다")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "404", description = "장부 없음")
        })
        @GetMapping("/{accountBookId}")
        public ResponseEntity<AccountBookResponse> getAccountBook(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable UUID accountBookId) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                AccountBookResponse response = accountBookService.getAccountBook(userId, accountBookId);
                return ResponseEntity.ok(response);
        }

        /**
         * 장부 멤버 목록 조회
         */
        @Operation(summary = "장부 멤버 목록", description = "장부의 멤버 목록을 조회합니다")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패 또는 권한 없음"),
                        @ApiResponse(responseCode = "404", description = "장부 없음")
        })
        @GetMapping("/{accountBookId}/members")
        public ResponseEntity<List<AccountBookResponse.MemberInfo>> getMembers(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable UUID accountBookId) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                List<AccountBookResponse.MemberInfo> members = accountBookService.getMembers(userId, accountBookId);
                return ResponseEntity.ok(members);
        }

        /**
         * 멤버 추가
         */
        @Operation(summary = "멤버 추가", description = "장부에 새 멤버를 추가합니다 (OWNER만 가능)")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "추가 성공"),
                        @ApiResponse(responseCode = "400", description = "이미 멤버임"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "권한 없음")
        })
        @PostMapping("/{accountBookId}/members")
        public ResponseEntity<Map<String, String>> addMember(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable UUID accountBookId,
                        @RequestParam UUID newMemberId) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                accountBookService.addMember(accountBookId, userId, newMemberId);

                Map<String, String> response = new HashMap<>();
                response.put("message", "멤버가 추가되었습니다");
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{accountBookId}")
        public ResponseEntity<AccountBookResponse> updateAccountBook(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable UUID accountBookId,
                        @Valid @RequestBody com.moneyflow.dto.request.UpdateAccountBookRequest request) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                AccountBookResponse response = accountBookService.updateAccountBook(userId, accountBookId, request);
                return ResponseEntity.ok(response);
        }

        /**
         * 장부 비활성화 (삭제)
         */
        @Operation(summary = "장부 비활성화", description = "장부를 비활성화합니다 (OWNER만 가능)")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "삭제 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "권한 없음")
        })
        @DeleteMapping("/{accountBookId}")
        public ResponseEntity<Map<String, String>> deactivateAccountBook(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable UUID accountBookId) {
                UUID userId = UUID.fromString(userDetails.getUsername());
                accountBookService.deactivateAccountBook(userId, accountBookId);

                Map<String, String> response = new HashMap<>();
                response.put("message", "장부가 삭제되었습니다");
                return ResponseEntity.ok(response);
        }

        /**
         * 초기 잔액 수정
         */
        @Operation(summary = "초기 잔액 수정", description = "장부의 초기 잔액을 수정합니다")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "수정 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "권한 없음"),
                        @ApiResponse(responseCode = "404", description = "장부 없음")
        })
        @PatchMapping("/{accountBookId}/initial-balance")
        public ResponseEntity<Map<String, Object>> updateInitialBalance(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @PathVariable UUID accountBookId,
                        @RequestBody Map<String, Object> request) {
                UUID userId = UUID.fromString(userDetails.getUsername());

                Object initialBalanceObj = request.get("initialBalance");
                java.math.BigDecimal initialBalance;
                if (initialBalanceObj instanceof Number) {
                        initialBalance = java.math.BigDecimal.valueOf(((Number) initialBalanceObj).doubleValue());
                } else {
                        initialBalance = new java.math.BigDecimal(initialBalanceObj.toString());
                }

                accountBookService.updateInitialBalance(userId, accountBookId, initialBalance);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "초기 잔액이 수정되었습니다");
                response.put("initialBalance", initialBalance);
                return ResponseEntity.ok(response);
        }
}
