package com.example.shoppingcart.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    void fullFlow_login_add_get_checkout() throws Exception {
        // 1) Login (crea sesión)
        var loginBody = om.writeValueAsString(Map.of("username", "luis", "password", "demo123"));
        var loginResp = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Usa cookie SESSION (Spring Session JDBC). Fallback a JSESSIONID si cambiara la config.
        var cookie = loginResp.getResponse().getCookie("SESSION");
        if (cookie == null) {
            cookie = loginResp.getResponse().getCookie("JSESSIONID");
        }
        assertNotNull(cookie, "No session cookie (SESSION/JSESSIONID) in login response");

        // 2) Agregar item
        var addBody = """
            {"productId":"SKU-001","productName":"Mouse","quantity":2,"unitPrice":349.90}
        """;
        mvc.perform(post("/api/carts/items")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("luis"))
                .andExpect(jsonPath("$.items[0].productId").value("SKU-001"));

        // 3) Obtener carrito
        mvc.perform(get("/api/carts").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        // 4) Checkout
        mvc.perform(post("/api/carts/checkout").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }
}
