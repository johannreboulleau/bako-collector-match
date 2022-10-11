package com.bakoconsigne.bako_collector_match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsumerUserDto implements Serializable {

    private String id;

    private String login;

    private String firstName;

    private String lastName;

    private boolean activated;

    private EnumTypeHolder typeUser;

    public String getDisplayName() {
        if (firstName != null || lastName != null) {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }
        return login;
    }
}
