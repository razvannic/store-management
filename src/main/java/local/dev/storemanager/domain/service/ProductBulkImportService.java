package local.dev.storemanager.domain.service;

import local.dev.storemanager.application.dto.BulkImportResponse;
import local.dev.storemanager.domain.model.product.ImportMode;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ProductBulkImportService {
    BulkImportResponse importFromJson(MultipartFile file, ImportMode mode) throws IOException;
}
