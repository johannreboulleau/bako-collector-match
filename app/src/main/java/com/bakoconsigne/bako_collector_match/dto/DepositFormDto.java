package com.bakoconsigne.bako_collector_match.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Form used to deposit boxes in collector
 */
@Data
public class DepositFormDto implements Serializable {

    private String userId;
    private String siteId;

    private Map<String, Integer> mapBoxes;
    private Integer numDrawer;
}
