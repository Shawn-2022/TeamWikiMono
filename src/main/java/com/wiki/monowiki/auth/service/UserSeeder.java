//package com.wiki.monowiki.auth.service;
//
//import com.wiki.monowiki.auth.model.Role;
//import com.wiki.monowiki.auth.model.Users;
//import com.wiki.monowiki.auth.repo.UserRepository;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class UserSeeder implements CommandLineRunner {
//
//    private final UserRepository repo;
//    private final PasswordEncoder encoder;
//
//    public UserSeeder(UserRepository repo, PasswordEncoder encoder) {
//	this.repo = repo;
//	this.encoder = encoder;
//    }
//
//    @Override
//    public void run(String... args) {
//	createIfMissing("admin", "admin123", Role.ADMIN);
//	createIfMissing("editor", "editor123", Role.EDITOR);
//	createIfMissing("viewer", "viewer123", Role.VIEWER);
//    }
//
//    private void createIfMissing(String username, String rawPassword, Role role) {
//	if (!repo.existsByUsername(username)) {
//	    repo.save(Users.builder().username(username).passwordHash(encoder.encode(rawPassword)).role(role).build());
//	}
//    }
//}
