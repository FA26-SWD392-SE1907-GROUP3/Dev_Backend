package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.repository.InterviewLogRepository;
import com.example.swd392_se1907_aives.domain.enums.ScheduleStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import java.nio.file.*;
import java.util.*;
import static com.example.swd392_se1907_aives.service.ManagementService.*;
@Service
@RequiredArgsConstructor
@Transactional
public class AudioService {
 private final InterviewLogRepository logs; private final AccessService access;
 @Value("${aives.storage.path}") private String storage;
 public void upload(Integer id,MultipartFile file) {
  var log=found(logs.findById(id)); access.owner(log.getExamSchedule().getStudent());
  require(log.getExamSchedule().getStatus()==ScheduleStatus.IN_PROGRESS,"Attempt is not in progress");
  require(log.getAnsweredAt()==null,"Audio cannot be changed after submission");
  require(!file.isEmpty() && file.getSize()<=20*1024*1024,"Invalid audio size");
  String type=Optional.ofNullable(file.getContentType()).orElse("").split(";")[0];
  Map<String,String> formats=Map.of("audio/webm","webm","audio/wav","wav","audio/ogg","ogg","audio/mp4","m4a");
  require(formats.containsKey(type),"Unsupported audio format");
  Path path=Path.of(storage).toAbsolutePath().normalize().resolve(UUID.randomUUID()+"."+formats.get(type));
  try {
   Files.createDirectories(path.getParent()); file.transferTo(path);
   String old=log.getAnswerAudioUrl(); log.setAnswerAudioUrl(path.toString()); logs.flush();
   org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
    @Override public void afterCompletion(int status) {
     try { if(status!=STATUS_COMMITTED) Files.deleteIfExists(path); else if(old!=null) Files.deleteIfExists(Path.of(old)); } catch(Exception ignored) {}
    }
   });
  } catch(Exception e) { try { Files.deleteIfExists(path); } catch(Exception ignored) {} throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not save audio"); }
 }
 public Path file(Integer id) {
  var log=found(logs.findById(id)); access.schedule(log.getExamSchedule()); require(log.getAnswerAudioUrl()!=null,"No recording available"); return Path.of(log.getAnswerAudioUrl());
 }
}
