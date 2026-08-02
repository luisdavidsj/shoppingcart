package com.example.shoppingcart.event;

public record CartItemUpdatedEvent(
        String userId,
        Long cartId,
        Long itemId,
        Integer quantity
) {}
