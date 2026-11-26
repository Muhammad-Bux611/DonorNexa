package com.donornexa.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        // Rethrow to let Spring Security filter chain handle 403 Forbidden
        throw ex;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResource(NoResourceFoundException ex) throws NoResourceFoundException {
        // Rethrow to let Spring Boot handle 404 Not Found
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Exception exception, Model model) {
        log.error("Unhandled application exception: {}", exception.getMessage(), exception);
        // Keep internal exception details out of the page shown to users.
        model.addAttribute("message", "Something went wrong. Please try again.");
        return "error/500";
    }
}
