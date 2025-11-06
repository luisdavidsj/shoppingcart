package com.example.shoppingcart.api.dto;

import java.math.BigDecimal;

public record CartItemDto(
        Long id,
        String productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {}
