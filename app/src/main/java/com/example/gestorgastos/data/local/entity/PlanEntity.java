package com.example.gestorgastos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "plans", indices = {@Index(value = {"remoteId"}, unique = true)})
public class PlanEntity {
    @PrimaryKey(autoGenerate = true)
    public long idLocal;
    
    @NonNull
    public String remoteId;
    
    @NonNull
    public String name;
    
    public double price;
    
    @Nullable
    public String description;
    
    @Nullable
    public String features; // JSON string
    
    public boolean isActive;
    
    public long updatedAt;
}
