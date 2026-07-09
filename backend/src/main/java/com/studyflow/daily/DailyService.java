package com.studyflow.daily;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.daily.dto.DailyPlanRequest;
import com.studyflow.daily.dto.DailyPlanResponse;
import com.studyflow.daily.dto.HabitRecordRequest;
import com.studyflow.daily.dto.HabitRecordResponse;
import com.studyflow.daily.dto.HabitRequest;
import com.studyflow.daily.dto.HabitResponse;
import com.studyflow.daily.dto.JournalRequest;
import com.studyflow.daily.dto.JournalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyService {
    private final DailyPlanMapper dailyPlanMapper;
    private final JournalMapper journalMapper;
    private final HabitMapper habitMapper;
    private final HabitRecordMapper habitRecordMapper;

    public DailyService(
            DailyPlanMapper dailyPlanMapper,
            JournalMapper journalMapper,
            HabitMapper habitMapper,
            HabitRecordMapper habitRecordMapper
    ) {
        this.dailyPlanMapper = dailyPlanMapper;
        this.journalMapper = journalMapper;
        this.habitMapper = habitMapper;
        this.habitRecordMapper = habitRecordMapper;
    }

    @Transactional
    public DailyPlanResponse createPlan(Long userId, DailyPlanRequest request) {
        DailyPlan plan = new DailyPlan();
        plan.setUserId(userId);
        plan.setPlanDate(request.planDate());
        plan.setTitle(request.title());
        plan.setDescription(request.description());
        plan.setStatus(normalizePlanStatus(request.status()));
        dailyPlanMapper.insert(plan);
        return DailyPlanResponse.from(plan);
    }

    public List<DailyPlanResponse> listPlans(Long userId, LocalDate date) {
        return dailyPlanMapper.selectList(new LambdaQueryWrapper<DailyPlan>()
                        .eq(DailyPlan::getUserId, userId)
                        .eq(DailyPlan::getPlanDate, date)
                        .orderByDesc(DailyPlan::getId))
                .stream()
                .map(DailyPlanResponse::from)
                .toList();
    }

    @Transactional
    public DailyPlanResponse updatePlan(Long userId, Long planId, DailyPlanRequest request) {
        DailyPlan plan = requireOwnedPlan(userId, planId);
        plan.setPlanDate(request.planDate());
        plan.setTitle(request.title());
        plan.setDescription(request.description());
        plan.setStatus(normalizePlanStatus(request.status()));
        dailyPlanMapper.updateById(plan);
        return DailyPlanResponse.from(plan);
    }

    @Transactional
    public JournalResponse upsertJournal(Long userId, JournalRequest request) {
        Journal journal = journalMapper.selectOne(new LambdaQueryWrapper<Journal>()
                .eq(Journal::getUserId, userId)
                .eq(Journal::getJournalDate, request.journalDate()));
        if (journal == null) {
            journal = new Journal();
            journal.setUserId(userId);
            journal.setJournalDate(request.journalDate());
            journal.setMood(request.mood());
            journal.setContent(request.content());
            journalMapper.insert(journal);
            return JournalResponse.from(journal);
        }

        journal.setMood(request.mood());
        journal.setContent(request.content());
        journalMapper.updateById(journal);
        return JournalResponse.from(journal);
    }

    public JournalResponse getJournal(Long userId, LocalDate date) {
        Journal journal = journalMapper.selectOne(new LambdaQueryWrapper<Journal>()
                .eq(Journal::getUserId, userId)
                .eq(Journal::getJournalDate, date));
        return journal == null ? null : JournalResponse.from(journal);
    }

    @Transactional
    public HabitResponse createHabit(Long userId, HabitRequest request) {
        Habit habit = new Habit();
        habit.setUserId(userId);
        habit.setName(request.name());
        habit.setDescription(request.description());
        habit.setActive(true);
        habitMapper.insert(habit);
        return HabitResponse.from(habit);
    }

    public List<HabitResponse> listHabits(Long userId) {
        return habitMapper.selectList(new LambdaQueryWrapper<Habit>()
                        .eq(Habit::getUserId, userId)
                        .eq(Habit::getActive, true)
                        .orderByDesc(Habit::getId))
                .stream()
                .map(HabitResponse::from)
                .toList();
    }

    @Transactional
    public HabitRecordResponse upsertHabitRecord(Long userId, Long habitId, HabitRecordRequest request) {
        Habit habit = requireOwnedHabit(userId, habitId);
        HabitRecord record = habitRecordMapper.selectOne(new LambdaQueryWrapper<HabitRecord>()
                .eq(HabitRecord::getHabitId, habit.getId())
                .eq(HabitRecord::getRecordDate, request.recordDate()));
        if (record == null) {
            record = new HabitRecord();
            record.setHabitId(habit.getId());
            record.setUserId(userId);
            record.setRecordDate(request.recordDate());
            record.setCompleted(Boolean.TRUE.equals(request.completed()));
            habitRecordMapper.insert(record);
            return HabitRecordResponse.from(record);
        }

        record.setCompleted(Boolean.TRUE.equals(request.completed()));
        habitRecordMapper.updateById(record);
        return HabitRecordResponse.from(record);
    }

    private DailyPlan requireOwnedPlan(Long userId, Long planId) {
        DailyPlan plan = dailyPlanMapper.selectOne(new LambdaQueryWrapper<DailyPlan>()
                .eq(DailyPlan::getId, planId)
                .eq(DailyPlan::getUserId, userId));
        if (plan == null) {
            throw new BusinessException(404, "日常计划不存在");
        }
        return plan;
    }

    private Habit requireOwnedHabit(Long userId, Long habitId) {
        Habit habit = habitMapper.selectOne(new LambdaQueryWrapper<Habit>()
                .eq(Habit::getId, habitId)
                .eq(Habit::getUserId, userId)
                .eq(Habit::getActive, true));
        if (habit == null) {
            throw new BusinessException(404, "习惯不存在");
        }
        return habit;
    }

    private String normalizePlanStatus(String status) {
        if (status == null || status.isBlank()) {
            return "TODO";
        }
        if (status.equals("TODO") || status.equals("DOING") || status.equals("DONE")) {
            return status;
        }
        throw new BusinessException(400, "日常计划状态不正确");
    }
}
