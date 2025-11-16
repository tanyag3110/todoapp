package com.example.auth.security.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
    }

    public String getEmail() {
        return (String) getAttributes().get("email");
    }

    public String getProviderId() {
        return (String) getAttributes().get("sub");
    }

    public String getPicture() {
        return (String) getAttributes().get("picture");
    }
}
