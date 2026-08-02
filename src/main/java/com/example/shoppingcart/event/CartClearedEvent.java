package com.example.shoppingcart.event;

public record CartClearedEvent(
        String userId,
        Long cartId
) {}
