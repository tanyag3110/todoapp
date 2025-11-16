package com.example.auth.security.oauth;

import com.example.auth.entity.AuthProvider;
import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User user = super.loadUser(request);

        return new CustomOAuth2User(
                user.getAuthorities(),
                user.getAttributes(),
                "sub"
        );
    }


    public User processOAuthPostLogin(CustomOAuth2User oAuthUser) {

        String email = oAuthUser.getEmail();
        String googleId = oAuthUser.getProviderId();

        return userRepository.findByEmail(email)
                .map(u -> updateExistingUser(u, googleId))
                .orElseGet(() -> registerNewUser(email, googleId));
    }

    private User updateExistingUser(User user, String googleId) {
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderId(googleId);
        user.setEnabled(true);
        user.setLocked(false);
        return userRepository.save(user);
    }

    private User registerNewUser(String email, String googleId) {
        User user = User.builder()
                .email(email)
                .username(email)
                .passwordHash("") // OAuth users don't need password
                .role("USER")
                .enabled(true)
                .locked(false)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(googleId)
                .build();

        return userRepository.save(user);
    }
}
