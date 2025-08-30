package com.example.gestorgastos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"uid"}, unique = true)})
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long idLocal;
    
    @NonNull
    public String uid;
    
    @NonNull
    public String name;
    
    @NonNull
    public String email;
    
    @NonNull
    public String role; // "admin" | "user"
    
    @NonNull
    public String planId;
    
    @NonNull
    public String zonaHoraria;
    
    public boolean isActive;
    
    public long updatedAt;
}
