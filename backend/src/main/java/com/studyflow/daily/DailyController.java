package com.studyflow.daily;

import com.studyflow.common.ApiResponse;
import com.studyflow.daily.dto.DailyPlanRequest;
import com.studyflow.daily.dto.DailyPlanResponse;
import com.studyflow.daily.dto.HabitRecordRequest;
import com.studyflow.daily.dto.HabitRecordResponse;
import com.studyflow.daily.dto.HabitRequest;
import com.studyflow.daily.dto.HabitResponse;
import com.studyflow.daily.dto.JournalRequest;
import com.studyflow.daily.dto.JournalResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/daily")
public class DailyController {
    private final DailyService dailyService;

    public DailyController(DailyService dailyService) {
        this.dailyService = dailyService;
    }

    @GetMapping("/plans")
    public ApiResponse<List<DailyPlanResponse>> listPlans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(dailyService.listPlans(principal.userId(), date));
    }

    @PostMapping("/plans")
    public ApiResponse<DailyPlanResponse> createPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DailyPlanRequest request
    ) {
        return ApiResponse.success(dailyService.createPlan(principal.userId(), request));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<DailyPlanResponse> updatePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DailyPlanRequest request
    ) {
        return ApiResponse.success(dailyService.updatePlan(principal.userId(), id, request));
    }

    @GetMapping("/journal")
    public ApiResponse<JournalResponse> getJournal(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(dailyService.getJournal(principal.userId(), date));
    }

    @PutMapping("/journal")
    public ApiResponse<JournalResponse> upsertJournal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JournalRequest request
    ) {
        return ApiResponse.success(dailyService.upsertJournal(principal.userId(), request));
    }

    @GetMapping("/habits")
    public ApiResponse<List<HabitResponse>> listHabits(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(dailyService.listHabits(principal.userId()));
    }

    @PostMapping("/habits")
    public ApiResponse<HabitResponse> createHabit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody HabitRequest request
    ) {
        return ApiResponse.success(dailyService.createHabit(principal.userId(), request));
    }

    @PutMapping("/habits/{id}/records")
    public ApiResponse<HabitRecordResponse> upsertHabitRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody HabitRecordRequest request
    ) {
        return ApiResponse.success(dailyService.upsertHabitRecord(principal.userId(), id, request));
    }
}
