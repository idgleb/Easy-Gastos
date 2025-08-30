package com.example.gestorgastos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses", 
       indices = {
           @Index(value = {"userUid"}),
           @Index(value = {"remoteId"}, unique = true),
           @Index(value = {"userUid", "fechaEpochMillis"}),
           @Index(value = {"userUid", "deletedAt"})
       })
public class ExpenseEntity {
    @PrimaryKey(autoGenerate = true)
    public long idLocal;
    
    @Nullable
    public String remoteId;
    
    @NonNull
    public String userUid;
    
    @NonNull
    public String categoryRemoteId;
    
    public double monto;
    
    public long fechaEpochMillis;
    
    public long updatedAt;
    
    @Nullable
    public Long deletedAt;
    
    @NonNull
    public String syncState = "PENDING"; // "PENDING" | "SYNCED" | "FAILED"
    
    @Override
    public String toString() {
        return "ExpenseEntity{" +
                "idLocal=" + idLocal +
                ", remoteId='" + remoteId + '\'' +
                ", userUid='" + userUid + '\'' +
                ", categoryRemoteId='" + categoryRemoteId + '\'' +
                ", monto=" + monto +
                ", fechaEpochMillis=" + fechaEpochMillis +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                ", syncState='" + syncState + '\'' +
                '}';
    }
}
