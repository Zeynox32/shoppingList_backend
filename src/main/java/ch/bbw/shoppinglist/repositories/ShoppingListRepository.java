package ch.bbw.shoppinglist.repositories;

import ch.bbw.shoppinglist.entities.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {
}
