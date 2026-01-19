package ch.bbw.shoppinglist.security;

import java.io.IOException;
import java.util.Optional;

import ch.bbw.shoppinglist.entities.ShoppingListUser;
import ch.bbw.shoppinglist.repositories.ShoppingListRepository;
import ch.bbw.shoppinglist.repositories.ShoppingListUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final ShoppingListUserRepository shoppingListUserRepository;
    private final ShoppingListRepository shoppingListRepository;

    public AuthTokenFilter(ShoppingListUserRepository shoppingListUserRepository,
                           ShoppingListRepository shoppingListRepository) {
        this.shoppingListUserRepository = shoppingListUserRepository;
        this.shoppingListRepository = shoppingListRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring(7);
        Optional<ShoppingListUser> userOpt = shoppingListUserRepository.findByToken(token);

        if (userOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        request.setAttribute("currentUser", userOpt.get());
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/login") || path.equals("/api/shoppingListUsers/register");
    }
}
