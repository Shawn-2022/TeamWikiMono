package com.wiki.monowiki.wiki.repo;

import com.wiki.monowiki.wiki.model.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    Optional<Space> findBySpaceKey(String spaceKey);

    boolean existsBySpaceKey(String spaceKey);
}
