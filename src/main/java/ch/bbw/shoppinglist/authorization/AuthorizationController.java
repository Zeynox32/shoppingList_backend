package ch.bbw.shoppinglist.authorization;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import ch.bbw.shoppinglist.entities.ShoppingListUser;
import ch.bbw.shoppinglist.repositories.ShoppingListUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.web.server.ResponseStatusException;

@RestController

@CrossOrigin(origins = "http://localhost:5173")
public class AuthorizationController {

    @Autowired
    ShoppingListUserRepository shoppingListUserRepository;

    private ShoppingListUser requireCurrentUser(HttpServletRequest request) {
        ShoppingListUser currentUser = (ShoppingListUser) request.getAttribute("currentUser");
        if (currentUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return currentUser;
    }

    @PostMapping("api/login")
    public ResponseEntity<?> login(@RequestBody LoginPasswordRequest request) {
        Argon2 argon2 = Argon2Factory.create();

        Optional<ShoppingListUser> userOpt = shoppingListUserRepository.findByUsername((request.getUsername()));

        if (userOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Username or password is incorrect.");
        }

        ShoppingListUser user = userOpt.get();

        if (!argon2.verify(user.getPassword(), request.getPassword().toCharArray())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Username or password is incorrect");
        }

        String token = generateToken();
        user.setToken(token);

        shoppingListUserRepository.save(user);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new TokenResponse(token));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        try {
            ShoppingListUser user = requireCurrentUser(request);
            user.setToken(null);
            return ResponseEntity.status(HttpStatus.OK).body("Logout successful.");

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Logout failed.");
        }
    }

    @GetMapping("/api/authorization-check")
    public ResponseEntity<Boolean> checkAuthorization(HttpServletRequest request) {
        try {
            ShoppingListUser user = requireCurrentUser(request);
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
            return ResponseEntity.status(HttpStatus.OK).body(true);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    public static String generateToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}