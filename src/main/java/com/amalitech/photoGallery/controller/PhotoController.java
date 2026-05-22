package com.amalitech.photoGallery.controller;

import com.amalitech.photoGallery.dto.request.PhotoEditRequest;
import com.amalitech.photoGallery.dto.request.PhotoUploadRequest;
import com.amalitech.photoGallery.models.Photo;
import com.amalitech.photoGallery.service.interfaces.PhotoServiceInterface;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@Controller
@RequestMapping("/")
public class PhotoController {

    private final PhotoServiceInterface photoService;

    @GetMapping
    public String listPhotos(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            Model model) {

        Page<Photo> photoPage = photoService.getPhotos(
                keyword, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("photos", photoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", photoPage.getTotalPages());
        model.addAttribute("totalItems", photoPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        return "gallery/list";
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("photoRequest", new PhotoUploadRequest());
        return "gallery/upload";
    }

    @PostMapping("/upload")
    public String uploadPhoto(
            @Valid @ModelAttribute("photoRequest") PhotoUploadRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "gallery/upload";
        }

        MultipartFile file = request.getImage();
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please select an image to upload.");
            return "gallery/upload";
        }

        try {
            photoService.uploadPhoto(request);
        } catch (Exception e) {
            model.addAttribute("error", "Upload failed: " + e.getMessage());
            return "gallery/upload";
        }

        return "redirect:/";
    }

    @GetMapping("/view/{id}")
    public String viewPhoto(@PathVariable("id") Long id, Model model) {
        model.addAttribute("photo", photoService.getPhotoById(id));
        return "gallery/view";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Photo photo = photoService.getPhotoById(id);
        PhotoEditRequest request = new PhotoEditRequest();
        request.setTitle(photo.getTitle());
        request.setDescription(photo.getDescription());
        model.addAttribute("editRequest", request);
        model.addAttribute("photo", photo);
        return "gallery/edit";
    }

    @PostMapping("/edit/{id}")
    public String updatePhoto(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("editRequest") PhotoEditRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            Photo photo = photoService.getPhotoById(id);
            model.addAttribute("photo", photo);
            return "gallery/edit";
        }

        photoService.updatePhoto(id, request);
        return "redirect:/view/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deletePhoto(@PathVariable("id") Long id) {
        photoService.deletePhoto(id);
        return "redirect:/";
    }
}