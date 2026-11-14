package com.example.minimarket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class PriceLookupActivity : AppCompatActivity() {
    private lateinit var db: MarketDb
    private lateinit var barcodeInput: EditText
    private lateinit var resultName: TextView
    private lateinit var resultBarcode: TextView
    private lateinit var resultSypPrice: TextView
    private lateinit var resultUsdPrice: TextView
    private lateinit var resultMessage: TextView

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra(ScannerActivity.RESULT_BARCODE).orEmpty()
            if (code.isNotBlank()) barcodeInput.setText(code)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_price_lookup)
        db = MarketDb(this)
        barcodeInput = findViewById(R.id.lookupBarcodeInput)
        resultName = findViewById(R.id.lookupItemName)
        resultBarcode = findViewById(R.id.lookupItemBarcode)
        resultSypPrice = findViewById(R.id.lookupItemPrice)
        resultUsdPrice = findViewById(R.id.lookupItemUsdPrice)
        resultMessage = findViewById(R.id.lookupMessage)

        findViewById<Button>(R.id.lookupScanButton).setOnClickListener { scannerLauncher.launch(Intent(this, ScannerActivity::class.java)) }
        findViewById<Button>(R.id.lookupClearButton).setOnClickListener {
            barcodeInput.text.clear()
            clearResult("امسح باركود مادة مسجلة أو أدخل الرقم يدوياً")
        }
        findViewById<Button>(R.id.lookupBackButton).setOnClickListener { finish() }

        barcodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val code = s?.toString()?.trim().orEmpty()
                if (code.isBlank()) clearResult("امسح باركود مادة مسجلة أو أدخل الرقم يدوياً") else showPrice(code)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun showPrice(code: String) {
        val item = db.findByBarcode(code)
        if (item == null) {
            resultName.text = ""
            resultBarcode.text = ""
            resultSypPrice.text = ""
            resultUsdPrice.text = ""
            resultMessage.text = "هذه المادة غير مسجلة. أضفها أولاً من واجهة إدارة المواد."
            return
        }
        resultMessage.text = "تم العثور على المادة"
        resultName.text = item.name
        resultBarcode.text = "الباركود: ${item.barcode}"
        resultSypPrice.text = "${formatPrice(item.price)} ل.س"
        val rate = db.latestExchangeRate()
        resultUsdPrice.text = if (rate == null) {
            "أدخل سعر الصرف من واجهة سعر الصرف لعرض القيمة بالدولار"
        } else "${formatPrice(item.price / rate.sypPerUsd)} USD"
    }

    private fun clearResult(message: String) {
        resultName.text = ""
        resultBarcode.text = ""
        resultSypPrice.text = ""
        resultUsdPrice.text = ""
        resultMessage.text = message
    }

    private fun formatPrice(value: Double): String =
        if (value % 1.0 == 0.0) String.format(Locale.US, "%,.0f", value)
        else String.format(Locale.US, "%,.2f", value)
}