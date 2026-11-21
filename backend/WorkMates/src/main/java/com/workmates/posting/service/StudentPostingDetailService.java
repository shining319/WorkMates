package com.workmates.posting.service;

import com.workmates.application.entity.Application;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.matching.MatchScoreCalculator;
import com.workmates.posting.dto.MatchBreakdownResponse;
import com.workmates.posting.dto.PostingDetailResponse;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.service.StudentProfileService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US 3.4 student details. Extends, rather than replaces, the existing posting detail response. */
@Service
public class StudentPostingDetailService {
    private final PostingService postingService;
    private final PostingRepository postings;
    private final StudentProfileService students;
    private final ApplicationRepository applications;

    public StudentPostingDetailService(PostingService postingService, PostingRepository postings,
                                       StudentProfileService students, ApplicationRepository applications) {
        this.postingService = postingService;
        this.postings = postings;
        this.students = students;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public PostingDetailResponse get(Long studentId, Long postingId) {
        PostingDetailResponse response = postingService.get(postingId);
        Posting job = postings.findById(postingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        StudentMatchInput profile = students.matchInput(studentId); // re-read on every request
        var match = MatchScoreCalculator.calculate(profile, job);
        Optional<Application> application = applications
                .findFirstByPostingIdAndStudentIdOrderByCreatedAtDescIdDesc(postingId, studentId);
        List<TimeSlotDto> freeSlots = profile == null || profile.getTimeSlots() == null
                ? List.of() : profile.getTimeSlots().stream()
                    .sorted(Comparator.comparingInt(com.workmates.common.timeslot.TimeSlot::getDayOfWeek)
                            .thenComparingInt(com.workmates.common.timeslot.TimeSlot::getStartMinute))
                    .map(TimeSlotDto::from).toList();
        response.addStudentMatch(MatchBreakdownResponse.from(match), freeSlots,
                application.map(Application::getId).orElse(null),
                application.map(Application::getStatus).orElse(null), application.isPresent(),
                job.getStatus() == PostingStatus.OPEN);
        return response;
    }
}
