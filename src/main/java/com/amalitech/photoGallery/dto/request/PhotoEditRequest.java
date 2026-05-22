package com.amalitech.photoGallery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PhotoEditRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
}