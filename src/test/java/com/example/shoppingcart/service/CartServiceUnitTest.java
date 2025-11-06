package com.example.shoppingcart.service;

import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.repository.CartItemRepository;
import com.example.shoppingcart.repository.CartRepository;
import com.example.shoppingcart.service.dto.AddItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartServiceUnitTest {

    @Test
    void addItem_createsCartAndPublishesEvent() {
        // Mocks
        CartRepository cartRepo = mock(CartRepository.class);
        CartItemRepository itemRepo = mock(CartItemRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String,String> kafkaTemplate = mock(KafkaTemplate.class);
        CartMapper mapper = new CartMapper(); // real, no dependencias

        // Stubs
        when(cartRepo.findByUserId("luis")).thenReturn(Optional.empty());
        when(cartRepo.save(any(Cart.class))).thenAnswer(a -> a.getArgument(0));
        when(cartRepo.saveAndFlush(any(Cart.class))).thenAnswer(a -> a.getArgument(0));

        // SUT con firma
        CartService service = new CartService(cartRepo, kafkaTemplate, itemRepo, mapper);

        // Act
        AddItemRequest req = new AddItemRequest("SKU-1", "Mouse", 2, new BigDecimal("10.00"));
        Cart c = service.addItem("luis", req);

        // Assert
        assertEquals("luis", c.getUserId());
        assertEquals(new BigDecimal("20.00"), c.getTotal());
        verify(kafkaTemplate, atLeastOnce()).send(eq("cart-item-added"), eq("luis"), anyString());
    }
}
