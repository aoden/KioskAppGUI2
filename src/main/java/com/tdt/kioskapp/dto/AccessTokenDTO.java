package com.tdt.kioskapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author aoden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenDTO {

    @JsonProperty("access_token")
    protected String accessToken;
    @JsonProperty("expires_in")
    protected String expire;
}
