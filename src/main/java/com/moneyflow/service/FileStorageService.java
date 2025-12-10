package com.moneyflow.service;

import com.moneyflow.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 파일 저장 서비스 (로컬 파일 시스템)
 * Phase 1: 로컬 파일 시스템에 임시 저장
 * Phase 2: AWS S3로 교체 예정
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads/ocr}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadPath);
            log.info("File storage initialized: {}", this.uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload directory", e);
            throw new BusinessException("파일 저장소 초기화에 실패했습니다");
        }
    }

    /**
     * 이미지 파일 저장
     * @param file 업로드된 파일
     * @return 저장된 파일 경로
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("저장할 파일이 없습니다");
        }

        try {
            // 고유 파일명 생성: {timestamp}_{uuid}_{originalFilename}
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            String originalFilename = file.getOriginalFilename();
            String filename = String.format("%s_%s_%s", timestamp, uuid, originalFilename);

            // 파일 저장
            Path targetPath = this.uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved: {}", filename);
            return filename;

        } catch (IOException e) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new BusinessException("파일 저장에 실패했습니다");
        }
    }

    /**
     * 파일 경로 반환
     * @param filename 파일명
     * @return 절대 경로
     */
    public Path getFilePath(String filename) {
        return this.uploadPath.resolve(filename).normalize();
    }

    /**
     * 파일 삭제
     * @param filename 파일명
     */
    public void deleteFile(String filename) {
        try {
            Path filePath = getFilePath(filename);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }
}
