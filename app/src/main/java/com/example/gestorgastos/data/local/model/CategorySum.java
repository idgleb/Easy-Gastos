package com.example.gestorgastos.data.local.model;

import androidx.room.ColumnInfo;

public class CategorySum {
    @ColumnInfo(name = "categoryRemoteId")
    public String categoryRemoteId;
    
    @ColumnInfo(name = "total")
    public double total;
    
    // Constructor para facilitar la creación
    public CategorySum(String categoryRemoteId, double total) {
        this.categoryRemoteId = categoryRemoteId;
        this.total = total;
    }
    
    // Constructor vacío requerido por Room
    public CategorySum() {}
}
