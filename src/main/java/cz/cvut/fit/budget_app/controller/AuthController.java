package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.LoginRequest;
import cz.cvut.fit.budget_app.dto.response.JwtResponse;
import cz.cvut.fit.budget_app.exception.TooManyLoginAttemptsException;
import cz.cvut.fit.budget_app.security.LoginRateLimiter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        String rateLimitKey = request.getUsername() + "|" + httpRequest.getRemoteAddr();
        if (loginRateLimiter.isBlocked(rateLimitKey)) {
            throw new TooManyLoginAttemptsException();
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            loginRateLimiter.recordFailure(rateLimitKey);
            throw ex;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        loginRateLimiter.reset(rateLimitKey);
        String token = tokenProvider.generateToken(principal);

        return ResponseEntity.ok(new JwtResponse(token, principal.getId(), principal.getUsername(), principal.getRole()));
    }
}
