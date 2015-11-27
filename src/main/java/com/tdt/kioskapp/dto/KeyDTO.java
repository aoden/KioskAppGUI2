package com.tdt.kioskapp.dto;

import com.tdt.kioskapp.model.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KeyDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyDTO {

    protected String key;

    public Key toEntity() {

        return Key.builder().key(key).build();
    }
}
