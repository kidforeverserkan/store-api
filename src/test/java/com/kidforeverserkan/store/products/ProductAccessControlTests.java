package com.kidforeverserkan.store.products;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import com.kidforeverserkan.store.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductAccessControlTests extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category category;

    @BeforeEach
    void setUpCategory() {
        category = categoryRepository.save(new Category("Electronics"));
    }

    private String validProductJson(String name, String categoryId) {
        return """
                {"name":"%s","description":"A great product","categoryId":%s,"price":19.99}
                """.formatted(name, categoryId);
    }

    // Product browsing is intentionally public so an anonymous storefront
    // visitor can view the catalog before registering/logging in.
    @Test
    void getProducts_withoutToken_returnsList() throws Exception {
        var product = new Product();
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new java.math.BigDecimal("89.99"));
        product.setCategory(category);
        productRepository.save(product);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }

    @Test
    void getProducts_withToken_returnsList() throws Exception {
        var product = new Product();
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new java.math.BigDecimal("89.99"));
        product.setCategory(category);
        productRepository.save(product);

        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(get("/products")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }

    @Test
    void createProduct_asRegularUser_isForbidden() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(post("/products")
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content(validProductJson("Keyboard", category.getId().toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_asAdmin_returns201() throws Exception {
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(post("/products")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(validProductJson("Keyboard", category.getId().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void createProduct_withBlankName_returns400() throws Exception {
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(post("/products")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(validProductJson("", category.getId().toString())))
                .andExpect(status().isBadRequest());
    }

    // Regression test: updateProduct used to check `productDto == null`
    // instead of `product == null`, so a PUT to a non-existent product ID
    // threw an NPE (500) instead of returning 404.
    @Test
    void updateProduct_nonExistentId_returns404NotServerError() throws Exception {
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(put("/products/{id}", 999_999L)
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(validProductJson("Keyboard", category.getId().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_asRegularUser_isForbidden() throws Exception {
        var product = new Product();
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new java.math.BigDecimal("89.99"));
        product.setCategory(category);
        productRepository.save(product);

        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(delete("/products/{id}", product.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_asAdmin_returns200() throws Exception {
        var product = new Product();
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new java.math.BigDecimal("89.99"));
        product.setCategory(category);
        productRepository.save(product);

        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(put("/products/{id}", product.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(validProductJson("Mechanical Keyboard", category.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"));
    }

    @Test
    void deleteProduct_asAdmin_returns200() throws Exception {
        var product = new Product();
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new java.math.BigDecimal("89.99"));
        product.setCategory(category);
        productRepository.save(product);

        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(delete("/products/{id}", product.getId())
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk());
    }

    // Regression test: a malformed/non-numeric path variable used to throw
    // MethodArgumentTypeMismatchException, which the new catch-all handler
    // incorrectly turned into a 500 instead of a 400.
    @Test
    void getProduct_withNonNumericId_returns400NotServerError() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(get("/products/{id}", "not-a-number")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isBadRequest());
    }

    // Regression test: ProductMapper#toEntity used to copy a client-supplied
    // "id", so POST /products {"id": <existing>} silently overwrote that
    // product instead of creating a new one.
    @Test
    void createProduct_withExistingIdInBody_createsNewProductAndLeavesOriginalUntouched() throws Exception {
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);
        var existing = productRepository.save(Product.builder()
                .name("Original")
                .description("Original description")
                .price(new java.math.BigDecimal("10.00"))
                .category(category)
                .build());

        mockMvc.perform(post("/products")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content("""
                                {"id":%d,"name":"Overwrite attempt","description":"x","categoryId":%d,"price":1.00}
                                """.formatted(existing.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(existing.getId().intValue())));

        mockMvc.perform(get("/products/{id}", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original"));
    }

    // NOTE ON AN UNTESTED FIX: deleting a product still referenced by an
    // order item hits a foreign-key constraint at the database level
    // (order_items.product_id has no ON DELETE CASCADE in
    // V4__add_order_tables.sql, unlike cart_items, which does cascade).
    // GlobalExceptionHandler now maps DataIntegrityViolationException to a
    // clean 409 instead of a 500 for exactly this case. That fix could NOT
    // be reliably regression-tested here: this test class's @Transactional
    // rollback wrapping means a delete issued via MockMvc joins the same
    // outer (never-committed) transaction as the test's own setup, so
    // Hibernate never flushes the DELETE to H2 in time to trigger the FK
    // check before the test's rollback discards it — the request returns
    // 200 in-test even though the real app (no ambient test transaction)
    // correctly returns 409. This was verified live instead: a real
    // (non-test) instance of the app was started, a product was placed on
    // an order, and DELETE /products/{id} was confirmed to return 409 with
    // this fix in place (details in the deployment report; not re-asserted
    // here as an automated test to avoid a false-negative regression test).
}
