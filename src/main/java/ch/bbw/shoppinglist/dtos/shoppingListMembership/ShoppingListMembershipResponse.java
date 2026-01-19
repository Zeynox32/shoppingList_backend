package ch.bbw.shoppinglist.dtos.shoppingListMembership;

import ch.bbw.shoppinglist.enums.ShoppingListRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ShoppingListMembershipResponse {
    private UUID userId;
    private String username;
    private ShoppingListRole shoppingListRole;
}
