package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.domain.entity.Subject;
import com.example.swd392_se1907_aives.dto.SubjectRequest;
import com.example.swd392_se1907_aives.dto.SubjectResponse;
import com.example.swd392_se1907_aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {
    private final SubjectRepository repository;

    public Page<SubjectResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(SubjectResponse::from);
    }

    public SubjectResponse findById(Integer id) {
        return SubjectResponse.from(requireSubject(id));
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        String code = request.subjectCode().strip();
        if (repository.existsBySubjectCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject code already exists");
        }
        Subject subject = new Subject();
        apply(subject, request);
        return SubjectResponse.from(repository.saveAndFlush(subject));
    }

    @Transactional
    public SubjectResponse update(Integer id, SubjectRequest request) {
        Subject subject = requireSubject(id);
        if (repository.existsBySubjectCodeAndSubjectIdNot(request.subjectCode().strip(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject code already exists");
        }
        apply(subject, request);
        return SubjectResponse.from(repository.saveAndFlush(subject));
    }

    @Transactional
    public void delete(Integer id) {
        repository.delete(requireSubject(id));
        repository.flush();
    }

    private Subject requireSubject(Integer id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setSubjectCode(request.subjectCode().strip());
        subject.setSubjectName(request.subjectName().strip());
        subject.setDescription(request.description());
    }
}
