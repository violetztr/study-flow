package com.studyflow.daily.dto;

import com.studyflow.daily.Journal;

import java.time.LocalDate;

public record JournalResponse(
        Long id,
        LocalDate journalDate,
        String mood,
        String content
) {
    public static JournalResponse from(Journal journal) {
        return new JournalResponse(
                journal.getId(),
                journal.getJournalDate(),
                journal.getMood(),
                journal.getContent()
        );
    }
}
