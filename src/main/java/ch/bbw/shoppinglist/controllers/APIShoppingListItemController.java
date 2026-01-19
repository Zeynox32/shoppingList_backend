package ch.bbw.shoppinglist.controllers;

import ch.bbw.shoppinglist.dtos.shoppingListItem.CreateShoppingListItem;
import ch.bbw.shoppinglist.dtos.shoppingListItem.ShoppingListItemResponse;
import ch.bbw.shoppinglist.dtos.shoppingListItem.UpdateShoppingListItem;
import ch.bbw.shoppinglist.dtos.shoppingListItemHistory.ShoppingListItemHistoryDto;
import ch.bbw.shoppinglist.entities.*;
import ch.bbw.shoppinglist.enums.ShoppingListItemStatus;
import ch.bbw.shoppinglist.enums.ShoppingListRole;
import ch.bbw.shoppinglist.repositories.ShoppingListItemRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListMembershipRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
@RestController
@RequestMapping("/api/shoppingLists/{shoppingListId}/items")
public class APIShoppingListItemController {

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;
    @Autowired
    private ShoppingListRepository shoppingListRepository;
    @Autowired
    private ShoppingListMembershipRepository membershipRepository;

    private ShoppingListUser getCurrentUser(HttpServletRequest request) {
        return (ShoppingListUser) request.getAttribute("currentUser");
    }

    private ShoppingList getShoppingList(UUID shoppingListId) {
        return shoppingListRepository.findById(shoppingListId).orElse(null);
    }

    private ShoppingListMembership getMembership(UUID shoppingListId, ShoppingListUser user) {
        return membershipRepository
                .findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(shoppingListId, user.getShoppingListUserId())
                .orElse(null);
    }

    private boolean hasWriteAccess(ShoppingListMembership membership) {
        return membership != null && membership.getRole() != ShoppingListRole.READ;
    }

