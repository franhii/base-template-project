package com.example.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadReceiptRequest {
    @NotBlank
    private String paymentId;

    @NotBlank
    private String receiptUrl; // URL de la imagen (subida a Cloudinary/S3/etc)

    private String notes;
}