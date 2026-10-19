package com.example.swd392_se1907_aives.repository;

import com.example.swd392_se1907_aives.domain.entity.CourseMaterial;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, UUID> {
    List<CourseMaterial> findByCourseIdOrderByUploadedAtDesc(UUID courseId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from CourseMaterial m where m.id = :id")
    Optional<CourseMaterial> findLockedById(@Param("id") UUID id);
}
