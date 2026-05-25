package com.amalitech.photoGallery.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;    // 10 MB
    private static final int  MEM_THRESHOLD  = 1024 * 1024;         // 1 MB — files below this stay in memory

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[] { PersistenceConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] { WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(
                new MultipartConfigElement(null, MAX_FILE_SIZE, MAX_FILE_SIZE, MEM_THRESHOLD));
    }
}
