package com.example.minimarket

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExchangeRateActivity : AppCompatActivity() {
    private lateinit var db: MarketDb
    private lateinit var rateInput: EditText
    private lateinit var currentRate: TextView
    private lateinit var updatedAt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_rate)
        db = MarketDb(this)
        rateInput = findViewById(R.id.exchangeRateInput)
        currentRate = findViewById(R.id.currentExchangeRate)
        updatedAt = findViewById(R.id.exchangeRateUpdatedAt)
        findViewById<Button>(R.id.saveExchangeRateButton).setOnClickListener { saveRate() }
        findViewById<Button>(R.id.exchangeRateBackButton).setOnClickListener { finish() }
        refreshCurrentRate()
    }

    private fun saveRate() {
        val value = rateInput.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (value == null || !value.isFinite() || value <= 0.0) {
            Toast.makeText(this, "أدخل سعر صرف صحيحاً أكبر من صفر", Toast.LENGTH_LONG).show()
            return
        }
        try {
            db.saveExchangeRate(value)
            rateInput.text.clear()
            refreshCurrentRate()
            Toast.makeText(this, "تم حفظ سعر الصرف الجديد", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر حفظ سعر الصرف: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshCurrentRate() {
        val rate = db.latestExchangeRate()
        if (rate == null) {
            currentRate.text = "لم يتم إدخال سعر صرف بعد"
            updatedAt.text = "أدخل قيمة الليرة السورية مقابل دولار واحد"
        } else {
            currentRate.text = "1 USD = ${format(rate.sypPerUsd)} ل.س"
            updatedAt.text = "آخر تحديث: " + SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(rate.updatedAt))
        }
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) String.format(Locale.US, "%,.0f", value)
        else String.format(Locale.US, "%,.2f", value)
}