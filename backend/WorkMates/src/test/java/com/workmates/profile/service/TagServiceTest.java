package com.workmates.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.repository.TagRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class TagServiceTest {

    private final TagRepository repository = mock(TagRepository.class);
    private final TagService service = new TagService(repository);

    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("java", service.normalize("  JaVa "));
    }

    @Test
    void normalizeRejectsBlank() {
        for (String bad : new String[] {"", "   ", null}) {
            BusinessException e = assertThrows(BusinessException.class, () -> service.normalize(bad));
            assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
        }
    }

    @Test
    void normalizeAllowsFiftyCharactersButNotFiftyOne() {
        assertEquals(50, service.normalize("a".repeat(50)).length());
        BusinessException e = assertThrows(BusinessException.class, () -> service.normalize("a".repeat(51)));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
    }

    @Test
    void normalizeAllDropsCaseInsensitiveDuplicatesKeepingOrder() {
        assertEquals(List.of("java", "english", "sql"),
                service.normalizeAll(List.of("Java", "English", " JAVA", "SQL", "english")));
    }

    @Test
    void findOrCreateReusesExistingAndCreatesMissing() {
        Tag java = new Tag("java");
        when(repository.findByNameIn(List.of("java", "sql"))).thenReturn(List.of(java));
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Tag> result = service.findOrCreate(List.of("Java", "SQL", "java"));

        assertEquals(2, result.size());
        assertSame(java, result.get(0));
        assertEquals("sql", result.get(1).getName());
        ArgumentCaptor<Tag> saved = ArgumentCaptor.forClass(Tag.class);
        verify(repository, times(1)).save(saved.capture());
        assertEquals("sql", saved.getValue().getName());
    }

    @Test
    void findOrCreateDoesNotSaveWhenAllExist() {
        Tag java = new Tag("java");
        when(repository.findByNameIn(List.of("java"))).thenReturn(List.of(java));

        service.findOrCreate(List.of("JAVA"));

        verify(repository, never()).save(any());
    }

    @Test
    void suggestLowercasesAndAsksForTenAtMost() {
        when(repository.findByPrefix(eq("ja"), any(Pageable.class))).thenReturn(List.of(new Tag("java")));

        List<Tag> result = service.suggest("  JA ");

        assertEquals(1, result.size());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByPrefix(eq("ja"), page.capture());
        assertEquals(10, page.getValue().getPageSize());
    }

    @Test
    void suggestEscapesLikeWildcards() {
        service.suggest("100%_a\\b");

        verify(repository).findByPrefix(eq("100\\%\\_a\\\\b"), any(Pageable.class));
    }

    @Test
    void suggestWithNoTextUsesEmptyPrefix() {
        service.suggest(null);

        verify(repository).findByPrefix(eq(""), any(Pageable.class));
    }
}
