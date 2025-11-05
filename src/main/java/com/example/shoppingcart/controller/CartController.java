package com.example.shoppingcart.controller;

import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.service.CartService;
import com.example.shoppingcart.service.dto.AddItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private String currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : null;
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@Valid @RequestBody AddItemRequest request) {
        String userId = currentUserOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        Cart cart = cartService.addItem(userId, request);
        return ResponseEntity.ok(cart);
    }

    @GetMapping
    public ResponseEntity<Cart> getCart() {
        String userId = currentUserOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(cartService.getCart(userId));
    }
}