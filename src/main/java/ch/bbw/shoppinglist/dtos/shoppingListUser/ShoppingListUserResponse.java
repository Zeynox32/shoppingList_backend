package ch.bbw.shoppinglist.dtos.shoppingListUser;

import ch.bbw.shoppinglist.dtos.shoppingList.ShoppingListSummaryWithRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ShoppingListUserResponse {
    private UUID id;
    private String username;
    private Set<ShoppingListSummaryWithRole> memberships;
}