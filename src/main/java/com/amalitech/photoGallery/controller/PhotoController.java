package com.amalitech.photoGallery.controller;

import com.amalitech.photoGallery.dto.request.PhotoEditRequest;
import com.amalitech.photoGallery.dto.request.PhotoUploadRequest;
import com.amalitech.photoGallery.models.Photo;
import com.amalitech.photoGallery.service.interfaces.PhotoServiceInterface;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/")
public class PhotoController {

    private final PhotoServiceInterface photoService;
    private final String albDomain;

    public PhotoController(PhotoServiceInterface photoService,
                           @Value("${aws.cloudfront.alb.domain}") String albDomain) {
        this.photoService = photoService;
        this.albDomain = albDomain;
    }

    private String appRedirect(String path) {
        return "redirect:https://" + albDomain + path;
    }

    @GetMapping
    public String listPhotos(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            Model model) {

        Sort order = "oldest".equals(sort) ? Sort.by("createdAt").ascending()
                   : "alpha".equals(sort)  ? Sort.by("title").ascending()
                   :                         Sort.by("createdAt").descending();

        Page<Photo> photoPage = photoService.getPhotos(
                keyword, PageRequest.of(page, size, order));

        model.addAttribute("photos", photoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", photoPage.getTotalPages());
        model.addAttribute("totalItems", photoPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
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
            Model model,
            RedirectAttributes redirectAttributes) {

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

        redirectAttributes.addFlashAttribute("successMessage", "Photo uploaded successfully!");
        return appRedirect("/");
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
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            Photo photo = photoService.getPhotoById(id);
            model.addAttribute("photo", photo);
            return "gallery/edit";
        }

        photoService.updatePhoto(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Changes saved.");
        return appRedirect("/view/" + id);
    }

    @PostMapping("/delete/{id}")
    public String deletePhoto(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        photoService.deletePhoto(id);
        redirectAttributes.addFlashAttribute("successMessage", "Photo deleted.");
        return appRedirect("/");
    }
}
