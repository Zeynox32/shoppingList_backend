package ch.bbw.shoppinglist;

import ch.bbw.shoppinglist.entities.*;
import ch.bbw.shoppinglist.enums.ShoppingListItemStatus;
import ch.bbw.shoppinglist.enums.ShoppingListItemUnit;
import ch.bbw.shoppinglist.enums.ShoppingListRole;
import ch.bbw.shoppinglist.repositories.ShoppingListItemRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListMembershipRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListUserRepository;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@SpringBootApplication
public class ShoppinglistApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppinglistApplication.class, args);
	}

    @Bean
    public CommandLineRunner demo(ShoppingListRepository shoppingListRepository, ShoppingListUserRepository userRepository, ShoppingListMembershipRepository membershipRepository, ShoppingListItemRepository itemRepository) {
        return (args)-> {
            List<ShoppingList> lists = IntStream.range(1, 11)
                    .mapToObj(i -> {
                        ShoppingList l = new ShoppingList();
                        l.setTitle("Einkaufsliste " + i);
                        return l;
                    })
                    .toList();

            shoppingListRepository.saveAll(lists);

            Argon2 argon2 = Argon2Factory.create();
            List<ShoppingListUser> users = IntStream.range(1, 11)
                    .mapToObj(i -> {
                        ShoppingListUser u = new ShoppingListUser();
                        u.setUsername("user" + i);
                        u.setPassword(argon2.hash(2, 65536, 1, "{noop}password" + i));
                        u.setToken(null);
                        return u;
                    })
                    .toList();

            userRepository.saveAll(users);

            List<ShoppingListMembership> memberships = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                ShoppingListMembership owner = new ShoppingListMembership();
                owner.setShoppingList(lists.get(i));
                owner.setUser(users.get(i));
                owner.setRole(ShoppingListRole.OWNER);
                memberships.add(owner);

                ShoppingListMembership writer = new ShoppingListMembership();
                writer.setShoppingList(lists.get(i));
                writer.setUser(users.get((i + 1) % users.size()));
                writer.setRole(ShoppingListRole.WRITE);
                memberships.add(writer);
            }

            membershipRepository.saveAll(memberships);

            List<ShoppingListItem> items = new ArrayList<>();

            for (ShoppingList list : lists) {
                for (int i = 1; i <= 10; i++) {

                    ShoppingListItem item = new ShoppingListItem();
                    item.setTitle("Artikel " + i);
                    item.setQuantity(ThreadLocalRandom.current().nextInt(1, 6));
                    item.setUnit(ShoppingListItemUnit.PIECES);
                    item.setStatus(i % 2 == 0 ? ShoppingListItemStatus.DONE : ShoppingListItemStatus.OPEN);
                    item.setLastUpdated(LocalDateTime.now().minusDays(i));
                    item.setAuthor(users.get(0));
                    item.setShoppingList(list);

                    for (int h = 0; h < 10; h++) {
                        ShoppingListItemHistory history = new ShoppingListItemHistory();
                        history.setTitle(item.getTitle());
                        history.setQuantity(item.getQuantity());
                        history.setUnit(item.getUnit());
                        history.setStatus(item.getStatus());
                        history.setUsername(item.getAuthor().getUsername());
                        history.setDate(LocalDateTime.now().minusDays(h));

                        item.getEditionHistory().add(history);
                    }

                    items.add(item);
                }
            }

            itemRepository.saveAll(items);
        };
    }
}
