package com.sellanythingtw.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "number_sequences")
public class NumberSequence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String sequenceKey;
    private Long currentValue = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSequenceKey() { return sequenceKey; }
    public void setSequenceKey(String sequenceKey) { this.sequenceKey = sequenceKey; }
    public Long getCurrentValue() { return currentValue; }
    public void setCurrentValue(Long currentValue) { this.currentValue = currentValue; }
}
