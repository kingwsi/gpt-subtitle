package org.example.gptsubtitle.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Response<String>> handleBusinessException(RuntimeException ex) {
        log.error("ExceptionHandler", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Response.failed(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<String>> handleException(Exception ex) {
        log.error("ExceptionHandler", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Response.failed(ex.getMessage()));
    }
}
