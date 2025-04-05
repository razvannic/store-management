package local.dev.storemanager.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.dto.ProductResponseDto;
import local.dev.storemanager.application.exception.ProductNotFoundException;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.application.security.JwtRequestFilter;
import local.dev.storemanager.domain.model.product.Book;
import local.dev.storemanager.domain.model.product.Product;
import local.dev.storemanager.domain.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class)
})
@ActiveProfiles("test")
class ProductControllerTest {

    /*
    As of Spring Boot 3.2 (and fully deprecated in 3.4),
    @MockBean is marked for removal and no longer the recommended way to mock beans in Spring tests.
    Spring encourages using @TestConfiguration with manual mocks instead of @MockBean.
     */

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product;

    @BeforeEach
    void setup() {
        reset(productService, productMapper);
        product = Product.builder().id(1L).name("Book").price(10.0).quantity(5).build();
        final var productResponseDto = new ProductResponseDto(12L, "Book", 10.0, 5, "Book",
                new Book("John Doe", "Fiction"));
        when(productMapper.toResponseDto(any(Product.class))).thenReturn(productResponseDto);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Qualifier("productServiceImpl") // replace with productServiceCacheableImpl if switch to cacheable needed
        public ProductService productService() {
            return mock(ProductService.class);
        }

        @Bean
        public ProductMapper productMapper() {
            return mock(ProductMapper.class);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAddProduct() throws Exception {
        when(productService.addProduct(any())).thenReturn(product);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                        "name": "Book",
                                        "price": 10.0,
                                        "quantity": 5
                                    }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Book"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldGetProductById() throws Exception {
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Book"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldGetAllProducts() throws Exception {
        when(productService.findAllFiltered(null, null, null, null))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Book"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateProduct() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(product);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                        "name": "Book",
                                        "price": 10.0,
                                        "quantity": 5
                                    }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Book"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/products/999")
                        .header("Authorization", "Bearer dummy-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product with id 999 was not found."));
    }

    @Test
    void shouldReturn500WhenUnhandledExceptionOccurs() throws Exception {
        when(productService.findById(1L)).thenThrow(new RuntimeException("Unexpected failure"));

        mockMvc.perform(get("/products/1")
                        .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected failure"));
    }

    @Test
    void shouldReturn400ForInvalidProductInput() throws Exception {
        final var invalidPayload = """
                    {
                        "name": "  ",
                        "price": -5,
                        "quantity": 0
                    }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload)
                        .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[?(@.field == 'name')].message").value("Product name must not be blank"))
                .andExpect(jsonPath("$.messages[?(@.field == 'price')].message").value("Price must be greater than 0"))
                .andExpect(jsonPath("$.messages[?(@.field == 'quantity')].message").value("Quantity must be at least 1"));
    }

    @Test
    void shouldReturn400ForInvalidProductInputOnUpdate() throws Exception {
        final var invalidPayload = """
                    {
                        "name": "  ",
                        "price": -5,
                        "quantity": 0
                    }
                """;

        when(productService.updateProduct(eq(1L), any())).thenReturn(product);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload)
                        .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages[?(@.field == 'name')].message").value("Product name must not be blank"))
                .andExpect(jsonPath("$.messages[?(@.field == 'price')].message").value("Price must be greater than 0"))
                .andExpect(jsonPath("$.messages[?(@.field == 'quantity')].message").value("Quantity must be at least 1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptBulkUpload() throws Exception {
        final var products = List.of(
                new ProductRequestDto("Effective Java", 45.0, 10,
                        "Book", "Joshua Bloch", "Programming", null, null, null, null),
                new ProductRequestDto("MacBook Pro", 1999.99, 5,
                        "Electronics", null, null, "Apple", "1 year", null, null),
                new ProductRequestDto("T-Shirt", 19.99, 100,
                        "Clothing", null, null, null, null, "M", "Cotton")
        );

        final var json = objectMapper.writeValueAsString(products);

        mockMvc.perform(post("/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(productService, times(3)).addProduct(org.mockito.ArgumentMatchers.any());
    }
}
