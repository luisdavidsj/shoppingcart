package com.example.shoppingcart.api.mapper;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.dto.CartItemDto;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.domain.CartItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartMapper {

    public CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(this::toDto)
                .toList();

        return new CartDto(
                cart.getId(),
                cart.getUserId(),
                cart.getTotal(),
                items
        );
    }

    private CartItemDto toDto(CartItem item) {
        return new CartItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
