package com.example.swd392_se1907_aives.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.example.swd392_se1907_aives.domain.enums.*;
public final class ApiModels {
 public record Login(@NotBlank String username, @NotBlank String password) {}
 public record Registration(@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{3,50}") String username,
  @NotBlank @Size(min=8,max=72) String password, @NotBlank @Size(max=100) String fullName,
  @NotBlank @Email @Size(max=100) String email) {}
 public record GoogleLogin(@NotBlank @Size(max=10000) String credential, @NotBlank @Size(max=100) String nonce) {}
 public record Account(@NotBlank @Size(max=50) String username, @NotBlank @Size(min=8,max=72) String password,
  @NotBlank @Size(max=100) String fullName, @Email @NotBlank @Size(max=100) String email, @NotNull RoleName role) {}
 public record AccountUpdate(@NotBlank @Size(max=100) String fullName, @Email @NotBlank @Size(max=100) String email,
  @NotNull UserStatus status, RoleName role) {}
 public record QuestionInput(@NotNull Integer subjectId, Integer documentId, @Size(max=150) String topic,
  @NotBlank @Size(max=20000) String questionContent, @NotNull BloomLevel bloomLevel) {}
 public record Review(@NotNull QuestionStatus status) {}
 public record Criterion(@NotBlank @Size(max=255) String criteriaName, @NotNull @DecimalMin(value="0",inclusive=false)
  @Digits(integer=3,fraction=2) java.math.BigDecimal maxScore, @Size(max=20000) String guideline) {}
 public record ExamInput(@NotNull Integer subjectId, @NotBlank @Size(max=200) String examName,
  @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime, @Min(1) @Max(50) int maxMainQuestions,
  @Min(0) @Max(5) int maxFollowUpQuestions, @Min(15) @Max(1800) int answerTimeLimitSeconds) {}
 public record ScheduleInput(@NotNull Integer studentId, @NotNull LocalDateTime allocatedStartTime,
  @NotNull LocalDateTime allocatedEndTime) {}
 public record Generate(@Min(1) @Max(10) int count, @NotBlank @Size(max=150) String topic, @NotNull BloomLevel bloomLevel) {}
 public record Answer(@NotNull Integer logId, @NotBlank @Size(max=20000) String transcript) {}
}
