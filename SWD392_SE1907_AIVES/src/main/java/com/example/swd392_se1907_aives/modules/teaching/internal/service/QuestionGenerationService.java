package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.MaterialChunk;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationJob;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationMaterialId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionSourceChunk;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionSourceChunkId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionGenerationJobRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionGenerationMaterialRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionSourceChunkRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;


import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class QuestionGenerationService {
    private final CourseAccessService access;
    private final MaterialService materials;
    private final RubricRepository rubrics;
    private final RubricCriterionRepository criteria;
    private final QuestionGenerationJobRepository jobs;
    private final QuestionGenerationMaterialRepository jobMaterials;
    private final QuestionRepository questions;
    private final QuestionSourceChunkRepository sources;
    private final OpenAiQuestionClient client;
    private final PlatformTransactionManager transactions;
    public record GenerateRequest(@NotEmpty @Size(max=5) List<@NotNull UUID> materialIds,@NotNull UUID rubricId,
                                  @Min(1) @Max(20) int desiredCount,
                                  @NotNull @Pattern(regexp="REMEMBER|UNDERSTAND|APPLY|ANALYZE") String bloomLevel,
                                  @NotNull @Pattern(regexp="EASY|MEDIUM|HARD") String difficulty,
                                  @Size(max=2000) String instruction) {}

    public List<QuestionGenerationJob> list(UUID courseId){access.requireTeacher(courseId);return jobs.findByCourseIdOrderByCreatedAtDesc(courseId);}
    public List<Question> generatedQuestions(UUID jobId){
        var job=jobs.findById(jobId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy lần sinh câu hỏi."));
        access.requireTeacher(job.getCourseId());
        return questions.findByGenerationJobId(jobId).stream().filter(q->job.getCourseId().equals(q.getCourseId())).sorted(Comparator.comparing(Question::getCreatedAt).thenComparing(Question::getId)).toList();
    }
    public QuestionGenerationJob generate(UUID courseId,GenerateRequest req){
        access.requireTeacher(courseId);client.requireConfigured();
        Rubric rubric=rubrics.findById(req.rubricId()).filter(r->courseId.equals(r.getCourseId()))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rubric không thuộc môn học đã chọn."));
        if(!"PUBLISHED".equals(rubric.getStatus()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hãy chọn rubric đã công bố.");
        List<UUID> materialIds=req.materialIds().stream().distinct().toList();
        List<MaterialChunk> selected=new ArrayList<>();
        for(UUID id:materialIds){
            CourseMaterial material=materials.get(id);
            if(!courseId.equals(material.getCourseId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tài liệu không thuộc môn học đã chọn.");
            if(!"READY".equals(material.getExtractionStatus()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tài liệu chưa xử lý xong.");
            var content=materials.chunks(id);
            if(content.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tài liệu chưa có văn bản. Hãy xử lý tài liệu trước.");
            selected.addAll(content);
        }
        int length=selected.stream().mapToInt(c->c.getContent().length()).sum();
        if(length>60_000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Tài liệu vượt 60.000 ký tự cho một lần sinh câu hỏi. Hãy chọn ít tài liệu hơn hoặc chia nhỏ nội dung.");
        String rubricText=criteria.findByRubricId(rubric.getId()).stream().map(c->c.getCriterionName()+": "+Objects.toString(c.getDescription(),"")+" ("+c.getMaxPoints()+" điểm)").collect(Collectors.joining("\n"));
        String context="Rubric: "+rubric.getName()+"\n"+rubricText+"\nNguồn:\n"+selected.stream()
            .map(c->"[chunkId="+c.getId()+"]\n"+c.getContent()).collect(Collectors.joining("\n\n"));
        UUID requestedBy=access.current().getId();
        TransactionTemplate tx=new TransactionTemplate(transactions);
        QuestionGenerationJob job=tx.execute(status->{
            var j=new QuestionGenerationJob();j.setCourseId(courseId);j.setRequestedBy(requestedBy);j.setInstruction(req.instruction());
            j.setDesiredCount(req.desiredCount());j.setTargetBloomLevel(req.bloomLevel());j.setModelName(client.model());j.setStatus("RUNNING");j.setGeneratedCount(0);j.setCreatedAt(OffsetDateTime.now());
            jobs.saveAndFlush(j);
            for(UUID id:materialIds){var link=new QuestionGenerationMaterial();link.setId(new QuestionGenerationMaterialId(j.getId(),id));link.setCourseId(courseId);jobMaterials.save(link);}
            return j;
        });
        try {
            var drafts=client.generate(context,req.desiredCount(),req.bloomLevel(),req.difficulty(),req.instruction());
            Set<UUID> allowed=selected.stream().map(MaterialChunk::getId).collect(Collectors.toSet());
            Set<String> seen=new HashSet<>();
            if(drafts.size()!=req.desiredCount())throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Số câu hỏi sinh ra không hợp lệ.");
            for(var draft:drafts){
                if(!allowed.containsAll(draft.sourceChunkIds()) || draft.sourceChunkIds().isEmpty() || !seen.add(draft.content().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ")))
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Câu hỏi AI bị trùng hoặc có nguồn không hợp lệ. Hãy thử lại.");
            }
            return tx.execute(status->{
                var currentRubric=rubrics.findLockedById(rubric.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Rubric không còn tồn tại."));
                if(!"PUBLISHED".equals(currentRubric.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Rubric đã ngừng sử dụng trong lúc AI sinh câu hỏi. Chọn rubric khác rồi thử lại.");
                for(var draft:drafts){
                    var q=new Question();q.setCourseId(courseId);q.setCreatedBy(requestedBy);q.setRubricId(rubric.getId());q.setContent(draft.content());q.setReferenceAnswer(draft.referenceAnswer());
                    q.setBloomLevel(req.bloomLevel());q.setDifficulty(req.difficulty());q.setSourceType("AI");q.setGenerationJobId(job.getId());q.setStatus("PENDING_REVIEW");q.setCreatedAt(OffsetDateTime.now());q.setUpdatedAt(OffsetDateTime.now());questions.saveAndFlush(q);
                    for(UUID chunkId:draft.sourceChunkIds()){var source=new QuestionSourceChunk();source.setId(new QuestionSourceChunkId(q.getId(),chunkId));sources.save(source);}
                }
                job.setStatus("COMPLETED");job.setGeneratedCount(drafts.size());job.setCompletedAt(OffsetDateTime.now());return jobs.save(job);
            });
        }catch(RuntimeException e){
            String message=e instanceof ResponseStatusException r?r.getReason():"Không lưu được câu hỏi. Vui lòng thử lại.";
            return tx.execute(status->{
                // Reload after a failed transaction so no partial generated questions are retained.
                var failed=jobs.findById(job.getId()).orElseThrow();failed.setStatus("FAILED");failed.setGeneratedCount(0);failed.setErrorMessage(message);failed.setCompletedAt(OffsetDateTime.now());return jobs.save(failed);
            });
        }
    }
}
