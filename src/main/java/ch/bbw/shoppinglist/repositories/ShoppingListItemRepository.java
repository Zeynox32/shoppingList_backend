package ch.bbw.shoppinglist.repositories;

import ch.bbw.shoppinglist.entities.ShoppingList;
import ch.bbw.shoppinglist.entities.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, UUID> {
    Optional<List<ShoppingListItem>> findByShoppingList(ShoppingList shoppingList);
}
