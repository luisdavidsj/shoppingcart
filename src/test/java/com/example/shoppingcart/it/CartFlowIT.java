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

class CartFlowIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    void fullFlow_login_add_get_checkout() throws Exception {
        // 1) Login (genera JWT)
        var loginBody = om.writeValueAsString(Map.of("username", "luis", "password", "demo123"));
        var loginResp = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = om.readTree(loginResp.getResponse().getContentAsString()).get("token").asText();
        assertNotNull(token, "No se recibió token JWT en la respuesta de login");
        String authHeader = "Bearer " + token;

        // 2) Agregar item
        var addBody = """
            {"productId":"SKU-001","productName":"Mouse","quantity":2,"unitPrice":349.90}
        """;
        mvc.perform(post("/api/carts/items")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("luis"))
                .andExpect(jsonPath("$.items[0].productId").value("SKU-001"));

        // 3) Obtener carrito
        mvc.perform(get("/api/carts").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        // 4) Checkout
        mvc.perform(post("/api/carts/checkout").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        var loginBody = om.writeValueAsString(Map.of("username", "luis", "password", "wrong-password"));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/carts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_asRegularUser_returns403() throws Exception {
        var loginBody = om.writeValueAsString(Map.of("username", "luis", "password", "demo123"));
        var loginResp = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String token = om.readTree(loginResp.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/admin/carts").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
