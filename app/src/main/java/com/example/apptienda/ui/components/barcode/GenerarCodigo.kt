package com.example.apptienda.ui.components.barcode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.apptienda.domain.model.Producto
import com.google.zxing.BarcodeFormat
import com.google.zxing.oned.Code128Writer
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.properties.TextAlignment
import java.io.ByteArrayOutputStream
import java.io.File
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Table

class BarcodeGenerator(private val context: Context) {

    fun calculateCodesPerPage(): Int {
        val pageWidth = PageSize.A4.width - MARGIN_X
        val pageHeight = PageSize.A4.height - MARGIN_Y

        val columnsPerPage = (pageWidth / (BARCODE_WIDTH + SPACING)).toInt()
        val rowsPerPage = (pageHeight / (BARCODE_HEIGHT + TEXT_HEIGHT + SPACING)).toInt()

        return columnsPerPage * rowsPerPage
    }

    fun generateBarcodesAndPDF(productos: List<Producto>): Uri {
        if (productos.isEmpty()) {
            throw IllegalArgumentException("La lista de productos no puede estar vacía")
        }

        val outputFile = createOutputFile()

        try {
            generatePDF(productos, outputFile)
            return getUriForFile(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error generando PDF: ${e.message}")
            throw e
        }
    }

    private fun createOutputFile(): File {
        val fileName = "codigos_barra_${System.currentTimeMillis()}.pdf"
        return File(context.getExternalFilesDir(null), fileName)
    }

    private fun generatePDF(productos: List<Producto>, outputFile: File) {
        val pdfWriter = PdfWriter(outputFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument, PageSize.A4).apply {
            setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT)
        }

        try {
            val table = createTable()
            addProductsToTable(productos, table)
            document.add(table)
        } finally {
            document.close()
        }
    }

    private fun createTable(): Table {
        val columnsPerPage = (PageSize.A4.width - MARGIN_X) / (BARCODE_WIDTH + SPACING)
        return Table(columnsPerPage.toInt()).apply {
            setMarginTop(0f)
            setMarginBottom(0f)
        }
    }

    private fun addProductsToTable(productos: List<Producto>, table: Table) {
        productos.forEach { producto ->
            val barcodeImage = generateBarcodeImage(producto.idNumerico)
            val cell = createTableCell(barcodeImage, producto.idNumerico)
            table.addCell(cell)
        }
    }

    private fun generateBarcodeImage(idNumerico: String): Bitmap {
        val barcodeWriter = Code128Writer()
        val bitMatrix = barcodeWriter.encode(
            idNumerico,
            BarcodeFormat.CODE_128,
            BARCODE_MATRIX_WIDTH,
            BARCODE_MATRIX_HEIGHT
        )

        val targetWidth = BARCODE_TARGET_WIDTH
        val padding = (targetWidth - bitMatrix.width) / 2
        return createBitmapFromMatrix(bitMatrix, targetWidth, padding)
    }

    private fun createBitmapFromMatrix(
        bitMatrix: com.google.zxing.common.BitMatrix,
        targetWidth: Int,
        padding: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            targetWidth,
            bitMatrix.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            eraseColor(0xFFFFFFFF.toInt())
        }

        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix[x, y]) {
                    bitmap.setPixel(x + padding, y, 0xFF000000.toInt())
                }
            }
        }

        return bitmap
    }

    private fun createTableCell(bitmap: Bitmap, idNumerico: String): Cell {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val image = Image(ImageDataFactory.create(stream.toByteArray())).apply {
            setWidth(BARCODE_WIDTH)
            setHeight(BARCODE_HEIGHT)
            setAutoScale(true)
        }

        return Cell().apply {
            setPadding(CELL_PADDING)
            setMargin(0f)
            add(image)
            add(
                Paragraph(idNumerico).apply {
                    setFontSize(FONT_SIZE)
                    setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                }
            )
        }
    }

    private fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    companion object {
        private const val TAG = "BarcodeGenerator"
        private const val MARGIN_X = 20f
        private const val MARGIN_Y = 20f
        private const val MARGIN_TOP = 10f
        private const val MARGIN_RIGHT = 10f
        private const val MARGIN_BOTTOM = 10f
        private const val MARGIN_LEFT = 10f
        private const val BARCODE_WIDTH = 56.7f // 2cm
        private const val BARCODE_HEIGHT = 28.35f // 1cm
        private const val TEXT_HEIGHT = 10f
        private const val SPACING = 5f
        private const val CELL_PADDING = 2f
        private const val FONT_SIZE = 6f
        private const val BARCODE_MATRIX_WIDTH = 300
        private const val BARCODE_MATRIX_HEIGHT = 100
        private const val BARCODE_TARGET_WIDTH = 400
    }
}