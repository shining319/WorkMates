package com.workmates.profile.repository;

import com.workmates.profile.entity.Tag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByNameIn(Collection<String> names);

    /** {@code prefix} must already have backslash, percent and underscore escaped with a backslash. */
    @Query("select t from Tag t where t.name like concat(:prefix, '%') escape '\\' order by t.name")
    List<Tag> findByPrefix(String prefix, Pageable pageable);
}
