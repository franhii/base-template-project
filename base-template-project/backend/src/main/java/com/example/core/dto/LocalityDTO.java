package com.example.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalityDTO {
    private String id;
    private String name;
    private String municipalityId;
    private String municipalityName;
    private String provinceId;
    private String provinceName;
}