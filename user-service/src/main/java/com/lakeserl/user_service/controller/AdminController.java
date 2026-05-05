package com.lakeserl.user_service.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.entity.AuditLog;
import com.lakeserl.user_service.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin user management APIs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<ApiResponse<?>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<?>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getUserById(id)));
    }

    @PatchMapping("/{id}/ban")
    @Operation(summary = "Ban user")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable UUID id) {
        adminService.banUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User banned"));
    }

    @PatchMapping("/{id}/unban")
    @Operation(summary = "Unban user")
    public ResponseEntity<ApiResponse<Void>> unbanUser(@PathVariable UUID id) {
        adminService.unbanUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User unbanned"));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable UUID id, @RequestParam String role) {
        adminService.updateRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok("Role updated"));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Force reset user password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable UUID id) {
        adminService.forceResetPassword(id);
        return ResponseEntity.ok(ApiResponse.ok("Password reset email sent"));
    }

    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "Get user audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAuditLogs(id, PageRequest.of(page, size))));
    }

    @GetMapping("/stats/summary")
    @Operation(summary = "Get user statistics summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsSummary() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStatsSummary()));
    }

    @GetMapping("/export")
    @Operation(summary = "Export all users as CSV")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = adminService.exportUsersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
