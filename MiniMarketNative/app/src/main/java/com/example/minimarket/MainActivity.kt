package com.example.minimarket

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.minimarket.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: MarketDb
    private var editingId: Long? = null

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val code = result.data?.getStringExtra(ScannerActivity.RESULT_BARCODE).orEmpty()
                if (code.isNotBlank()) {
                    binding.barcodeInput.setText(code)
                    lookupBarcode(code)
                }
            }
        }

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) exportCsv(uri)
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importCsv(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = MarketDb(this)

        binding.scanButton.setOnClickListener {
            scannerLauncher.launch(Intent(this, ScannerActivity::class.java))
        }

        binding.barcodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val code = s?.toString()?.trim().orEmpty()
                if (code.isNotEmpty()) lookupBarcode(code)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.saveButton.setOnClickListener { saveItem() }
        binding.clearButton.setOnClickListener { clearForm() }
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderItems()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.exportButton.setOnClickListener {
            if (db.all().isEmpty()) {
                toast("لا توجد بيانات لتصديرها")
            } else {
                exportLauncher.launch("market_items_backup.csv")
            }
        }

        binding.importButton.setOnClickListener {
            importLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/csv"))
        }

        renderItems()
    }

    private fun lookupBarcode(code: String) {
        val found = db.findByBarcode(code)
        if (found != null) {
            editingId = found.id
            binding.nameInput.setText(found.name)
            binding.priceInput.setText(formatPrice(found.price))
            binding.formTitle.text = "تعديل مادة مسجلة مسبقاً"
        } else if (editingId != null) {
            val currentCode = db.all().firstOrNull { it.id == editingId }?.barcode
            if (currentCode != code) {
                editingId = null
                binding.nameInput.text.clear()
                binding.priceInput.text.clear()
                binding.formTitle.text = "إضافة مادة جديدة"
            }
        }
    }

    private fun saveItem() {
        val barcode = binding.barcodeInput.text.toString().trim()
        val name = binding.nameInput.text.toString().trim()
        val priceText = binding.priceInput.text.toString().trim().replace(',', '.')
        val price = priceText.toDoubleOrNull()

        if (barcode.isBlank() || name.isBlank() || price == null || !price.isFinite() || price < 0) {
            toast("أدخل الباركود والاسم وسعراً صحيحاً غير سالب")
            return
        }

        val duplicate = db.findByBarcode(barcode)
        if (duplicate != null && duplicate.id != editingId) {
            toast("هذا الباركود مسجل لمادة أخرى")
            return
        }

        try {
            db.save(editingId, barcode, name, price)
            toast("تم حفظ المادة")
            clearForm()
            renderItems()
        } catch (e: Exception) {
            toast("تعذر الحفظ: ${e.message ?: "خطأ غير معروف"}")
        }
    }

    private fun clearForm() {
        editingId = null
        binding.barcodeInput.text.clear()
        binding.nameInput.text.clear()
        binding.priceInput.text.clear()
        binding.formTitle.text = "إضافة مادة جديدة"
    }

    private fun renderItems() {
        val query = binding.searchInput.text.toString().trim().lowercase(Locale.ROOT)
        val all = db.all()
        val filtered = all.filter {
            it.name.lowercase(Locale.ROOT).contains(query) ||
                it.barcode.lowercase(Locale.ROOT).contains(query)
        }

        binding.totalItems.text = all.size.toString()
        binding.totalValue.text = formatPrice(all.sumOf { it.price })
        binding.resultsCount.text = "عدد النتائج: ${filtered.size}"
        binding.itemsContainer.removeAllViews()

        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لا توجد مواد مطابقة"
                gravity = Gravity.CENTER
                setPadding(12, 28, 12, 28)
                setTextColor(0xFF777777.toInt())
            }
            binding.itemsContainer.addView(empty)
            return
        }

        filtered.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(0xFFFFFFFF.toInt())
                elevation = 2f
            }

            val details = TextView(this).apply {
                text = "${item.name}\nالباركود: ${item.barcode}\nالسعر: ${formatPrice(item.price)}"
                textSize = 15f
                setTextColor(0xFF263445.toInt())
                setPadding(2, 2, 2, 8)
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val edit = Button(this).apply {
                text = "تعديل"
                setOnClickListener {
                    editingId = item.id
                    binding.barcodeInput.setText(item.barcode)
                    binding.nameInput.setText(item.name)
                    binding.priceInput.setText(formatPrice(item.price))
                    binding.formTitle.text = "تعديل المادة"
                    binding.scrollToTop()
                }
            }

            val delete = Button(this).apply {
                text = "حذف"
                setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("حذف المادة")
                        .setMessage("هل تريد حذف ${item.name}؟")
                        .setNegativeButton("إلغاء", null)
                        .setPositiveButton("حذف") { _, _ ->
                            db.delete(item.id)
                            if (editingId == item.id) clearForm()
                            renderItems()
                            toast("تم حذف المادة")
                        }.show()
                }
            }

            actions.addView(edit)
            actions.addView(delete)
            card.addView(details)
            card.addView(actions)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
            binding.itemsContainer.addView(card, params)
        }
    }

    private fun ActivityMainBinding.scrollToTop() {
        root.smoothScrollTo(0, 0)
    }

    private fun exportCsv(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write("\uFEFFالباركود,المادة,السعر\r\n")
                    db.all().forEach {
                        writer.write(
                            listOf(it.barcode, it.name, formatPrice(it.price))
                                .joinToString(",") { value -> csvEscape(value) } + "\r\n"
                        )
                    }
                }
            } ?: throw IllegalStateException("تعذر فتح ملف التصدير")
            toast("تم تصدير النسخة الاحتياطية")
        } catch (e: Exception) {
            toast("فشل التصدير: ${e.message}")
        }
    }

    private fun importCsv(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: throw IllegalStateException("تعذر قراءة الملف")

            val rows = parseCsv(text)
            val start = if (rows.firstOrNull()?.joinToString()?.contains("الباركود") == true) 1 else 0
            val valid = rows.drop(start).mapNotNull { row ->
                if (row.size < 3) return@mapNotNull null
                val barcode = row.first().trim()
                val price = row.last().trim().replace(',', '.').toDoubleOrNull()
                val name = row.subList(1, row.size - 1).joinToString(",").trim()
                if (barcode.isBlank() || name.isBlank() || price == null || !price.isFinite() || price < 0)
                    null
                else Triple(barcode, name, price)
            }.distinctBy { it.first }

            if (valid.isEmpty()) {
                toast("لم يتم العثور على بيانات صالحة")
                return
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("استيراد البيانات")
                .setMessage("تم العثور على ${valid.size} مادة. اختر طريقة الاستيراد.")
                .setNegativeButton("إلغاء", null)
                .setNeutralButton("دمج") { _, _ ->
                    try {
                        db.merge(valid)
                        renderItems()
                        toast("تم دمج البيانات")
                    } catch (e: Exception) {
                        toast("فشل الدمج: ${e.message}")
                    }
                }
                .setPositiveButton("استبدال") { _, _ ->
                    try {
                        db.replaceAll(valid)
                        clearForm()
                        renderItems()
                        toast("تم استبدال القائمة")
                    } catch (e: Exception) {
                        toast("فشل الاستيراد: ${e.message}")
                    }
                }.show()

        } catch (e: Exception) {
            toast("تعذر استيراد الملف: ${e.message}")
        }
    }

    private fun parseCsv(source: String): List<List<String>> {
        val text = source.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' && quoted && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    row.add(field.toString()); field.clear()
                }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(field.toString())
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                    field.clear()
                }
                else -> field.append(ch)
            }
            i++
        }
        if (quoted) throw IllegalArgumentException("ملف CSV غير مكتمل الاقتباس")
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }

    private fun csvEscape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    private fun formatPrice(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value)

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
