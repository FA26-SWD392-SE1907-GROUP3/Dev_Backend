package com.example.swd392_se1907_aives.repository;

import com.example.swd392_se1907_aives.domain.entity.MaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, UUID> {
    List<MaterialChunk> findByMaterialIdOrderByChunkIndex(UUID materialId);
    void deleteByMaterialId(UUID materialId);
    long countByMaterialId(UUID materialId);
}
