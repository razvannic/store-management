package local.dev.storemanager.infrastructure.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import local.dev.storemanager.application.dto.BulkImportResponse;
import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.application.dto.ProductResponseDto;
import local.dev.storemanager.application.mapper.ProductMapper;
import local.dev.storemanager.domain.model.product.ImportMode;
import local.dev.storemanager.domain.service.ProductBulkImportService;
import local.dev.storemanager.domain.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Controller", description = "Managing products in the store")
public class ProductController {

    private final ProductService productService;
    private final ProductBulkImportService bulkImportService;
    private final ProductMapper productMapper;

    public ProductController(@Qualifier("productServiceImpl") ProductService productService,
                             ProductBulkImportService bulkImportService, ProductMapper productMapper) {
        this.productService = productService;
        this.bulkImportService = bulkImportService;
        this.productMapper = productMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a new product", description = "Accessible only authenticated users")
    public ResponseEntity<ProductResponseDto> addProduct(@RequestBody @Valid ProductRequestDto dto) {
        log.info("A request to add a new product was received with : {}", dto);
        final var product = productService.addProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponseDto(product));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get product by ID", description = "Accessible by all roles")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        log.info("A request to retrieve a product was received with id : {}", id);
        final var product = productService.findById(id);
        return ResponseEntity.ok(productMapper.toResponseDto(product));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Retrieve all products", description = "Accessible by all roles")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String size
    ) {
        log.info("A request to retrieve all products was received.");
        final var products = productService.findAllFiltered(type, author, brand, size);
        return ResponseEntity.ok(products.stream().map(productMapper::toResponseDto).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product", description = "Accessible only by authenticated users")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequestDto dto) {
        log.info("A request to update a product was received with id : {}", id);
        final var updated = productService.updateProduct(id, dto);
        return ResponseEntity.ok(productMapper.toResponseDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product", description = "Accessible only by authenticated users")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("A request to remove a product was received with id : {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk upload products", description = "Upload multiple products via JSON")
    public ResponseEntity<Void> uploadProducts(@RequestBody List<@Valid ProductRequestDto> products) {
        log.info("A request to bulk add {} products was received.", products.size());
        products.forEach(productService::addProduct);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/bulk/import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk import products", description = "Upload multiple products via file")
    public ResponseEntity<BulkImportResponse> importProducts(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "SINGLE_THREADED") ImportMode mode
    ) throws IOException {
        final var result = bulkImportService.importFromJson(file, mode);
        return ResponseEntity.ok(result);
    }

}
