package com.example.KMALegend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {
    private Integer pageIndex = 0;
    private Integer pageSize = 10;
    private String keyword;
    private String sortBy; // field name
    private String sortDirection; // ASC or DESC
}
