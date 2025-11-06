package com.example.shoppingcart.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        Long id,
        String userId,
        BigDecimal total,
        List<CartItemDto> items
) {}
