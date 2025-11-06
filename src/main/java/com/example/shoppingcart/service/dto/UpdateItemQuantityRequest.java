package com.example.shoppingcart.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateItemQuantityRequest(@NotNull @Min(1) Integer quantity) {}
