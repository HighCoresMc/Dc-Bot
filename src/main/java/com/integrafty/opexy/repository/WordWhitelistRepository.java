package com.integrafty.opexy.repository;

import com.integrafty.opexy.entity.WordWhitelistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordWhitelistRepository extends JpaRepository<WordWhitelistEntity, Long> {
    Optional<WordWhitelistEntity> findByWordIgnoreCase(String word);
    void deleteByWordIgnoreCase(String word);
}
