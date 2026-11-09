package com.workmates.posting.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.Role;
import com.workmates.posting.dto.PostingDetailResponse;
import com.workmates.posting.service.PostingSearchService;
import com.workmates.posting.service.PostingService;
import com.workmates.posting.service.StudentPostingDetailService;
import org.junit.jupiter.api.Test;

class PostingControllerStudentDetailTest {
    @Test void studentGetsPersonalisedDetailAndEmployerKeepsOriginalDetail() {
        PostingService postingService = mock(PostingService.class);
        PostingSearchService searchService = mock(PostingSearchService.class);
        StudentPostingDetailService studentDetails = mock(StudentPostingDetailService.class);
        PostingController controller = new PostingController(postingService, searchService, studentDetails);
        PostingDetailResponse studentResponse = new PostingDetailResponse();
        PostingDetailResponse employerResponse = new PostingDetailResponse();
        when(studentDetails.get(7L, 42L)).thenReturn(studentResponse);
        when(postingService.get(42L)).thenReturn(employerResponse);
        assertSame(studentResponse, controller.get(new CurrentUser(7L, Role.STUDENT), 42L).getData());
        assertSame(employerResponse, controller.get(new CurrentUser(9L, Role.EMPLOYER), 42L).getData());
        verify(studentDetails, times(1)).get(7L, 42L);
        verify(postingService, times(1)).get(42L);
    }
}
