package ch.bbw.shoppinglist.dtos.shoppingListItem;

import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import ch.bbw.shoppinglist.enums.ShoppingListItemStatus;
import lombok.Data;

@Data
public class UpdateShoppingListItem {
    private String title;
    private Integer quantity;
    private ShoppingListItemUnit unit;
    private ShoppingListItemStatus status;
}
