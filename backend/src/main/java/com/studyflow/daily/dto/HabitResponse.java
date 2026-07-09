package com.studyflow.daily.dto;

import com.studyflow.daily.Habit;

public record HabitResponse(
        Long id,
        String name,
        String description,
        Boolean active
) {
    public static HabitResponse from(Habit habit) {
        return new HabitResponse(
                habit.getId(),
                habit.getName(),
                habit.getDescription(),
                Boolean.TRUE.equals(habit.getActive())
        );
    }
}
