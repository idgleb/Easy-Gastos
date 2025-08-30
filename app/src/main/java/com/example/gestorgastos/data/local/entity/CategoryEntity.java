package com.example.gestorgastos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories", 
       indices = {
           @Index(value = {"userUid"}),
           @Index(value = {"remoteId"}, unique = true),
           @Index(value = {"userUid", "deletedAt"})
       })
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long idLocal;
    
    @Nullable
    public String remoteId;
    
    @NonNull
    public String userUid;
    
    @NonNull
    public String name;
    
    @NonNull
    public String icono; // emoji string
    
    public boolean isActive = true;
    
    public long updatedAt;
    
    @Nullable
    public Long deletedAt;
    
    @NonNull
    public String syncState = "PENDING"; // "PENDING" | "SYNCED" | "FAILED"
}
