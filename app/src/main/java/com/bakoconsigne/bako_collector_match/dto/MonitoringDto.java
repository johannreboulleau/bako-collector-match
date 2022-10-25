package com.bakoconsigne.bako_collector_match.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO sent by collector with its status
 */
@Data
public class MonitoringDto implements Serializable {

    private String siteId;

    private String status;

    private float batteryPercent;
}
