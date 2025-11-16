package com.example.auth.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

class CaptchaServiceTest {

//    private CaptchaService captchaService;
//    private RestTemplate restTemplate;
//
//    private final String SECRET_KEY = "test-secret";
//
//    @BeforeEach
//    void setUp() {
//        restTemplate = mock(RestTemplate.class);
////        captchaService = new CaptchaService(SECRET_KEY, restTemplate);
//    }
//
//    @Test
//    void verifyCaptcha_ShouldReturnTrue_WhenVerificationSuccess() {
//        // Arrange
//        String token = "valid-token";
//        Map<String, Object> response = Map.of("success", true);
//        when(restTemplate.postForEntity(
//                anyString(),
//                any(),
//                eq(Map.class)
//        )).thenReturn(ResponseEntity.ok(response));
//
//        // Act
//        boolean result = captchaService.verify(token);
//
//        // Assert
//        assertFalse(result);
////        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
//    }
//
//    @Test
//    void verifyCaptcha_ShouldReturnFalse_WhenVerificationFails() {
//        String token = "invalid-token";
//        Map<String, Object> response = Map.of("success", false);
//        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
//                .thenReturn(ResponseEntity.ok(response));
//
//        boolean result = captchaService.verify(token);
//
//        assertFalse(result);
//    }
//
//    @Test
//    void verifyCaptcha_ShouldReturnFalse_WhenRestTemplateThrowsException() {
//        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
//                .thenThrow(new RuntimeException("Connection error"));
//
//        boolean result = captchaService.verify("token");
//
//        assertFalse(result);
//    }
//
//    @Test
//    void verifyCaptcha_ShouldReturnFalse_WhenResponseBodyIsNull() {
//        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
//                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
//
//        boolean result = captchaService.verify("token");
//
//        assertFalse(result);
//    }
}
