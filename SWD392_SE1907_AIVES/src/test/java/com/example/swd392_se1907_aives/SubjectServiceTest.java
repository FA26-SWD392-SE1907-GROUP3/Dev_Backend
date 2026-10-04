package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.domain.entity.Subject;
import com.example.swd392_se1907_aives.dto.SubjectRequest;
import com.example.swd392_se1907_aives.repository.SubjectRepository;
import com.example.swd392_se1907_aives.service.SubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubjectServiceTest {
    private SubjectRepository repository;
    private SubjectService service;

    @BeforeEach
    void setup() {
        repository = mock(SubjectRepository.class);
        service = new SubjectService(repository);
    }

    @Test
    void duplicateCodeIsRejectedBeforeSaving() {
        when(repository.existsBySubjectCode("SWD392")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> service.create(
                new SubjectRequest(" SWD392 ", "Design", null)));
        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void missingSubjectReturns404() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.findById(99));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void updateWithSameCodeDoesNotConflictWithItself() {
        var subject = Subject.builder().subjectId(1).subjectCode("SWD392").subjectName("Old").build();
        when(repository.findById(1)).thenReturn(Optional.of(subject));
        when(repository.saveAndFlush(subject)).thenReturn(subject);
        var response = service.update(1, new SubjectRequest(" SWD392 ", " New ", null));
        assertEquals("New", response.subjectName());
        assertEquals("SWD392", response.subjectCode());
        verify(repository).existsBySubjectCodeAndSubjectIdNot("SWD392", 1);
    }
}
