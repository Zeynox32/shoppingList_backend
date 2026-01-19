package ch.bbw.shoppinglist.dtos.shoppingListItem;

import ch.bbw.shoppinglist.dtos.shoppingListItemHistory.ShoppingListItemHistoryDto;
import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ShoppingListItemResponse {
    private UUID shoppingListItemId;
    private String title;
    private int quantity;
    private ShoppingListItemUnit unit;
    private LocalDateTime lastUpdated;
    private String status;
    private UUID authorId;
    private String authorName;

    private List<ShoppingListItemHistoryDto> editionHistory;
}
