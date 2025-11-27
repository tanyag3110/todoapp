package com.example.auth.security.oauth;

import com.example.auth.entity.User;
import com.example.auth.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest req,
            HttpServletResponse res,
            Authentication authentication
    ) throws IOException {

        log.info("OAuth2LoginSuccessHandler invoked. Principal class = {}", authentication.getPrincipal().getClass().getName());

        Object principal = authentication.getPrincipal();
        CustomOAuth2User customUser = null;

        if (principal instanceof CustomOAuth2User cu) {
            customUser = cu;
        } else if (principal instanceof OidcUser oidc) {
            // wrap OidcUser in our custom wrapper
            customUser = new CustomOAuth2User(oidc);
        } else {
            log.error("Unsupported principal type in OAuth2LoginSuccessHandler: {}", principal.getClass().getName());
            // fallback: redirect to frontend without tokens (safer than throwing)
            res.sendRedirect("https://listtodo.duckdns.org/login?error=unsupported_principal");
            return;
        }

        // process the user in DB (create or update)
        User savedUser = customOAuth2UserService.processOAuthPostLogin(customUser);

        // generate tokens
        String accessToken = jwtProvider.generateAccessToken(savedUser.getUsername());
        String refreshToken = jwtProvider.generateRefreshToken(savedUser.getUsername());

        log.info("Generated tokens for user={} redirecting to frontend", savedUser.getUsername());

        String redirectUrl = "https://listtodo.duckdns.org/oauth2/callback"
                + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        log.info("Full redirect URL: {}", redirectUrl);
        res.sendRedirect(redirectUrl);
    }
}
