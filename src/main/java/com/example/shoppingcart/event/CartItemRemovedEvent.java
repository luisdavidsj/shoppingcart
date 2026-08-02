package com.example.shoppingcart.event;

public record CartItemRemovedEvent(
        String userId,
        Long cartId,
        Long itemId
) {}
