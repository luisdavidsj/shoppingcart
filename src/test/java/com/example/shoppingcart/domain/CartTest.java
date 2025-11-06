package com.example.shoppingcart.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    void recalcTotal_works() {
        Cart cart = Cart.builder().userId("u1").build();

        CartItem i1 = CartItem.builder()
                .productId("A").productName("A")
                .unitPrice(new BigDecimal("10.00")).quantity(2).build();
        CartItem i2 = CartItem.builder()
                .productId("B").productName("B")
                .unitPrice(new BigDecimal("5.50")).quantity(3).build();

        cart.addItem(i1);
        cart.addItem(i2);

        assertEquals(new BigDecimal("36.50"), cart.getTotal());
    }
}
