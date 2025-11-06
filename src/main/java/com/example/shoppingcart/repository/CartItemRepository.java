package com.example.shoppingcart.repository;

import com.example.shoppingcart.domain.CartItem;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCart_UserId(Long id, String userId);
}
