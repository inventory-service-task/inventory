package com.mbicycle.inventory.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Builder
public class InventoryResponseDTO {
    private String reference;
    private String country;
    private String cedant;
    private String validationStatus;
    private String confirmationStatus;
    private String publicationDate;
    private String branche;
    private BigDecimal calculatedREC;
}
