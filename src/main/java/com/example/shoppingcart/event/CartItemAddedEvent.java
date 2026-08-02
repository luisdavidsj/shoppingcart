package com.example.shoppingcart.event;

import java.math.BigDecimal;

public record CartItemAddedEvent(
        String userId,
        Long cartId,
        String productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
