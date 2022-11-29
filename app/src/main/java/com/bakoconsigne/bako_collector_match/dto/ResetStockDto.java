package com.bakoconsigne.bako_collector_match.dto;

import lombok.Data;

/**
 * DTO form to reset stock
 */
@Data
public class ResetStockDto {

    private String siteId;
    private boolean changeTicketPaper;
}
