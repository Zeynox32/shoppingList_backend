package ch.bbw.shoppinglist.dtos.shoppingListItemHistory;

import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import ch.bbw.shoppinglist.enums.ShoppingListItemStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShoppingListItemHistoryDto {
    private LocalDateTime date;
    private String title;
    private Integer quantity;
    private String username;
    private ShoppingListItemStatus status;
    private ShoppingListItemUnit unit;
}
