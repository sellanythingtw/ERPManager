package com.gigastone.inventory.service;

import com.gigastone.inventory.entity.NumberSequence;
import com.gigastone.inventory.repository.NumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumberSequenceService {
    private final NumberSequenceRepository repository;

    public NumberSequenceService(NumberSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized long nextValue(String key) {
        NumberSequence sequence = repository.findBySequenceKey(key).orElseGet(() -> {
            NumberSequence ns = new NumberSequence();
            ns.setSequenceKey(key);
            ns.setCurrentValue(0L);
            return ns;
        });
        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        repository.save(sequence);
        return sequence.getCurrentValue();
    }

    public String nextMasterCode(String prefix) {
        return prefix + String.format("%06d", nextValue(prefix));
    }

    public String nextDocumentNo(String prefix, String yyyyMMdd) {
        return prefix + yyyyMMdd + String.format("%04d", nextValue(prefix + yyyyMMdd));
    }
}
