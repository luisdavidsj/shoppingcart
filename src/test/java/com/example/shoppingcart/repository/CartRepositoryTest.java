package com.example.shoppingcart.repository;

import com.example.shoppingcart.domain.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("shopping_cart_db")
            .withUsername("sc_user")
            .withPassword("sc_pass");

    @Autowired
    CartRepository repo;

    @Test
    void saveAndFindByUserId() {
        Cart c = repo.save(Cart.builder().userId("luis").build());
        assertNotNull(c.getId());

        var found = repo.findByUserId("luis");
        assertTrue(found.isPresent());
        assertEquals(c.getId(), found.get().getId());
    }
}
