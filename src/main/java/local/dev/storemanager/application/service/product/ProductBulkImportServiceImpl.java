package local.dev.storemanager.application.service.product;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import local.dev.storemanager.application.dto.BulkImportResponse;
import local.dev.storemanager.application.dto.ProductRequestDto;
import local.dev.storemanager.domain.model.product.ImportMode;
import local.dev.storemanager.domain.service.ProductBulkImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import local.dev.storemanager.domain.service.ProductService;


@Slf4j
@Service
public class ProductBulkImportServiceImpl implements ProductBulkImportService {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private ExecutorService executor;

    @Value("${import.bulk.batch-size:100}")
    private int batchSize;

    public ProductBulkImportServiceImpl(@Qualifier("productServiceImpl") ProductService productService,
                                        ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initExecutor() {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public BulkImportResponse importFromJson(MultipartFile file, ImportMode mode) throws IOException {
        log.info("Starting product import in {} mode", mode);

        final List<ProductRequestDto> productDtos = objectMapper.readValue(
                file.getInputStream(),
                new TypeReference<>() {
                }
        );

        log.info("Parsed {} products from uploaded file", productDtos.size());

        return switch (mode) {
            case SINGLE_THREADED -> importSingleThreaded(productDtos);
            case MULTI_THREADED -> importMultiThreaded(productDtos);
        };
    }

    private BulkImportResponse importSingleThreaded(List<ProductRequestDto> products) {
        log.info("Processing {} products in single-threaded mode", products.size());
        int success = 0, failed = 0;
        long start = System.currentTimeMillis();

        for (final var dto : products) {
            try {
                productService.addProduct(dto);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to import product: {}", dto.name(), e);
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Single-threaded import completed in {} ms. Success: {}, Failed: {}", duration, success, failed);
        return new BulkImportResponse(products.size(), success, failed, duration);
    }

    private BulkImportResponse importMultiThreaded(List<ProductRequestDto> products) {
        log.info("Processing {} products in multi-threaded mode with batch size {}", products.size(), batchSize);
        int total = products.size();
        long start = System.currentTimeMillis();

        final var success = new AtomicInteger(0);
        final var failed = new AtomicInteger(0);

        final List<List<ProductRequestDto>> batches = new ArrayList<>();
        for (int i = 0; i < products.size(); i += batchSize) {
            batches.add(products.subList(i, Math.min(i + batchSize, products.size())));
        }

        log.info("Split into {} batches", batches.size());

        final var futures = batches.stream()
                .map(batch -> executor.submit(() -> {
                    for (ProductRequestDto dto : batch) {
                        try {
                            productService.addProduct(dto);
                            success.incrementAndGet();
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            log.error("Failed to import product: {}", dto.name(), e);
                        }
                    }
                }))
                .toList();

        try {
            for (final var future : futures) {
                future.get(); // block until all the tasks are done
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Thread error during bulk import", e);
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Multi-threaded import completed in {} ms. Success: {}, Failed: {}", duration, success.get(), failed.get());
        return new BulkImportResponse(total, success.get(), failed.get(), duration);
    }

}
