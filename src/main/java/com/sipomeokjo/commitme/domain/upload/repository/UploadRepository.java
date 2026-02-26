package com.sipomeokjo.commitme.domain.upload.repository;

import com.sipomeokjo.commitme.domain.upload.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadRepository extends JpaRepository<Upload, Long> {}
