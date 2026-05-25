package com.amalitech.photoGallery.service;

import com.amalitech.photoGallery.dto.request.PhotoEditRequest;
import com.amalitech.photoGallery.dto.request.PhotoUploadRequest;
import com.amalitech.photoGallery.exceptions.PhotoNotFoundException;
import com.amalitech.photoGallery.exceptions.PhotoUploadException;
import com.amalitech.photoGallery.models.Photo;
import com.amalitech.photoGallery.repository.PhotoRepository;
import com.amalitech.photoGallery.service.interfaces.PhotoServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class PhotoService implements PhotoServiceInterface {

    private final PhotoRepository photoRepository;
    private final S3Client s3Client;
    private final String bucketName;
    private final String cloudfrontDomain;

    public PhotoService(PhotoRepository photoRepository,
                        S3Client s3Client,
                        @Value("${aws.s3.bucket}") String bucketName,
                        @Value("${aws.cloudfront.domain}") String cloudfrontDomain) {
        this.photoRepository = photoRepository;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.cloudfrontDomain = cloudfrontDomain;
    }

    @Override
    public Photo uploadPhoto(PhotoUploadRequest request) {
        MultipartFile file = request.getImage();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image to upload");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String s3Key = UUID.randomUUID() + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new PhotoUploadException("Failed to read image file", e);
        }

        return savePhoto(request.getTitle(), request.getDescription(), s3Key);
    }

    @Transactional
    protected Photo savePhoto(String title, String description, String s3Key) {
        Photo photo = new Photo();
        photo.setTitle(title);
        photo.setDescription(description);
        photo.setS3Key(s3Key);
        return photoRepository.save(photo);
    }

    @Override
    public Page<Photo> getPhotos(String keyword, Pageable pageable) {
        Page<Photo> page = (keyword != null && !keyword.isBlank())
                ? photoRepository.searchPhotos(keyword, pageable)
                : photoRepository.findAll(pageable);
        page.forEach(p -> p.setImageUrl(cloudfrontUrl(p.getS3Key())));
        return page;
    }

    @Override
    public Photo getPhotoById(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with id: " + id));
        photo.setImageUrl(cloudfrontUrl(photo.getS3Key()));
        return photo;
    }

    @Override
    @Transactional
    public Photo updatePhoto(Long id, PhotoEditRequest request) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with id: " + id));
        photo.setTitle(request.getTitle());
        photo.setDescription(request.getDescription());
        return photoRepository.save(photo);
    }

    @Override
    @Transactional
    public void deletePhoto(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with id: " + id));
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(photo.getS3Key())
                .build());
        photoRepository.delete(photo);
    }

    private String cloudfrontUrl(String s3Key) {
        return "https://" + cloudfrontDomain + "/" + s3Key;
    }
}
