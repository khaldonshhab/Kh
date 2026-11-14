package com.example.minimarket

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MarketItem(
    val id: Long,
    val barcode: String,
    val name: String,
    val price: Double
)

class MarketDb(context: Context) :
    SQLiteOpenHelper(context, "market.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                barcode TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                price REAL NOT NULL CHECK(price >= 0)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Keep existing data when schema versions are added.
    }

    fun all(): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        readableDatabase.rawQuery(
            "SELECT id, barcode, name, price FROM items ORDER BY name COLLATE NOCASE",
            null
        ).use { c ->
            while (c.moveToNext()) {
                result.add(
                    MarketItem(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3))
                )
            }
        }
        return result
    }

    fun findByBarcode(barcode: String): MarketItem? {
        readableDatabase.query(
            "items", arrayOf("id", "barcode", "name", "price"),
            "barcode = ?", arrayOf(barcode), null, null, null
        ).use { c ->
            if (c.moveToFirst()) {
                return MarketItem(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3))
            }
        }
        return null
    }

    fun save(id: Long?, barcode: String, name: String, price: Double): Long {
        val values = ContentValues().apply {
            put("barcode", barcode)
            put("name", name)
            put("price", price)
        }
        return if (id == null) {
            writableDatabase.insertOrThrow("items", null, values)
        } else {
            val changed = writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
            if (changed == 0) throw IllegalStateException("المادة غير موجودة")
            id
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("items", "id = ?", arrayOf(id.toString()))
    }

    fun replaceAll(rows: List<Triple<String, String, Double>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("items", null, null)
            rows.forEach { (barcode, name, price) ->
                val v = ContentValues().apply {
                    put("barcode", barcode)
                    put("name", name)
                    put("price", price)
                }
                db.insertOrThrow("items", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun merge(rows: List<Triple<String, String, Double>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            rows.forEach { (barcode, name, price) ->
                val v = ContentValues().apply {
                    put("barcode", barcode)
                    put("name", name)
                    put("price", price)
                }
                db.insertWithOnConflict("items", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
