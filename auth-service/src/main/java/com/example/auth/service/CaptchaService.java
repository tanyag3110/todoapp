package com.example.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${google.recaptcha.secret}")
    private String secret;

    private static final String VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify";

    public boolean verify(String captchaResponse) {
        RestTemplate rest = new RestTemplate();
        Map response = rest.postForObject(
                VERIFY_URL + "?secret=" + secret + "&response=" + captchaResponse,
                null,
                Map.class
        );
        return (Boolean) response.get("success");
    }
}
