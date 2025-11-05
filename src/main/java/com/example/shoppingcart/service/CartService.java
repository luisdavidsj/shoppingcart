package com.example.shoppingcart.service;

import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.domain.CartItem;
import com.example.shoppingcart.repository.CartRepository;
import com.example.shoppingcart.service.dto.AddItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

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

                // 🔹 Evita que un fallo en Kafka tumbe la petición
                try {
                    if (kafkaTemplate != null) {
                        kafkaTemplate.send("cart-item-added", saved.getUserId(), message);
                    }
                } catch (Exception ignored) {
                    // puedes loguearlo si quieres ver cuando Kafka falla
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
}
