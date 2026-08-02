package com.example.shoppingcart.event;

import java.math.BigDecimal;

public record CartCheckedOutEvent(
        String userId,
        Long cartId,
        BigDecimal total,
        int itemCount
) {}
