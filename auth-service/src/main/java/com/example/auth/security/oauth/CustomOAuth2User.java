package com.example.auth.security.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OidcUser {

    private final OidcUser oidcUser;

    public CustomOAuth2User(OidcUser oidcUser) {
        this.oidcUser = oidcUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oidcUser.getAuthorities();
    }

    @Override
    public String getName() {
        // subject is stable unique id
        return oidcUser.getSubject();
    }

    @Override
    public Map<String, Object> getClaims() {
        // crucial: forward real claims (do NOT return an empty map)
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    // convenience accessors
    public String getEmail() {
        return oidcUser.getEmail();
    }

    public String getProviderId() {
        return oidcUser.getSubject();
    }

    public String getPicture() {
        Object p = oidcUser.getAttributes().get("picture");
        return p == null ? null : p.toString();
    }

    public String getFullName() {
        Object n = oidcUser.getAttributes().get("name");
        return n == null ? null : n.toString();
    }
}
