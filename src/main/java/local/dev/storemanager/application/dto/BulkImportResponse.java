package local.dev.storemanager.application.dto;

public record BulkImportResponse(
        int total,
        int success,
        int failed,
        long durationMs
) {}
