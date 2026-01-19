package ch.bbw.shoppinglist.dtos.shoppingList;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ShoppingListResponse {
    private UUID shoppingListId;
    private String title;
    private int membersAmount;
}
