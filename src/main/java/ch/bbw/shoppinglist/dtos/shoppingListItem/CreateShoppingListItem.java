package ch.bbw.shoppinglist.dtos.shoppingListItem;

import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import lombok.Data;

@Data
public class CreateShoppingListItem {
    private String title;
    private Integer quantity;
    private ShoppingListItemUnit unit;
}

