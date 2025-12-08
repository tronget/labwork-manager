package com.tronget.islab1.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull
    private Double x; //Поле не может быть null

    @Column(nullable = false)
    @NotNull
    private Long y; //Поле не может быть null

    @Column(nullable = false)
    @NotNull
    private String name; //Поле не может быть null

    @OneToMany(mappedBy = "location", cascade = CascadeType.PERSIST)
    private List<Person> persons;
}
