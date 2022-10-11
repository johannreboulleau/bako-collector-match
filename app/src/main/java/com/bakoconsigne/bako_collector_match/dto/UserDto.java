package com.bakoconsigne.bako_collector_match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {

    private List<String> authorities = new ArrayList<>();
}
