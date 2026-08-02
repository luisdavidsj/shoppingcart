package com.example.shoppingcart.controller;

import com.example.shoppingcart.api.dto.CartDto;
import com.example.shoppingcart.api.mapper.CartMapper;
import com.example.shoppingcart.domain.Cart;
import com.example.shoppingcart.security.JwtAuthenticationFilter;
import com.example.shoppingcart.service.CartService;
import com.example.shoppingcart.service.dto.AddItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test del controller: excluye JwtAuthenticationFilter (no aplica en este
 * slice, y arrastra a JwtService que no está en el contexto reducido) y deja
 * correr la cadena de seguridad por defecto de Spring Boot (permisiva). El
 * flujo de autenticación real con JWT/SecurityConfig ya lo cubre CartFlowIT.
 * Aquí se prueba validación de request, el chequeo manual de "sin usuario
 * autenticado" de CartController y la delegación al service.
 */
@WebMvcTest(
        controllers = CartController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class CartControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    CartService cartService;

    @MockitoBean
    CartMapper cartMapper;

    @Test
    void addItem_withoutAuthenticatedUser_returns401() throws Exception {
        var body = """
                {"productId":"SKU-1","productName":"Mouse","quantity":1,"unitPrice":10.00}
                """;

        mvc.perform(post("/api/carts/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_missingRequiredFields_returns400() throws Exception {
        var body = """
                {"quantity":1,"unitPrice":10.00}
                """;

        mvc.perform(post("/api/carts/items")
                        .with(user("luis").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_valid_delegatesToServiceAndReturnsCart() throws Exception {
        Cart cart = Cart.builder().id(1L).userId("luis").total(new BigDecimal("10.00")).build();
        CartDto dto = new CartDto(1L, "luis", new BigDecimal("10.00"), List.of());

        when(cartService.addItem(anyString(), any(AddItemRequest.class))).thenReturn(cart);
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        var body = """
                {"productId":"SKU-1","productName":"Mouse","quantity":1,"unitPrice":10.00}
                """;

        mvc.perform(post("/api/carts/items")
                        .with(user("luis").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("luis"));
    }

    @Test
    void getCart_withoutAuthenticatedUser_returns401() throws Exception {
        mvc.perform(get("/api/carts")).andExpect(status().isUnauthorized());
    }
}
