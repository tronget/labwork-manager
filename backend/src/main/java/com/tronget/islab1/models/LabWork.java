package com.tronget.islab1.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.util.Date;

@Entity
@Data
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LabWork implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически

    @Column(nullable = false)
    @NotBlank
    private String name; //Поле не может быть null, Строка не может быть пустой

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "coordinates_id", nullable = false)
    @NotNull
    private Coordinates coordinates; //Поле не может быть null

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Date creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически

    @Column(nullable = false)
    private String description; //Поле не может быть null

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty; //Поле может быть null

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "discipline_id")
    private Discipline discipline; //Поле может быть null

    @Positive
    private float minimalPoint; //Значение поля должно быть больше 0

    @Positive
    private double maximumPoint; //Значение поля должно быть больше 0

    private int tunedInWorks;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull
    private Person author; //Поле не может быть null
}
