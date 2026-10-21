package com.workmates.profile.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.repository.TagRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tag rules shared by student profiles and postings: normalising names, find-or-create, suggestions. */
@Service
public class TagService {
    static final int MAX_NAME_LENGTH = 50;
    static final int MAX_SUGGESTIONS = 10;

    private final TagRepository tags;

    public TagService(TagRepository tags) {
        this.tags = tags;
    }

    /** Trims and lowercases one name. A blank name or one over 50 characters is rejected. */
    public String normalize(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tag cannot be empty");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Tag is longer than " + MAX_NAME_LENGTH + " characters: " + normalized);
        }
        return normalized;
    }

    /** Normalises every name and drops duplicates, keeping the first occurrence's order. */
    public List<String> normalizeAll(List<String> names) {
        return names.stream().map(this::normalize).distinct().toList();
    }

    /** Returns one tag per distinct name, creating the ones that do not exist yet. */
    @Transactional
    public List<Tag> findOrCreate(List<String> names) {
        List<String> normalized = normalizeAll(names);
        Map<String, Tag> existing = tags.findByNameIn(normalized).stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));
        return normalized.stream()
                .map(name -> existing.containsKey(name) ? existing.get(name) : tags.save(new Tag(name)))
                .toList();
    }

    /** Up to 10 existing tags whose name starts with {@code q}, ignoring case. */
    @Transactional(readOnly = true)
    public List<Tag> suggest(String q) {
        String prefix = (q == null ? "" : q.trim().toLowerCase(Locale.ROOT))
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return tags.findByPrefix(prefix, PageRequest.of(0, MAX_SUGGESTIONS));
    }
}
