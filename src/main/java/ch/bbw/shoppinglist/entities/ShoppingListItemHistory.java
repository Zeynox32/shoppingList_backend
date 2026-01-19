package ch.bbw.shoppinglist.entities;

import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import ch.bbw.shoppinglist.enums.ShoppingListItemStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shopping_list_item_history")
public class ShoppingListItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID shoppingListItemHistoryId;
    private LocalDateTime date;
    private String title;
    private Integer quantity;
    private String username;
    private ShoppingListItemStatus status;
    private ShoppingListItemUnit unit;
}
