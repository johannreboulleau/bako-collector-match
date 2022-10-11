package com.bakoconsigne.bako_collector_match.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for stock of drawer
 */
@Data
public class StockDrawerDTO implements Serializable {

    private Integer numDrawer;
    private List<StockDTO> stockList;
}
