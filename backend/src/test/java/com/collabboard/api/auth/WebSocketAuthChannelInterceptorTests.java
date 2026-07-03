package com.collabboard.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.Principal;

@SpringBootTest
class WebSocketAuthChannelInterceptorTests {

    @Autowired
    private WebSocketAuthChannelInterceptor interceptor;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void connectWithValidBearerTokenSetsWebSocketUser() {
        AppUser user = userRepository.save(new AppUser(
                "Socket User",
                "socket.user@example.com",
                passwordEncoder.encode("password123"),
                UserRole.USER,
                Instant.now()
        ));
        String token = jwtService.issue(user).token();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);
        Principal principal = (Principal) result.getHeaders().get(SimpMessageHeaderAccessor.USER_HEADER);

        assertThat(principal).isNotNull();
        assertThat(principal.getName()).isEqualTo("socket.user@example.com");
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bearer token");
    }
}
