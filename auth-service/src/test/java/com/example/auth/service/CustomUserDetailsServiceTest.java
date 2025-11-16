package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock UserRepository userRepo;
    @InjectMocks CustomUserDetailsService service;

    @BeforeEach
    void setUp(){ MockitoAnnotations.openMocks(this); }

    @Test
    void loadUserByUsername_notFound_throws() {
        when(userRepo.findByUsername("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("no"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_returnsUserDetails() {
        User u = User.builder().username("alice").passwordHash("ph").role("CUSTOMER").enabled(true).locked(false).build();
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(u));
        UserDetails ud = service.loadUserByUsername("alice");
        assertThat(ud.getUsername()).isEqualTo("alice");
        assertThat(ud.getPassword()).isEqualTo("ph");
        assertThat(ud.isAccountNonLocked()).isTrue();
        assertThat(ud.isEnabled()).isTrue();
    }
}
