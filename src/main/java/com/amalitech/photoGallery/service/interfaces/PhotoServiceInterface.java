package com.amalitech.photoGallery.service.interfaces;

import com.amalitech.photoGallery.dto.request.PhotoEditRequest;
import com.amalitech.photoGallery.dto.request.PhotoUploadRequest;
import com.amalitech.photoGallery.models.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PhotoServiceInterface {
    Photo uploadPhoto(PhotoUploadRequest request);
    Page<Photo> getPhotos(String keyword, Pageable pageable);
    Photo getPhotoById(Long id);
    Photo updatePhoto(Long id, PhotoEditRequest request);
    void deletePhoto(Long id);
}