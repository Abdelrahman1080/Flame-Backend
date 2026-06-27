package com.Flame.backend.auth;

import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DAO.users.ProviderRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.config.JwtService;
import com.Flame.backend.entities.user.*;
import com.Flame.backend.services.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor()
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PreferenceService preferenceService;




    public @Nullable AuthenticationResponse register(RegisterRequest request) {
        User user;
        if( userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        if(request.getRole().equalsIgnoreCase("USER")) {
            // Normalize and validate preferences before storing
            List<String> normalizedPrefs = preferenceService.splitAndNormalize(request.getPreferences());
            preferenceService.ensureAllExist(normalizedPrefs);
            String prefsCsv = String.join(",", normalizedPrefs);

            user = Customer.builder()
                    .firstname(request.getFirstName())
                    .lastname(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.USER)
                    .preferences(prefsCsv.isBlank() ? null : prefsCsv)
                    .build();
            customerRepository.save((Customer) user);
        }
        else if(request.getRole().equalsIgnoreCase("ADMIN")) {
            user = User.builder()
                    .firstname(request.getFirstName())
                    .lastname(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(user);
        }
        else {
            return null;
        }

        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .build();
    }




    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .build();
    }


}
