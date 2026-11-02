package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.MaterialChunk;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseMaterialRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.MaterialChunkRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionGenerationMaterialRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.MaterialService;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.MaterialTextExtractor;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.MaterialPreviewRenderer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@lombok.extern.slf4j.Slf4j
public class MaterialServiceImpl implements MaterialService {

    private final CourseMaterialRepository materials;
    private final MaterialChunkRepository chunks;
    private final CourseRepository courses;
    private final CourseAccessService access;
    private final MaterialTextExtractor extractor;
    private final MaterialPreviewRenderer renderer;
    private final Path root;
    private final TransactionTemplate transaction;
    private final QuestionGenerationMaterialRepository jobMaterials;

    public MaterialServiceImpl(
            CourseMaterialRepository materials,
            MaterialChunkRepository chunks,
            CourseRepository courses,
            CourseAccessService access,
            MaterialTextExtractor extractor,
            @Value("${app.materials.storage-dir:./storage/materials}") String storageDir,
            PlatformTransactionManager transactionManager,
            QuestionGenerationMaterialRepository jobMaterials,
            MaterialPreviewRenderer renderer) {
        this.materials = materials;
        this.chunks = chunks;
        this.courses = courses;
        this.access = access;
        this.extractor = extractor;
        this.renderer = renderer;
        this.jobMaterials = jobMaterials;
        root = Path.of(storageDir).toAbsolutePath().normalize();
        transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public List<CourseMaterial> list(UUID courseId) {
        requireReadableCourse(courseId);
        return materials.findByCourseIdOrderByUploadedAtDesc(courseId);
    }

    @Override
    public CourseMaterial get(UUID id) {
        var material =
                materials
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu."));
        requireReadableCourse(material.getCourseId());
        return material;
    }

    @Override
    public CourseMaterial upload(UUID courseId, MultipartFile file) {
        requireCourse(courseId);
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chọn tệp PDF/DOCX/PPT/PPTX từ 1 byte đến 20 MB.");
        String filename =
                Optional.ofNullable(file.getOriginalFilename())
                        .orElse("document")
                        .replace('\\', '/');
        filename =
                filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
        if (filename.length() > 255)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tên tệp quá dài (tối đa 255 ký tự).");
        String lower = filename.toLowerCase(Locale.ROOT);
        String mime =
                lower.endsWith(".pdf") ? "application/pdf" : lower.endsWith(".docx") ? DOCX
                        : lower.endsWith(".pptx") ? PPTX : lower.endsWith(".ppt") ? PPT : null;
        if (mime == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ PDF, DOCX, PPT và PPTX.");
        Path destination = null;
        try {
            byte[] bytes = file.getBytes();
            boolean signature =
                    "application/pdf".equals(mime)
                            ? new String(
                                            bytes,
                                            0,
                                            Math.min(5, bytes.length),
                                            java.nio.charset.StandardCharsets.US_ASCII)
                                    .equals("%PDF-")
                            : PPT.equals(mime) ? bytes.length>=8 && java.util.Arrays.equals(java.util.Arrays.copyOf(bytes,8),new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1})
                            : bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K';
            if (!signature)
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Nội dung tệp không khớp định dạng PDF/DOCX/PPT/PPTX.");
            String key =
                    courseId
                            + "/"
                            + UUID.randomUUID()
                            + MaterialService.extension(mime);
            destination = path(key);
            Files.createDirectories(destination.getParent());
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            // Validate that the upload is readable before saving metadata.
            extractor.validate(destination, mime);
            CourseMaterial material = new CourseMaterial();
            material.setCourseId(courseId);
            material.setUploadedBy(access.current().getId());
            material.setOriginalFilename(filename);
            material.setObjectKey(key);
            material.setMimeType(mime);
            material.setFileSha256(
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            material.setExtractionStatus("UPLOADED");
            material.setUploadedAt(OffsetDateTime.now());
            return materials.saveAndFlush(material);
        } catch (IOException e) {
            cleanup(destination);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không đọc được tài liệu PDF/DOCX/PPT/PPTX. Hãy kiểm tra tệp; PDF ảnh cần OCR.");
        } catch (NoSuchAlgorithmException e) {
            cleanup(destination);
            throw new IllegalStateException(e);
        } catch (RuntimeException e) {
            cleanup(destination);
            throw e;
        }
    }

    @Override
    public CourseMaterial index(UUID id) {
        get(id); // Authorize before starting processing or changing its failure status.
        try {
            return transaction.execute(status -> indexInTransaction(id));
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() != 403 && e.getStatusCode().value() != 401)
                markFailed(id);
            throw e;
        } catch (RuntimeException e) {
            log.error("Cannot index course material {}", id, e);
            markFailed(id);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không lưu được văn bản đã trích xuất. Vui lòng thử lại; nếu vẫn lỗi, kiểm tra"
                            + " log backend.");
        }
    }

