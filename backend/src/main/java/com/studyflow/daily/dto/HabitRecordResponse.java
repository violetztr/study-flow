package com.studyflow.daily.dto;

import com.studyflow.daily.HabitRecord;

import java.time.LocalDate;

public record HabitRecordResponse(
        Long id,
        Long habitId,
        LocalDate recordDate,
        Boolean completed
) {
    public static HabitRecordResponse from(HabitRecord record) {
        return new HabitRecordResponse(
                record.getId(),
                record.getHabitId(),
                record.getRecordDate(),
                Boolean.TRUE.equals(record.getCompleted())
        );
    }
}
