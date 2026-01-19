package ch.bbw.shoppinglist.entities;

import ch.bbw.shoppinglist.enums.ShoppingListItemStatus;
import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_item")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShoppingListItem {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID shoppingListItemId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingListItemStatus status = ShoppingListItemStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingListItemUnit unit;

    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_item_id")
    private List<ShoppingListItemHistory> editionHistory = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private ShoppingListUser author;
}
