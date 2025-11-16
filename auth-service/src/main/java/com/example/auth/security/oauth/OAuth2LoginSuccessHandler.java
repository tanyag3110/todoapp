package com.example.auth.security.oauth;

import com.example.auth.entity.User;
import com.example.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest req,
            HttpServletResponse res,
            Authentication authentication
    ) throws IOException {

        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();

        User savedUser = customOAuth2UserService.processOAuthPostLogin(oauthUser);

        String accessToken = jwtProvider.generateAccessToken(savedUser.getUsername());
        String refreshToken = jwtProvider.generateRefreshToken(savedUser.getUsername());

        String redirectUrl = "http://localhost:5173/todos"
                + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        res.sendRedirect(redirectUrl);
    }
}
