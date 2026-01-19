package ch.bbw.shoppinglist.dtos.shoppingList;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ShoppingListSummaryWithRole {
    private UUID shoppingListId;
    private String title;
    private String role;
}