package com.example.swd392_se1907_aives.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SubjectID")
    private Integer subjectId;

    @Column(name = "SubjectCode", nullable = false, unique = true, length = 20)
    private String subjectCode;

    @Column(name = "SubjectName", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
}
