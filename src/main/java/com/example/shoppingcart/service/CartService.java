package com.example.shoppingcart.service;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.domain.CartItem;
import com.example.shoppingcart.event.CartCheckedOutEvent;
import com.example.shoppingcart.event.CartClearedEvent;
import com.example.shoppingcart.event.CartItemAddedEvent;
import com.example.shoppingcart.event.CartItemRemovedEvent;
import com.example.shoppingcart.event.CartItemUpdatedEvent;
import com.example.shoppingcart.repository.CartItemRepository;
import com.example.shoppingcart.repository.CartRepository;
import com.example.shoppingcart.service.dto.AddItemRequest;
import com.example.shoppingcart.service.dto.UpdateItemQuantityRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Cart addItem(String userId, AddItemRequest req) {
        int attempts = 0;
        while (true) {
            try {
                Cart cart = cartRepository.findByUserId(userId)
                        .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

                CartItem item = CartItem.builder()
                        .productId(req.productId())
                        .productName(req.productName())
                        .unitPrice(req.unitPrice())
                        .quantity(req.quantity())
                        .build();

                cart.addItem(item);
                Cart saved = cartRepository.saveAndFlush(cart);

                publishEvent("cart-item-added", userId, new CartItemAddedEvent(
                        saved.getUserId(), saved.getId(), item.getProductId(), item.getQuantity(), item.getUnitPrice()));

                return saved;
            } catch (OptimisticLockingFailureException ex) {
                if (++attempts >= 3)
                    throw ex;
                try {
                    Thread.sleep(50L * attempts);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for userId=" + userId));
    }

    @Transactional
    public Cart updateItemQuantity(String userId, Long itemId, UpdateItemQuantityRequest req) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        item.setQuantity(req.quantity());
        // recalcular subtotal de item
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        // recalcular total del cart
        Cart cart = item.getCart();
        cart.recalcTotal();
        Cart saved = cartRepository.saveAndFlush(cart);

        publishEvent("cart-item-updated", userId,
                new CartItemUpdatedEvent(userId, saved.getId(), itemId, req.quantity()));

        return saved;
    }

    @Transactional
    public Cart removeItem(String userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        Cart cart = item.getCart();
        cart.getItems().remove(item); // orphanRemoval=true lo borra
        cart.recalcTotal();
        Cart saved = cartRepository.saveAndFlush(cart);

        publishEvent("cart-item-removed", userId,
                new CartItemRemovedEvent(userId, saved.getId(), itemId));

        return saved;
    }

    @Transactional
    public Cart clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for userId=" + userId));
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        Cart saved = cartRepository.saveAndFlush(cart);

        publishEvent("cart-cleared", userId, new CartClearedEvent(userId, saved.getId()));

        return saved;
    }

    /**
     * Checkout atómico: toma un snapshot del total, publica evento y limpia el
     * carrito.
     * (Si prefieres conservar items, quita el clear, pero aquí lo dejamos como
     * "consume y limpia".)
     */
    @Transactional
    public Cart checkout(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for userId=" + userId));
        cart.recalcTotal();
        BigDecimal finalTotal = cart.getTotal();

        publishEvent("cart-checked-out", userId,
                new CartCheckedOutEvent(userId, cart.getId(), finalTotal, cart.getItems().size()));

        // Limpia el carrito tras el "pago" simulado
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        return cartRepository.saveAndFlush(cart);
    }

    @Transactional(readOnly = true)
    public CartDto getCartDto(String userId) {
        // Carga el carrito + items en la misma transacción
        Cart cart = cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for userId=" + userId));
        // Mapea a DTO antes de cerrar la transacción
        return cartMapper.toDto(cart);
    }

    /**
     * Publica un evento en Kafka. Un fallo aquí (broker caído, serialización, etc.)
     * nunca debe tumbar la petición HTTP que ya se completó exitosamente en la DB.
     */
    private void publishEvent(String topic, String userId, Object event) {
        try {
            kafkaTemplate.send(topic, userId, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("No se pudo publicar el evento '{}' en Kafka para userId={}", topic, userId, e);
        }
    }
}
