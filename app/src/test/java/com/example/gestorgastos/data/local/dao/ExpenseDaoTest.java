package com.example.gestorgastos.data.local.dao;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ExpenseDaoTest {
    private AppDatabase database;
    private ExpenseDao expenseDao;
    
    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        expenseDao = database.expenseDao();
    }
    
    @After
    public void closeDb() {
        database.close();
    }
    
    @Test
    public void insertAndGetExpense() {
        // Given
        ExpenseEntity expense = new ExpenseEntity();
        expense.userUid = "test_user";
        expense.categoryRemoteId = "test_category";
        expense.monto = 100.0;
        expense.fechaEpochMillis = System.currentTimeMillis();
        expense.updatedAt = System.currentTimeMillis();
        expense.syncState = "PENDING";
        
        // When
        long id = expenseDao.insertExpense(expense);
        List<ExpenseEntity> expenses = expenseDao.getExpensesByUserSync("test_user");
        
        // Then
        assertEquals(1, expenses.size());
        assertEquals(100.0, expenses.get(0).monto, 0.01);
        assertEquals("test_user", expenses.get(0).userUid);
    }
    
    @Test
    public void softDeleteExpense() {
        // Given
        ExpenseEntity expense = new ExpenseEntity();
        expense.userUid = "test_user";
        expense.categoryRemoteId = "test_category";
        expense.monto = 100.0;
        expense.fechaEpochMillis = System.currentTimeMillis();
        expense.updatedAt = System.currentTimeMillis();
        expense.syncState = "PENDING";
        
        long id = expenseDao.insertExpense(expense);
        
        // When
        long deletedAt = System.currentTimeMillis();
        expenseDao.softDeleteExpense(id, deletedAt, System.currentTimeMillis());
        List<ExpenseEntity> expenses = expenseDao.getExpensesByUserSync("test_user");
        
        // Then
        assertEquals(0, expenses.size());
    }
}


