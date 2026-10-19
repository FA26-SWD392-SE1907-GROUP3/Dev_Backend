package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class QuestionImportService {
    private final QuestionImportParser parser;private final QuestionRepository questions;private final RubricRepository rubrics;
    private final CourseRepository courses;private final CourseAccessService access;private final QuestionImportBatchRepository batches;private final ObjectMapper json;
    public record PreviewRow(int row,String content,String referenceAnswer,String bloomLevel,String difficulty,UUID rubricId,String rubricName,List<String> errors){}
    public record Result(UUID batchId,String status,int totalRows,int importedRows,List<PreviewRow> rows){}
    @Transactional public Result process(UUID courseId,UUID defaultRubricId,String filename,byte[] bytes,boolean preview){
        access.requireTeacher(courseId);courses.findLockedById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));
        var parsed=parser.parse(filename,bytes);Set<String> seen=new HashSet<>();
        questions.findByCourseId(courseId).stream().filter(q->!"ARCHIVED".equals(q.getStatus())).forEach(q->seen.add(normalize(q.getContent())));
        List<PreviewRow> rows=new ArrayList<>();
        for(var row:parsed){
            List<String> errors=new ArrayList<>();String content=row.content().strip(),bloom=bloom(row.bloomLevel()),difficulty=row.difficulty().strip().toUpperCase(Locale.ROOT);UUID rubricId=defaultRubricId;
            if(content.isBlank()||content.length()>12000)errors.add("Nội dung phải từ 1 đến 12.000 ký tự.");
            if(row.referenceAnswer().length()>24000)errors.add("Đáp án vượt 24.000 ký tự.");
            if(!Set.of("REMEMBER","UNDERSTAND","APPLY","ANALYZE").contains(bloom))errors.add("Bloom phải là Nhớ, Hiểu, Vận dụng hoặc Phân tích.");
            if(!Set.of("EASY","MEDIUM","HARD").contains(difficulty))errors.add("Độ khó phải là EASY, MEDIUM hoặc HARD.");
            if(!seen.add(normalize(content)))errors.add("Nội dung câu hỏi bị trùng trong tệp hoặc trong môn học.");
            if(!row.rubricId().isBlank())try{rubricId=UUID.fromString(row.rubricId().strip());}catch(IllegalArgumentException e){errors.add("rubricId không hợp lệ.");rubricId=null;}
            Rubric rubric=rubricId==null?null:rubrics.findById(rubricId).orElse(null);
            if(rubric==null||!courseId.equals(rubric.getCourseId())||!"PUBLISHED".equals(rubric.getStatus()))errors.add("Cần rubric đã công bố thuộc môn học này.");
            rows.add(new PreviewRow(row.row(),content,row.referenceAnswer().strip(),bloom,difficulty,rubricId,rubric==null?null:rubric.getName(),errors));
        }
        boolean invalid=rows.stream().anyMatch(r->!r.errors().isEmpty());
        if(preview)return new Result(null,invalid?"INVALID":"READY",rows.size(),0,rows);
        QuestionImportBatch batch=new QuestionImportBatch();batch.setCourseId(courseId);batch.setImportedBy(access.current().getId());batch.setOriginalFilename(filename.length()>255?filename.substring(filename.length()-255):filename);
        batch.setTotalRows(rows.size());batch.setImportedRows(invalid?0:rows.size());batch.setStatus(invalid?"FAILED":"COMPLETED");batch.setImportedAt(OffsetDateTime.now());batch.setErrorDetails(json.valueToTree(rows.stream().filter(r->!r.errors().isEmpty()).toList()));batch=batches.saveAndFlush(batch);
        if(!invalid)for(var row:rows){
            var q=new Question();q.setCourseId(courseId);q.setCreatedBy(access.current().getId());q.setRubricId(row.rubricId());q.setContent(row.content());q.setReferenceAnswer(row.referenceAnswer());q.setBloomLevel(row.bloomLevel());q.setDifficulty(row.difficulty());
            q.setSourceType("IMPORTED");q.setImportBatchId(batch.getId());q.setStatus("PENDING_REVIEW");q.setCreatedAt(OffsetDateTime.now());q.setUpdatedAt(OffsetDateTime.now());questions.save(q);
        }
        return new Result(batch.getId(),batch.getStatus(),rows.size(),batch.getImportedRows(),rows);
    }
    private String normalize(String text){return text.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
    private String bloom(String value){return switch(value.strip().toUpperCase(Locale.ROOT)){case "NHỚ","NHO"->"REMEMBER";case "HIỂU","HIEU"->"UNDERSTAND";case "VẬN DỤNG","VAN DUNG"->"APPLY";case "PHÂN TÍCH","PHAN TICH"->"ANALYZE";default->value.strip().toUpperCase(Locale.ROOT);};}
}
