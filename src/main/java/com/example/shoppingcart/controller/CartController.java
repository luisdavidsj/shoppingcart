package com.example.shoppingcart.controller;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.service.CartService;
import com.example.shoppingcart.service.dto.AddItemRequest;
import com.example.shoppingcart.service.dto.UpdateItemQuantityRequest;
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
    private final CartMapper cartMapper;

    private String currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : null;
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody AddItemRequest request) {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        Cart cart = cartService.addItem(userId, request);
        return ResponseEntity.ok(cartMapper.toDto(cart));
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart() {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cartService.getCartDto(userId)); // usa el DTO del service
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemQuantityRequest request
    ) {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cartMapper.toDto(cartService.updateItemQuantity(userId, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> deleteItem(@PathVariable Long itemId) {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cartMapper.toDto(cartService.removeItem(userId, itemId)));
    }

    @DeleteMapping
    public ResponseEntity<CartDto> clearCart() {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cartMapper.toDto(cartService.clearCart(userId)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CartDto> checkout() {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cartMapper.toDto(cartService.checkout(userId)));
    }

    // (Opcional) Solo items
    @GetMapping("/items")
    public ResponseEntity<?> getItems() {
        String userId = currentUserOrNull();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cartMapper.toDto(cartService.getCart(userId)).items());
    }
}