    @GetMapping
    public ResponseEntity<List<ShoppingListItemResponse>> getItems(@PathVariable UUID shoppingListId, HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if (shoppingList == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListMembership membership = getMembership(shoppingListId, currentUser);
        if (membership == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        List<ShoppingListItemResponse> items = shoppingListItemRepository.findByShoppingList(shoppingList)
                .orElse(List.of())
                .stream()
                .map(this::toShoppingListItemResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    @DeleteMapping
    public ResponseEntity<List<ShoppingListItemResponse>> deleteAllItems(
            @PathVariable UUID shoppingListId, HttpServletRequest request) {

        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if (shoppingList == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListMembership membership = getMembership(shoppingListId, currentUser);
        if (membership == null || !hasWriteAccess(membership)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ShoppingListItem> items = shoppingListItemRepository.findByShoppingList(shoppingList)
                .orElse(List.of());
        List<ShoppingListItemResponse> responseItems = items.stream()
                .map(this::toShoppingListItemResponse)
                .collect(Collectors.toList());
        shoppingListItemRepository.deleteAll(items);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{itemId}")
    public ResponseEntity<ShoppingListItemResponse> getItem(@PathVariable UUID shoppingListId, @PathVariable UUID itemId, HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if (shoppingList == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListMembership membership = getMembership(shoppingListId, currentUser);
        if (membership == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<ShoppingListItem> itemOpt = shoppingListItemRepository.findById(itemId)
                .filter(i -> i.getShoppingList().getShoppingListId().equals(shoppingListId));

        if (itemOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(toShoppingListItemResponse(itemOpt.get()));
    }

    @PostMapping
    public ResponseEntity<ShoppingListItemResponse> addItem(@PathVariable UUID shoppingListId, @RequestBody CreateShoppingListItem dto, HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if (shoppingList == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListMembership membership = getMembership(shoppingListId, currentUser);
        if (!hasWriteAccess(membership)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getQuantity() == null || dto.getUnit() == null) {
            return ResponseEntity.badRequest().build();
        }

        ShoppingListItem item = new ShoppingListItem();
        item.setTitle(dto.getTitle());
        item.setQuantity(dto.getQuantity());
        item.setStatus(ShoppingListItemStatus.OPEN);
        item.setLastUpdated(LocalDateTime.now());
        item.setShoppingList(shoppingList);
        item.setUnit(dto.getUnit());
        item.setAuthor(currentUser);

        ShoppingListItemHistory history = new ShoppingListItemHistory();
        history.setTitle(dto.getTitle());
        history.setQuantity(dto.getQuantity());
        history.setDate(LocalDateTime.now());
        history.setUnit(dto.getUnit());
        history.setStatus(ShoppingListItemStatus.OPEN);
        history.setUsername(currentUser.getUsername());
        item.getEditionHistory().add(history);

        ShoppingListItem saved = shoppingListItemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(toShoppingListItemResponse(saved));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ShoppingListItemResponse> updateItem(
            @PathVariable UUID shoppingListId,
            @PathVariable UUID itemId,
            @RequestBody UpdateShoppingListItem dto,
            HttpServletRequest request
    ) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if (shoppingList == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListMembership membership = getMembership(shoppingListId, currentUser);
        if (membership == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        boolean isOnlyStatusChange =
                dto.getStatus() != null &&
                        dto.getTitle() == null &&
                        dto.getQuantity() == null &&
                        dto.getUnit() == null;

        if (!isOnlyStatusChange && !hasWriteAccess(membership)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<ShoppingListItem> itemOpt = shoppingListItemRepository.findById(itemId)
                .filter(i -> i.getShoppingList().getShoppingListId().equals(shoppingListId));

        if (itemOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListItem item = itemOpt.get();

        if (dto.getTitle() != null) item.setTitle(dto.getTitle());
        if (dto.getQuantity() != null) item.setQuantity(dto.getQuantity());
        if (dto.getStatus() != null) item.setStatus(dto.getStatus());
        if (dto.getUnit() != null) item.setUnit(dto.getUnit());

        item.setLastUpdated(LocalDateTime.now());

        ShoppingListItemHistory history = new ShoppingListItemHistory();
        history.setDate(LocalDateTime.now());
        history.setTitle(item.getTitle());
        history.setQuantity(item.getQuantity());
        history.setUsername(currentUser.getUsername());
        history.setStatus(item.getStatus());
        history.setUnit(item.getUnit());
        item.getEditionHistory().add(history);

        ShoppingListItem saved = shoppingListItemRepository.save(item);
        return ResponseEntity.ok(toShoppingListItemResponse(saved));
    }


    @DeleteMapping("/{itemId}")
    @Transactional
    public ResponseEntity<Void> deleteItem(@PathVariable UUID shoppingListId, @PathVariable UUID itemId, HttpServletRequest request) {
        ShoppingListUser currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ShoppingList shoppingList = getShoppingList(shoppingListId);
        if (shoppingList == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        ShoppingListMembership membership = getMembership(shoppingListId, currentUser);
        if (!hasWriteAccess(membership)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<ShoppingListItem> itemOpt = shoppingListItemRepository.findById(itemId)
                .filter(i -> i.getShoppingList().getShoppingListId().equals(shoppingListId));

        if (itemOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        shoppingListItemRepository.delete(itemOpt.get());
        return ResponseEntity.noContent().build();
    }

    private ShoppingListItemResponse toShoppingListItemResponse(ShoppingListItem item) {
        ShoppingListItemResponse dto = new ShoppingListItemResponse();
        dto.setShoppingListItemId(item.getShoppingListItemId());
        dto.setTitle(item.getTitle());
        dto.setQuantity(item.getQuantity());
        dto.setUnit(item.getUnit());
        dto.setStatus(item.getStatus().name());
        dto.setAuthorId(item.getAuthor() != null ? item.getAuthor().getShoppingListUserId() : null);
        dto.setAuthorName(item.getAuthor() != null ? item.getAuthor().getUsername() : null);
        dto.setLastUpdated(item.getLastUpdated());

        if (item.getEditionHistory() != null) {
            List<ShoppingListItemHistoryDto> historyDtos = item.getEditionHistory().stream()
                    .map(h -> {
                        ShoppingListItemHistoryDto hDto = new ShoppingListItemHistoryDto();
                        hDto.setDate(h.getDate());
                        hDto.setTitle(h.getTitle());
                        hDto.setQuantity(h.getQuantity());
                        hDto.setUnit(h.getUnit());
                        hDto.setUsername(h.getUsername());
                        hDto.setStatus(h.getStatus());
                        return hDto;
                    })
                    .toList();
            dto.setEditionHistory(historyDtos);
        }

        return dto;
    }
}