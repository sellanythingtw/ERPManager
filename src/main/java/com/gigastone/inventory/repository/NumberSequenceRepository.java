package com.gigastone.inventory.repository;

import com.gigastone.inventory.entity.NumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {
    Optional<NumberSequence> findBySequenceKey(String sequenceKey);
}
