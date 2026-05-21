package com.lakeserl.notification_service.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakeserl.notification_service.service.UnsubscribeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Public one-click email unsubscribe landing page. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Unsubscribe", description = "Email unsubscribe landing page")
public class UnsubscribeController {

    private final UnsubscribeService unsubscribeService;

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Redeem an email unsubscribe token")
    public String unsubscribe(@RequestParam String token) {
        String category = unsubscribeService.consume(token);
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"><title>CalmSKIN</title></head>
                <body style="font-family:Arial,sans-serif;text-align:center;padding:48px;">
                  <h2>CalmSKIN</h2>
                  <p>You have been unsubscribed from <strong>%s</strong> emails.</p>
                  <p>You can change this anytime in your notification preferences.</p>
                </body>
                </html>
                """.formatted(category == null ? "marketing" : category.toLowerCase());
    }
}
