package com.bakoconsigne.bako_collector_match.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for stock
 */
@Data
public class StockDTO implements Serializable {

    private String boxTypeId;
    private String boxTypeReference;
    private Integer quantity;
    private Integer numDrawer;
}
