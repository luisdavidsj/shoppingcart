package com.example.shoppingcart.controller;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/carts")
@RequiredArgsConstructor
public class AdminCartController {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    @GetMapping
    public ResponseEntity<Page<CartDto>> getAllCarts(@PageableDefault(size = 20) Pageable pageable) {
        Page<Cart> carts = cartRepository.findAll(pageable);
        return ResponseEntity.ok(carts.map(cartMapper::toDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        if (!cartRepository.existsById(id)) return ResponseEntity.notFound().build();
        cartRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
