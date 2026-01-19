package ch.bbw.shoppinglist.controllers;

import ch.bbw.shoppinglist.dtos.shoppingListMembership.ShoppingListMembershipResponse;
import ch.bbw.shoppinglist.dtos.shoppingListRole.ShoppingListRoleRequest;
import ch.bbw.shoppinglist.entities.ShoppingListMembership;
import ch.bbw.shoppinglist.entities.ShoppingListUser;
import ch.bbw.shoppinglist.enums.ShoppingListRole;
import ch.bbw.shoppinglist.repositories.ShoppingListMembershipRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.PUT,
                RequestMethod.DELETE,
                RequestMethod.OPTIONS
        }
)
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class ShoppingListMembershipController {

    @Autowired
    private ShoppingListMembershipRepository membershipRepository;
    @Autowired
    private ShoppingListUserRepository shoppingListUserRepository;

    private ShoppingListMembership getMembershipForCurrentUser(UUID listId, ShoppingListUser currentUser) {
        return membershipRepository
                .findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(listId, currentUser.getShoppingListUserId())
                .orElse(null);
    }

    private boolean isOwner(ShoppingListMembership membership) {
        return membership != null && membership.getRole() == ShoppingListRole.OWNER;
    }

    private boolean hasOwnerRights(ShoppingListMembership membership) {
        return membership != null && membership.getRole() == ShoppingListRole.OWNER;
    }

    @GetMapping("/{listId}")
    public ResponseEntity<List<ShoppingListMembershipResponse>> getAllMembershipsForOneList(HttpServletRequest request, @PathVariable UUID listId) {
        ShoppingListUser currentUser = (ShoppingListUser) request.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (getMembershipForCurrentUser(listId, currentUser) == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<ShoppingListMembership> memberships = membershipRepository.findByShoppingList_ShoppingListId(listId);
        if (memberships.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<ShoppingListMembershipResponse> response = memberships.stream()
                .map(m -> new ShoppingListMembershipResponse(m.getUser().getShoppingListUserId(), m.getUser().getUsername(), m.getRole()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{listId}/leave")
    public ResponseEntity<Void> leaveAListAsAUser(HttpServletRequest request, @PathVariable UUID listId) {
        ShoppingListUser currentUser = (ShoppingListUser) request.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingListMembership membership = getMembershipForCurrentUser(listId, currentUser);
        if (membership == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (isOwner(membership)) {
            long ownerCount = membershipRepository.countByShoppingList_ShoppingListIdAndRole(listId, ShoppingListRole.OWNER);
            if (ownerCount <= 1) return ResponseEntity.badRequest().build();
        }

        membershipRepository.delete(membership);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{listId}/editrole/{userId}")
    public ResponseEntity<Void> changeRole(HttpServletRequest request, @PathVariable UUID listId, @PathVariable UUID userId, @RequestBody ShoppingListRoleRequest body) {
        ShoppingListUser currentUser = (ShoppingListUser) request.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingListMembership currentMembership = getMembershipForCurrentUser(listId, currentUser);
        if (!hasOwnerRights(currentMembership)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        ShoppingListMembership membershipToChange = membershipRepository
                .findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(listId, userId)
                .orElse(null);
        if (membershipToChange == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListRole newRole = body.getShoppingListRole();
        if (newRole == null) return ResponseEntity.badRequest().build();

        if (isOwner(membershipToChange) && newRole != ShoppingListRole.OWNER) {
            long ownerCount = membershipRepository.countByShoppingList_ShoppingListIdAndRole(listId, ShoppingListRole.OWNER);
            if (ownerCount <= 1) return ResponseEntity.badRequest().build();
        }

        membershipToChange.setRole(newRole);
        membershipRepository.save(membershipToChange);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{listId}/remove/{shoppingListUsername}")
    public ResponseEntity<Void> removeUser(HttpServletRequest request, @PathVariable UUID listId, @PathVariable String shoppingListUsername) {
        ShoppingListUser currentUser = (ShoppingListUser) request.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingListMembership currentMembership = getMembershipForCurrentUser(listId, currentUser);
        if (!hasOwnerRights(currentMembership)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (shoppingListUsername == null || shoppingListUsername.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<ShoppingListUser> userOptional =
                shoppingListUserRepository.findByUsername(shoppingListUsername);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ShoppingListUser user = userOptional.get();
        UUID userId = user.getShoppingListUserId();

        ShoppingListMembership membershipToRemove = membershipRepository
                .findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(listId, userId)
                .orElse(null);
        if (membershipToRemove == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (isOwner(membershipToRemove)) {
            long ownerCount = membershipRepository.countByShoppingList_ShoppingListIdAndRole(listId, ShoppingListRole.OWNER);
            if (ownerCount <= 1) return ResponseEntity.badRequest().build();
        }

        membershipRepository.delete(membershipToRemove);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listId}/add/{shoppingListUsername}")
    public ResponseEntity<Void> addUserToList(HttpServletRequest request,
                                              @PathVariable UUID listId,
                                              @PathVariable String shoppingListUsername) {

        ShoppingListUser currentUser = (ShoppingListUser) request.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShoppingListMembership currentMembership = getMembershipForCurrentUser(listId, currentUser);
        if (currentMembership == null || !hasOwnerRights(currentMembership)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (shoppingListUsername == null || shoppingListUsername.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<ShoppingListUser> userOptional =
                shoppingListUserRepository.findByUsername(shoppingListUsername);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ShoppingListUser user = userOptional.get();
        UUID userId = user.getShoppingListUserId();

        boolean alreadyMember =
                membershipRepository.existsByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(listId, userId);

        if (alreadyMember) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        ShoppingListMembership newMembership = new ShoppingListMembership();
        newMembership.setShoppingList(currentMembership.getShoppingList());
        newMembership.setUser(user);
        newMembership.setRole(ShoppingListRole.READ);

        membershipRepository.save(newMembership);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
