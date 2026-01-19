package ch.bbw.shoppinglist.controllers;

import ch.bbw.shoppinglist.dtos.shoppingList.CreateShoppingList;
import ch.bbw.shoppinglist.dtos.shoppingList.ShoppingListResponse;
import ch.bbw.shoppinglist.dtos.shoppingList.UpdateShoppingList;
import ch.bbw.shoppinglist.entities.ShoppingList;
import ch.bbw.shoppinglist.entities.ShoppingListMembership;
import ch.bbw.shoppinglist.entities.ShoppingListUser;
import ch.bbw.shoppinglist.enums.ShoppingListRole;
import ch.bbw.shoppinglist.repositories.ShoppingListMembershipRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.POST, RequestMethod.OPTIONS}
)
@RestController
@RequestMapping("/api/shoppingLists")
public class APIShoppingListController {

    @Autowired
    private ShoppingListRepository shoppingListRepository;
    @Autowired
    private ShoppingListMembershipRepository membershipRepository;

    private ShoppingListUser getCurrentUser(HttpServletRequest request) {
        return (ShoppingListUser) request.getAttribute("currentUser");
    }

    private ShoppingList getShoppingList(UUID id) {
        return shoppingListRepository.findById(id).orElse(null);
    }

    private ShoppingListMembership getMembership(UUID listId, ShoppingListUser user) {
        return membershipRepository
                .findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(
                        listId,
                        user.getShoppingListUserId()
                )
                .orElse(null);
    }

    private boolean isOwner(ShoppingListMembership membership) {
        return membership != null && membership.getRole() == ShoppingListRole.OWNER;
    }

    @GetMapping
    public ResponseEntity<List<ShoppingListResponse>> getShoppingLists(HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ShoppingListResponse> lists = membershipRepository
                .findByUser_ShoppingListUserId(currentUser.getShoppingListUserId())
                .stream()
                .map(m -> toShoppingListResponse(m.getShoppingList()))
                .toList();

        return ResponseEntity.ok(lists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> getOneShoppingList(@PathVariable UUID id, HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShoppingList list = getShoppingList(id);
        if (list == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ShoppingListMembership membership = getMembership(id, currentUser);
        if (membership == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toShoppingListResponse(list));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ShoppingListResponse> createNewShoppingList(
            @RequestBody CreateShoppingList dto,
            HttpServletRequest request
    ) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ShoppingList list = new ShoppingList();
        list.setTitle(dto.getTitle());
        shoppingListRepository.save(list);

        ShoppingListMembership ownerMembership = new ShoppingListMembership();
        ownerMembership.setShoppingList(list);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(ShoppingListRole.OWNER);
        membershipRepository.save(ownerMembership);

        return ResponseEntity.status(HttpStatus.CREATED).body(toShoppingListResponse(list));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ShoppingListResponse> updateShoppingList(
            @PathVariable UUID id,
            @RequestBody UpdateShoppingList dto,
            HttpServletRequest request
    ) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShoppingList list = getShoppingList(id);
        if (list == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ShoppingListMembership membership = getMembership(id, currentUser);
        if (!isOwner(membership)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        list.setTitle(dto.getTitle());
        shoppingListRepository.save(list);

        return ResponseEntity.ok(toShoppingListResponse(list));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteShoppingList(@PathVariable UUID id, HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ShoppingList list = getShoppingList(id);
        if (list == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ShoppingListMembership membership = getMembership(id, currentUser);
        if (!isOwner(membership)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        shoppingListRepository.delete(list);
        return ResponseEntity.noContent().build();
    }

    private ShoppingListResponse toShoppingListResponse(ShoppingList list) {

        List<ShoppingListMembership> memberships = membershipRepository.findByShoppingList_ShoppingListId(list.getShoppingListId());
        int membersAmount = memberships.size();
        if (memberships.isEmpty()) membersAmount = 0;

        return new ShoppingListResponse(list.getShoppingListId(), list.getTitle(), membersAmount);
    }
}
