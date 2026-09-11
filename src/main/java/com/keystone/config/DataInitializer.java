package com.keystone.config;

import com.keystone.model.Customer;
import com.keystone.model.Role;
import com.keystone.model.User;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the four role accounts on first startup.
 * Runs after Flyway migrations so schema + reference data already exist.
 * Passwords are BCrypt-encoded here — never stored in plain text in SQL.
 *
 * Default seed password for every account: Password123!
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_PASSWORD = "Password123!";

    private final UserRepository     userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder    passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository     = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder    = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        createUserIfAbsent("Alice Manager",    "manager@keystone.example.com",    Role.MANAGER,    null);
        createUserIfAbsent("Bob Dispatcher",   "dispatcher@keystone.example.com", Role.DISPATCHER, null);
        createUserIfAbsent("Carlos Technician","tech1@keystone.example.com",      Role.TECHNICIAN, null);
        createUserIfAbsent("Diana Technician", "tech2@keystone.example.com",      Role.TECHNICIAN, null);

        // CUSTOMER user must be linked to the Meridian customer record
        customerRepository.findByContactEmail("hq@meridian.example.com")
                .ifPresentOrElse(
                        customer -> createUserIfAbsent(
                                "Eve Customer",
                                "customer@keystone.example.com",
                                Role.CUSTOMER,
                                customer),
                        () -> log.warn(
                                "DataInitializer: Meridian customer not found — " +
                                "CUSTOMER seed user was not created. " +
                                "Ensure V2 migration ran successfully.")
                );

        log.info("DataInitializer: seed users verified.");
    }

    private void createUserIfAbsent(String name, String email, Role role, Customer customer) {

        if (userRepository.findByEmail(email).isEmpty()) {

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setRole(role);
            user.setCustomer(customer);

            userRepository.save(user);
            log.info("DataInitializer: created seed user [{}] with role [{}]", email, role);
        }
    }
}
