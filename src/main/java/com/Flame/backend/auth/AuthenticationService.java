package com.Flame.backend.auth;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DAO.users.ProviderRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.config.JwtService;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Provider;
import com.Flame.backend.entities.user.Role;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.PreferenceService;

import lombok.RequiredArgsConstructor;

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

    private String normalizePreferences(String preferences) {
        return String.join(",", preferenceService.splitAndNormalize(preferences));
    }




    public @Nullable AuthenticationResponse register(RegisterRequest request) {
        String normalizedPreferences = normalizePreferences(request.getPreferences());
        preferenceService.ensureAllExist(preferenceService.splitAndNormalize(normalizedPreferences));

        User user;

        if(request.getRole().equalsIgnoreCase("PROVIDER")) {
            user = Provider.builder()
                    .firstname(request.getFirstName())
                    .lastname(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .companyName(request.getCompanyName())
                    .companyLogoUrl(request.getCompanyLogoUrl())
                    .companyTagline(request.getCompanyTagline())
                    .companyDescription(request.getCompanyDescription())
                    .companyWebsite(request.getCompanyWebsite())
                    .companyLocation(request.getCompanyLocation())
                    .preferences(normalizedPreferences)
                    .role(Role.PROVIDER)
                    .build();
            providerRepository.save((Provider) user);
        } else if(request.getRole().equalsIgnoreCase("USER")) {
            user = Customer.builder()
                    .firstname(request.getFirstName())
                    .lastname(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .preferences(normalizedPreferences)
                    .role(Role.USER)
                    .build();
            customerRepository.save((Customer) user);
        }
        else if(request.getRole().equalsIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin self-registration is disabled");
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
