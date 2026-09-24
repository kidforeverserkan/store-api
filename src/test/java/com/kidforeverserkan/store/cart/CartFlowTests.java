package com.kidforeverserkan.store.cart;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import com.kidforeverserkan.store.products.Category;
import com.kidforeverserkan.store.products.CategoryRepository;
import com.kidforeverserkan.store.products.Product;
import com.kidforeverserkan.store.products.ProductRepository;
import com.kidforeverserkan.store.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartFlowTests extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product product;
    private String authHeader;

    @BeforeEach
    void setUp() {
        var category = categoryRepository.save(new Category("Electronics"));

        product = new Product();
        product.setName("Wireless Mouse");
        product.setDescription("A comfortable wireless mouse");
        product.setPrice(new BigDecimal("27.99"));
        product.setCategory(category);
        product = productRepository.save(product);

        var user = createUser("shopper@example.com", "password123", Role.USER);
        authHeader = bearerToken(user);
    }

    private String createCartAndGetId() throws Exception {
        var response = mockMvc.perform(post("/carts").header("Authorization", authHeader))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void createCart_returnsEmptyCart() throws Exception {
        mockMvc.perform(post("/carts").header("Authorization", authHeader))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0));
    }

    @Test
    void addItemToCart_returnsCreatedItem() throws Exception {
        var cartId = createCartAndGetId();

        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.id").value(product.getId()))
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void addItemToCart_withUnknownProduct_returns400() throws Exception {
        var cartId = createCartAndGetId();

        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content("{\"productId\":999999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCart_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/carts/{cartId}", UUID.randomUUID())
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCartItemQuantity_returnsUpdatedItem() throws Exception {
        var cartId = createCartAndGetId();

        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + "}"));

        mockMvc.perform(put("/carts/{cartId}/items/{productId}", cartId, product.getId())
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void removeItemFromCart_returns204AndClearsItem() throws Exception {
        var cartId = createCartAndGetId();

        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .header("Authorization", authHeader)
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + "}"));

        mockMvc.perform(delete("/carts/{cartId}/items/{productId}", cartId, product.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/carts/{cartId}", cartId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // Guest carts are intentionally public: an anonymous storefront visitor
    // must be able to build a cart before registering/logging in, and carts
    // already have no user-ownership model to check against.
    @Test
    void cartFlow_worksWithoutAuthentication() throws Exception {
        var createResponse = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var cartId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType("application/json")
                        .content("{\"productId\":" + product.getId() + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/carts/{cartId}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].product.id").value(product.getId()));
    }
}