    private CourseMaterial indexInTransaction(UUID id) {
        CourseMaterial material =
                materials
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu."));
        access.requireTeacher(material.getCourseId());
        if ("READY".equals(material.getExtractionStatus())) return material;
        material.setExtractionStatus("PROCESSING");
        try {
            String text = extractor.extract(file(material), material.getMimeType());
            List<MaterialChunk> records = new ArrayList<>();
            for (int offset = 0, index = 0; offset < text.length(); index++) {
                int end = Math.min(offset + 1200, text.length());
                if (end < text.length()
                        && Character.isHighSurrogate(text.charAt(end - 1))
                        && Character.isLowSurrogate(text.charAt(end))) end--;
                String part = text.substring(offset, end).strip();
                offset = end;
                if (part.isBlank()) continue;
                MaterialChunk chunk = new MaterialChunk();
                chunk.setMaterialId(id);
                chunk.setChunkIndex(index);
                chunk.setContent(part);
                chunk.setTokenCount((part.length() + 3) / 4);
                chunk.setCreatedAt(OffsetDateTime.now());
                records.add(chunk);
            }
            chunks.deleteByMaterialId(id);
            chunks.flush();
            chunks.saveAllAndFlush(records);
            material.setExtractionStatus("READY");
            return materials.save(material);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không xử lý được tài liệu. Tệp có thể bị lỗi, bị khóa hoặc không chứa văn"
                            + " bản.");
        }
    }

    private void markFailed(UUID id) {
        try {
            transaction.executeWithoutResult(
                    status ->
                            materials
                                    .findLockedById(id)
                                    .filter(m -> !"READY".equals(m.getExtractionStatus()))
                                    .ifPresent(
                                            m -> {
                                                m.setExtractionStatus("FAILED");
                                                materials.saveAndFlush(m);
                                            }));
        } catch (RuntimeException e) {
            log.warn("Cannot record indexing failure for material {}", id, e);
        }
    }

    @Override
    public List<MaterialChunk> chunks(UUID id) {
        get(id);
        return chunks.findByMaterialIdOrderByChunkIndex(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CourseMaterial rename(UUID id, String name) {
        var m =
                materials
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu."));
        access.requireTeacher(m.getCourseId());
        String extension = MaterialService.extension(m.getMimeType());
        if (name.isBlank()
                || name.length() > 255
                || name.contains("/")
                || name.contains("\\")
                || !name.toLowerCase(Locale.ROOT).endsWith(extension)
                || name.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên tài liệu tối đa 255 ký tự, không chứa đường dẫn và cần giữ đuôi "
                            + extension
                            + ".");
        m.setOriginalFilename(name.strip());
        return materials.save(m);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteUnused(UUID id) {
        var m =
                materials
                        .findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu."));
        access.requireTeacher(m.getCourseId());
        if (jobMaterials.existsByIdMaterialId(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tài liệu đã dùng trong lịch sử sinh câu hỏi. Giữ tệp và nguồn trích dẫn; có"
                            + " thể đổi tên hoặc tải tài liệu mới.");
        Path original = path(m.getObjectKey());
        chunks.deleteByMaterialId(id);
        chunks.flush();
        materials.delete(m);
        materials.flush();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                cleanup(original);
                                cleanup(previewPath(m));
                            }
                        });
    }

    @Override
    public Path file(CourseMaterial material) {
        Path result = path(material.getObjectKey());
        if (!Files.isRegularFile(result))
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Tệp không còn trong kho lưu trữ. Hãy tải lại tài liệu.");
        return result;
    }

    private Path path(String key) {
        Path result = root.resolve(key).normalize();
        if (!result.startsWith(root))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Đường dẫn tài liệu không hợp lệ.");
        return result;
    }

    private void requireCourse(UUID id) {
        access.requireTeacher(id);
        if (!courses.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học.");
    }

    private void requireReadableCourse(UUID id) {
        if("STUDENT".equals(access.current().getRole())) {
            access.requireEnrolled(id);
            if(!courses.findById(id).map(c->Boolean.TRUE.equals(c.getIsActive())).orElse(false))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Môn học không còn mở để xem tài liệu.");
        } else requireCourse(id);
    }

    private Path previewPath(CourseMaterial material) {return path(".previews/"+material.getId()+".pdf");}

    @Override public Path preview(UUID id) {
        var material=get(id);Path original=file(material);
        if("application/pdf".equals(material.getMimeType()))return original;
        if(!PPT.equals(material.getMimeType())&&!PPTX.equals(material.getMimeType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tài liệu này sử dụng bản xem văn bản.");
        Path preview=previewPath(material),temporary=null;
        if(Files.isRegularFile(preview))return preview;
        try {
            byte[] bytes=renderer.renderPowerPoint(original,material.getMimeType());
            Files.createDirectories(preview.getParent());
            temporary=Files.createTempFile(preview.getParent(),"preview-",".tmp");
            Files.write(temporary,bytes);
            try {Files.move(temporary,preview,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}
            catch(java.nio.file.FileAlreadyExistsException alreadyRendered) {/* The concurrent result is complete. */}
            return preview;
        } catch(IOException failure) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Không tạo được bản xem trước. Bạn vẫn có thể tải tệp gốc; hãy thử xem lại sau.");
        } finally {cleanup(temporary);}
    }

    @Override public String textPreview(UUID id) {
        var material=get(id);
        try {return extractor.extract(file(material),material.getMimeType());}
        catch(IOException failure){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Không đọc được văn bản tài liệu. Bạn vẫn có thể tải tệp gốc.");}
    }

    private void cleanup(Path path) {
        if (path != null)
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
    }
}
