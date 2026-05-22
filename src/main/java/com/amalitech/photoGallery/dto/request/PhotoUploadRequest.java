package com.amalitech.photoGallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
public class PhotoUploadRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private MultipartFile image;
}