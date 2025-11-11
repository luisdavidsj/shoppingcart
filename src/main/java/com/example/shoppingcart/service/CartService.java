package com.example.shoppingcart.service;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.domain.CartItem;
import com.example.shoppingcart.repository.CartItemRepository;
import com.example.shoppingcart.repository.CartRepository;
import com.example.shoppingcart.service.dto.AddItemRequest;
import com.example.shoppingcart.service.dto.UpdateItemQuantityRequest;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

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

                // Mensaje Kafka
                String message = """
                        {"event":"cart-item-added","userId":"%s","cartId":%d,"productId":"%s","qty":%d,"unitPrice":"%s"}
                        """.formatted(
                        saved.getUserId(),
                        saved.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice().toPlainString());

                // Evita que un fallo en Kafka tumbe la petición
                try {
                    if (kafkaTemplate != null) {
                        kafkaTemplate.send("cart-item-added", saved.getUserId(), message);
                    }
                } catch (Exception ignored) {
                    // log cuando Kafka falla
                    System.err.println("[WARN] No se pudo publicar el mensaje en Kafka.");
                }

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

        // evento opcional
        try {
            if (kafkaTemplate != null) {
                String message = """
                        {"event":"cart-item-updated","userId":"%s","cartId":%d,"itemId":%d,"qty":%d}
                        """.formatted(userId, saved.getId(), itemId, req.quantity());
                kafkaTemplate.send("cart-item-updated", userId, message);
            }
        } catch (Exception ignored) {
        }
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

        try {
            if (kafkaTemplate != null) {
                String message = """
                        {"event":"cart-item-removed","userId":"%s","cartId":%d,"itemId":%d}
                        """.formatted(userId, saved.getId(), itemId);
                kafkaTemplate.send("cart-item-removed", userId, message);
            }
        } catch (Exception ignored) {
        }
        return saved;
    }

    @Transactional
    public Cart clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for userId=" + userId));
        cart.getItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        Cart saved = cartRepository.saveAndFlush(cart);

        try {
            if (kafkaTemplate != null) {
                String message = """
                        {"event":"cart-cleared","userId":"%s","cartId":%d}
                        """.formatted(userId, saved.getId());
                kafkaTemplate.send("cart-cleared", userId, message);
            }
        } catch (Exception ignored) {
        }
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

        // Publica evento de checkout
        try {
            if (kafkaTemplate != null) {
                String message = """
                        {"event":"cart-checked-out","userId":"%s","cartId":%d,"total":"%s","items":%d}
                        """.formatted(userId, cart.getId(), finalTotal.toPlainString(), cart.getItems().size());
                kafkaTemplate.send("cart-checked-out", userId, message);
            }
        } catch (Exception ignored) {
        }

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
}
