package com.github.stella.springaoplab.tx;

import com.github.stella.springaoplab.common.api.ApiResponse;
import com.github.stella.springaoplab.post.repository.PostRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tx-demo")
public class TxDemoController {

    private final TxBoundaryService txService;
    private final PostRepository postRepository;

    public TxDemoController(TxBoundaryService txService, PostRepository postRepository) {
        this.txService = txService;
        this.postRepository = postRepository;
    }

    private record CountResult(String prefix, long count) {}

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<CountResult>> count(@RequestParam String prefix, HttpServletRequest request) {
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }

    // 1) RuntimeException → rollback
    @PostMapping("/runtime")
    public ResponseEntity<ApiResponse<CountResult>> runtime(@RequestParam String prefix, HttpServletRequest request) {
        try {
            txService.createThenRuntimeException(prefix);
        } catch (RuntimeException ignored) {
            // after rollback
        }
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }

    // 2) Checked Exception → commit by default
    @PostMapping("/checked")
    public ResponseEntity<ApiResponse<CountResult>> checked(@RequestParam String prefix, HttpServletRequest request) {
        try {
            txService.createThenCheckedException(prefix);
        } catch (Exception ignored) {
            // after commit
        }
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }

    // 3) Checked + rollbackFor → rollback
    @PostMapping("/checked-rollback")
    public ResponseEntity<ApiResponse<CountResult>> checkedRollback(@RequestParam String prefix, HttpServletRequest request) {
        try {
            txService.createThenCheckedExceptionWithRollback(prefix);
        } catch (Exception ignored) {
            // after rollback
        }
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }

    // 4) REQUIRED → REQUIRED (same Tx) inner fail → all rollback
    @PostMapping("/propagation/required-required")
    public ResponseEntity<ApiResponse<CountResult>> requiredRequired(@RequestParam String prefix, HttpServletRequest request) {
        try {
            txService.outerRequired_calls_innerRequired_fail(prefix);
        } catch (RuntimeException ignored) {
            // after rollback
        }
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }

    // 5) REQUIRED → REQUIRES_NEW inner fails but outer catches → outer commits, inner rolled back
    @PostMapping("/propagation/requires-new")
    public ResponseEntity<ApiResponse<CountResult>> requiresNew(@RequestParam String prefix, HttpServletRequest request) {
        txService.outerRequired_calls_innerRequiresNew_fail_but_catch(prefix);
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }

    // 6) readOnly write attempt → usually not persisted
    @PostMapping("/readonly-write")
    public ResponseEntity<ApiResponse<CountResult>> readOnlyWrite(@RequestParam String prefix, HttpServletRequest request) {
        txService.readOnlyWriteAttempt(prefix);
        long count = postRepository.countByTitleStartingWith(prefix);
        return ResponseEntity.ok(ApiResponse.success(new CountResult(prefix, count), request.getRequestURI()));
    }
}
