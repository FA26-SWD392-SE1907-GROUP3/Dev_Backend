package com.example.swd392_se1907_aives.repository;

import com.example.swd392_se1907_aives.domain.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    boolean existsBySubjectCode(String subjectCode);
    boolean existsBySubjectCodeAndSubjectIdNot(String subjectCode, Integer subjectId);
}
