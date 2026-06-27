package com.overseas.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusStepDTO {

    private String code;
    private String label;
    private String stage;
    private String description;
    private boolean completed;
    private boolean current;
}
