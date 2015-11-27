package com.tdt.kioskapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SlideDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlideDTO {

    protected String location;
    protected int seconds;
}
