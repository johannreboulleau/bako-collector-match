package com.bakoconsigne.bako_collector_match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BoxTypeDto implements Serializable {

    private String id;

    private String reference;

    private String type;

    private String designation;

    private Integer weight;
}
