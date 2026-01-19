package ch.bbw.shoppinglist.dtos.shoppingListUser;

import lombok.Data;

import java.util.UUID;

@Data
public class ShoppingListSearchedUserResponse {
    private String username;
    private UUID shoppingListUserId;
}
