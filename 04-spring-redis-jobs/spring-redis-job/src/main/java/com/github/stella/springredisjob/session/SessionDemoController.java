package com.github.stella.springredisjob.session;

import com.github.stella.springredisjob.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
public class SessionDemoController {

    @PostMapping("/put")
    public ResponseEntity<ApiResponse<String>> put(
            @RequestParam String key,
            @RequestParam String value,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(true);
        session.setAttribute(key, value);
        return ResponseEntity.ok(ApiResponse.success("OK", request.getRequestURI()));
    }

    @GetMapping("/get")
    public ResponseEntity<ApiResponse<String>> get(
            @RequestParam String key,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        String value = session == null ? null : (String) session.getAttribute(key);
        return ResponseEntity.ok(ApiResponse.success(value, request.getRequestURI()));
    }
}
