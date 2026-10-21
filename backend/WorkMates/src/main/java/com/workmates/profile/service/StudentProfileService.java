package com.workmates.profile.service;

import com.workmates.account.service.AccountService;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.common.timeslot.TimeSlotRules;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.dto.StudentProfileRequest;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.dto.TagResponse;
import com.workmates.profile.entity.StudentProfile;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.repository.StudentProfileRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and saves a student's profile: details, skill tags, expected rate and free time slots. */
@Service
public class StudentProfileService {
    static final int MAX_TAGS = 20;

    private final StudentProfileRepository profiles;
    private final TagService tags;
    private final AccountService accounts;

    public StudentProfileService(StudentProfileRepository profiles, TagService tags, AccountService accounts) {
        this.profiles = profiles;
        this.tags = tags;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse get(Long studentId) {
        return toResponse(studentId, load(studentId));
    }

    /** Replaces the whole profile. Tags and time slots are replaced as a group. */
    @Transactional
    public StudentProfileResponse update(Long studentId, StudentProfileRequest request) {
        // Check everything first so a bad request changes nothing
        List<String> tagNames = tags.normalizeAll(request.getTags());
        if (tagNames.size() > MAX_TAGS) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "At most " + MAX_TAGS + " tags are allowed");
        }
        List<TimeSlot> slots = TimeSlotDto.toSlots(request.getTimeSlots());
        TimeSlotRules.validate(slots);

        StudentProfile profile = load(studentId);
        accounts.changeName(studentId, request.getName());
        profile.setPhone(blankToNull(request.getPhone()));
        profile.setMajor(blankToNull(request.getMajor()));
        profile.setYearOfStudy(blankToNull(request.getYearOfStudy()));
        profile.setExpectedHourlyRate(request.getExpectedHourlyRate());
        profile.getTags().clear();
        profile.getTags().addAll(tags.findOrCreate(tagNames));
        profile.getTimeSlots().clear();
        profile.getTimeSlots().addAll(slots);
        return toResponse(studentId, profile);
    }

    /** A profile is complete with at least one tag, an expected rate and at least one free time slot. */
    @Transactional(readOnly = true)
    public boolean isComplete(Long studentId) {
        return missingParts(load(studentId)).isEmpty();
    }

    /** The inputs matching needs from this student. */
    @Transactional(readOnly = true)
    public StudentMatchInput matchInput(Long studentId) {
        StudentProfile profile = load(studentId);
        Set<Long> tagIds = profile.getTags().stream().map(Tag::getId).collect(Collectors.toSet());
        return new StudentMatchInput(List.copyOf(profile.getTimeSlots()), tagIds, profile.getExpectedHourlyRate());
    }

    private StudentProfile load(Long studentId) {
        return profiles.findById(studentId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private List<String> missingParts(StudentProfile profile) {
        List<String> missing = new ArrayList<>();
        if (profile.getTags().isEmpty()) {
            missing.add("tags");
        }
        if (profile.getExpectedHourlyRate() == null) {
            missing.add("expectedHourlyRate");
        }
        if (profile.getTimeSlots().isEmpty()) {
            missing.add("timeSlots");
        }
        return missing;
    }

    private StudentProfileResponse toResponse(Long studentId, StudentProfile profile) {
        List<String> missing = missingParts(profile);
        StudentProfileResponse response = new StudentProfileResponse();
        response.setName(accounts.getUser(studentId).getName());
        response.setPhone(profile.getPhone());
        response.setMajor(profile.getMajor());
        response.setYearOfStudy(profile.getYearOfStudy());
        response.setExpectedHourlyRate(profile.getExpectedHourlyRate());
        response.setTags(profile.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName)).map(TagResponse::from).toList());
        response.setTimeSlots(profile.getTimeSlots().stream()
                .sorted(Comparator.comparingInt(TimeSlot::getDayOfWeek).thenComparingInt(TimeSlot::getStartMinute))
                .map(TimeSlotDto::from).toList());
        response.setComplete(missing.isEmpty());
        response.setMissing(missing);
        return response;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
