package ch.bbw.shoppinglist.repositories;

import ch.bbw.shoppinglist.entities.ShoppingListUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingListUserRepository extends JpaRepository<ShoppingListUser, UUID> {
    Optional<ShoppingListUser> findByUsername(String username);
    Optional<ShoppingListUser> findByToken(String token);
    boolean existsByUsername(String username);
}
