package com.bakoconsigne.bako_collector_match.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for stock
 */
@Data
public class StockCollectorDTO implements Serializable {

    private List<StockDrawerDTO> drawers;

    private Integer maxPerDrawer;
    private Integer totalDrawer;
    private Integer weightTolerancePercent;
}
