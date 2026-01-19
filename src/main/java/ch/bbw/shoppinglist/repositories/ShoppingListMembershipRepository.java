package ch.bbw.shoppinglist.repositories;

import ch.bbw.shoppinglist.entities.ShoppingListMembership;
import ch.bbw.shoppinglist.entities.ShoppingListUser;
import ch.bbw.shoppinglist.enums.ShoppingListRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListMembershipRepository extends JpaRepository<ShoppingListMembership, UUID> {

    List<ShoppingListMembership> findByUser_ShoppingListUserId(UUID userId);

    Optional<ShoppingListMembership> findByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(UUID shoppingListId, UUID userId);

    long countByShoppingList_ShoppingListIdAndRole(UUID shoppingListId, ShoppingListRole role);

    List<ShoppingListMembership> findByShoppingList_ShoppingListId(UUID shoppingListId);

    boolean existsByShoppingList_ShoppingListIdAndUser_ShoppingListUserId(UUID shoppingListId, UUID userId);

}
