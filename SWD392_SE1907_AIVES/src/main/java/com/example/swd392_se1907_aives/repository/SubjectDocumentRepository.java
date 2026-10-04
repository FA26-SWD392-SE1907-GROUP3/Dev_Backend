package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.SubjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SubjectDocumentRepository extends JpaRepository<SubjectDocument, Integer> { List<SubjectDocument> findBySubjectSubjectId(Integer subjectId); }
