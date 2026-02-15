package com.wiki.monowiki.wiki.repo;

import com.wiki.monowiki.wiki.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByNameIgnoreCase(String name);
}
