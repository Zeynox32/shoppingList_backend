package ch.bbw.shoppinglist.controllers;

import ch.bbw.shoppinglist.dtos.shoppingList.ShoppingListSummaryWithRole;
import ch.bbw.shoppinglist.dtos.shoppingListUser.ShoppingListUserResponse;
import ch.bbw.shoppinglist.dtos.shoppingListUser.ShoppingListSearchedUserResponse;
import ch.bbw.shoppinglist.dtos.shoppingListRole.ShoppingListUserRoleResponse;
import ch.bbw.shoppinglist.entities.ShoppingList;
import ch.bbw.shoppinglist.entities.ShoppingListMembership;
import ch.bbw.shoppinglist.entities.ShoppingListUser;
import ch.bbw.shoppinglist.enums.ShoppingListRole;
import ch.bbw.shoppinglist.repositories.ShoppingListMembershipRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListUserRepository;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {
                RequestMethod.GET,
                RequestMethod.DELETE,
                RequestMethod.PUT,
                RequestMethod.POST,
                RequestMethod.OPTIONS
        }
)
@RestController
@RequestMapping("/api/shoppingListUsers")
public class APIShoppingListUserController {

    @Autowired
    private ShoppingListUserRepository shoppingListUserRepository;
    @Autowired
    private ShoppingListMembershipRepository membershipRepository;
    @Autowired
    private ShoppingListRepository shoppingListRepository;

    private ShoppingListUser getCurrentUser(HttpServletRequest request) {
        return (ShoppingListUser) request.getAttribute("currentUser");
    }

    private boolean isLastOwner(ShoppingListMembership membership) {
        if (membership.getRole() != ShoppingListRole.OWNER) return false;
        long ownerCount = membershipRepository.countByShoppingList_ShoppingListIdAndRole(
                membership.getShoppingList().getShoppingListId(),
                ShoppingListRole.OWNER
        );
        return ownerCount <= 1;
    }

    @GetMapping("/profile")
    public ResponseEntity<ShoppingListUserResponse> getOneProfile(HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<ShoppingListUser> userOpt = shoppingListUserRepository.findById(currentUser.getShoppingListUserId());
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toShoppingListUserResponse(userOpt.get()));
    }

    @GetMapping("/role/{shoppingListId}")
    public ResponseEntity<ShoppingListUserRoleResponse> getRoleForOneList(HttpServletRequest request, @PathVariable UUID shoppingListId) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<ShoppingList> shoppingListOpt = shoppingListRepository.findById(shoppingListId);
        if (shoppingListOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Optional<ShoppingListMembership> membershipOpt = membershipRepository
                .findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(
                        shoppingListId,
                        currentUser.getShoppingListUserId()
                );
        if (membershipOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListUserRoleResponse response = new ShoppingListUserRoleResponse();
        response.setRole(membershipOpt.get().getRole());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<Void> deleteUser(HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<ShoppingListUser> userOpt = shoppingListUserRepository.findById(currentUser.getShoppingListUserId());
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<ShoppingListMembership> memberships = membershipRepository.findByUser_ShoppingListUserId(currentUser.getShoppingListUserId());
        for (ShoppingListMembership m : memberships) {
            if (isLastOwner(m)) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        shoppingListUserRepository.delete(userOpt.get());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<ShoppingListUserResponse> register(@RequestBody ShoppingListUser shoppingListUser) {
        if (shoppingListUser.getUsername() == null || shoppingListUser.getPassword() == null) return ResponseEntity.badRequest().build();
        if (shoppingListUserRepository.existsByUsername(shoppingListUser.getUsername())) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        Argon2 argon2 = Argon2Factory.create();
        char[] password = shoppingListUser.getPassword().toCharArray();
        shoppingListUser.setPassword(argon2.hash(2, 65536, 1, password));
        argon2.wipeArray(password);

        try {
            ShoppingListUser savedUser = shoppingListUserRepository.save(shoppingListUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(toShoppingListUserResponse(savedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/edit")
    public ResponseEntity<ShoppingListUserResponse> editProfile(HttpServletRequest request, @RequestBody ShoppingListUser newUser) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<ShoppingListUser> userOpt = shoppingListUserRepository.findById(currentUser.getShoppingListUserId());
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListUser user = userOpt.get();
        if (newUser.getUsername() != null && !newUser.getUsername().isBlank()) {
            Optional<ShoppingListUser> existing = shoppingListUserRepository.findByUsername(newUser.getUsername());
            if (existing.isPresent() && !existing.get().getShoppingListUserId().equals(currentUser.getShoppingListUserId()))
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            user.setUsername(newUser.getUsername());
        }
        if (newUser.getPassword() != null && !newUser.getPassword().isBlank()) {
            Argon2 argon2 = Argon2Factory.create();
            char[] password = newUser.getPassword().toCharArray();
            user.setPassword(argon2.hash(2, 65536, 1, password));
            argon2.wipeArray(password);
        }

        ShoppingListUser saved = shoppingListUserRepository.save(user);
        return ResponseEntity.ok(toShoppingListUserResponse(saved));
    }

    private ShoppingListUserResponse toShoppingListUserResponse(ShoppingListUser user) {
        ShoppingListUserResponse dto = new ShoppingListUserResponse();
        dto.setId(user.getShoppingListUserId());
        dto.setUsername(user.getUsername());
        Set<ShoppingListSummaryWithRole> memberships = membershipRepository.findByUser_ShoppingListUserId(user.getShoppingListUserId())
                .stream()
                .map(m -> new ShoppingListSummaryWithRole(
                        m.getShoppingList().getShoppingListId(),
                        m.getShoppingList().getTitle(),
                        m.getRole().name()
                ))
                .collect(Collectors.toSet());
        dto.setMemberships(memberships);
        return dto;
    }
}
