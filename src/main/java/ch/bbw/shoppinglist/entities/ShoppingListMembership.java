package ch.bbw.shoppinglist.entities;

import ch.bbw.shoppinglist.enums.ShoppingListRole;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "shopping_list_membership",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"shopping_list_id", "user_id"})})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShoppingListMembership {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private UUID shoppingListMembershipId;

    @ManyToOne
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private ShoppingListUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingListRole role;
}
