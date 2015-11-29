package com.tdt.kioskapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseTokenDTO {

    @JsonProperty("firebase_token")
    protected String firebaseToken;
    protected String updates;
    protected String downloads;
}
