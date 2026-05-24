package com.lakeserl.product_service.controller;

import com.lakeserl.product_service.dto.request.GenerateRoutineRequest;
import com.lakeserl.product_service.dto.response.ApiResponse;
import com.lakeserl.product_service.dto.response.RoutineResponse;
import com.lakeserl.product_service.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routines")
@RequiredArgsConstructor
@Tag(name = "Routine Builder", description = "Endpoints for generating skincare routines")
public class RoutineController {

    private final RoutineService routineService;

    @PostMapping("/generate")
    @Operation(
        summary = "Generate morning and evening skincare routine",
        description = "Generates a personalized morning and evening skincare routine matching skin profile signals. " +
                      "If the request body is empty, it attempts to fetch the profile of the authenticated user."
    )
    public ApiResponse<RoutineResponse> generateRoutine(
            @RequestBody(required = false) GenerateRoutineRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        RoutineResponse response = routineService.generateRoutine(request, userId);
        return ApiResponse.ok("Routine generated successfully", response);
    }
}
