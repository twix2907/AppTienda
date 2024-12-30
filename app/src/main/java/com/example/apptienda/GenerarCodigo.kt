package com.example.apptienda

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
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
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Table
import com.itextpdf.kernel.geom.Rectangle

class BarcodeGenerator(private val context: Context) {

    fun generateBarcodesAndPDF(productos: List<Producto>): Uri {
        val fileName = "codigos_barra_${System.currentTimeMillis()}.pdf"
        val outputFile = File(context.getExternalFilesDir(null), fileName)

        val pdfWriter = PdfWriter(outputFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument, PageSize.A4)

        try {
            // Configurar márgenes mínimos
            document.setMargins(10f, 10f, 10f, 10f)

            // Calcular dimensiones para la cuadrícula
            val pageWidth = PageSize.A4.width - 20f // Ancho de página menos márgenes
            val pageHeight = PageSize.A4.height - 20f // Alto de página menos márgenes

            // Dimensiones del código de barras (1cm x 2cm en puntos)
            val barcodeWidth = 56.7f // 2cm
            val barcodeHeight = 28.35f // 1cm
            val textHeight = 10f // Altura para el texto del ID

            // Calcular número de columnas y filas que caben en la página
            val columnsPerPage = (pageWidth / (barcodeWidth + 5)).toInt() // 5 puntos de espacio entre columnas
            val rowsPerPage = (pageHeight / (barcodeHeight + textHeight + 5)).toInt() // 5 puntos de espacio entre filas

            // Crear tabla
            val table = Table(columnsPerPage)
            table.setMarginTop(0f)
            table.setMarginBottom(0f)

            productos.forEach { producto ->
                // Generar código de barras
                val barcodeWriter = Code128Writer()
                val bitMatrix = barcodeWriter.encode(
                    producto.idNumerico.toString(),
                    BarcodeFormat.CODE_128,
                    200,
                    100
                )

                // Convertir a bitmap
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                    }
                }

                // Convertir bitmap a imagen para PDF
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val image = Image(ImageDataFactory.create(stream.toByteArray()))
                image.setWidth(barcodeWidth)
                image.setHeight(barcodeHeight)

                // Crear celda con código de barras y ID
                val cell = Cell().apply {
                    setPadding(2f)
                    setMargin(0f)
                    add(image)
                    add(Paragraph(producto.idNumerico.toString()).apply {
                        setFontSize(6f)
                        setTextAlignment(TextAlignment.CENTER)
                    })
                }
                table.addCell(cell)
            }

            document.add(table)
            document.close()
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}