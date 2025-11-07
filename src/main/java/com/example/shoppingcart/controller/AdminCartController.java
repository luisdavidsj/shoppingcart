package com.example.shoppingcart.controller;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carts")
@RequiredArgsConstructor
public class AdminCartController {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    @GetMapping
    public ResponseEntity<List<CartDto>> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        return ResponseEntity.ok(carts.stream().map(cartMapper::toDto).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        if (!cartRepository.existsById(id)) return ResponseEntity.notFound().build();
        cartRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
