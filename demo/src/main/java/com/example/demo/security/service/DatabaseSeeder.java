package com.example.demo.security.service;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedDefaultUser();
    }

    private void seedAdminUser() {
        String adminUsername = "admin";
        if (!userRepository.existsByUsername(adminUsername)) {
            log.info("Seeding default administrator account...");
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            log.info("Administrator account seeded successfully (username: 'admin', password: 'admin123')");
        }
    }

    private void seedDefaultUser() {
        String defaultUsername = "user";
        if (!userRepository.existsByUsername(defaultUsername)) {
            log.info("Seeding default user account...");
            User user = new User();
            user.setUsername(defaultUsername);
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole(Role.ROLE_USER);
            userRepository.save(user);
            log.info("Default user account seeded successfully (username: 'user', password: 'user123')");
        }
    }
}
