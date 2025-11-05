package com.example.shoppingcart.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ejemplo de metadatos del producto
    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Version
    private Long version; // Locking en item también

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @PrePersist @PreUpdate
    public void calcSubtotal() {
        if (unitPrice == null || quantity == null) return;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
