package com.roze.dbnavigator.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableIndex {
    private String name;
    private String columnName;
    private boolean nonUnique;
}