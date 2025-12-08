package com.tronget.islab1.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.List;

@Entity
@Data
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Discipline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name; //Поле не может быть null, Строка не может быть пустой
    private long practiceHours;
    private int selfStudyHours;
    private int labsCount;

    @OneToMany(mappedBy = "discipline", cascade = CascadeType.PERSIST)
    private List<LabWork> labWorks;
}
