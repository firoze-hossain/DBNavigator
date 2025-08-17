package com.roze.dbnavigator.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableColumnInfo {
    private String name;
    private String type;
    private int size;
    private String nullable;
    private String defaultValue;
}
