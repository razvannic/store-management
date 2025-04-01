package local.dev.storemanager.infrastructure.rest;

import local.dev.storemanager.application.dto.ProductDto;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.application.security.JwtRequestFilter;
import local.dev.storemanager.domain.model.Product;
import local.dev.storemanager.domain.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private Product product;
    private ProductDto productDto;

    @BeforeEach
    void setup() {
        product = Product.builder().id(1L).name("Book").price(10.0).quantity(5).build();
        productDto = new ProductDto("Book", 10.0, 5);

        when(productMapper.toDto(any(Product.class))).thenReturn(productDto);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
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
        when(productService.findAll()).thenReturn(List.of(product));

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
}
