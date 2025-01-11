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

    fun calculateCodesPerPage(): Int {
        // Dimensiones de página A4 en puntos
        val pageWidth = PageSize.A4.width - 20f // Ancho de página menos márgenes
        val pageHeight = PageSize.A4.height - 20f // Alto de página menos márgenes

        // Dimensiones del código de barras (1cm x 2cm en puntos)
        val barcodeWidth = 56.7f // 2cm
        val barcodeHeight = 28.35f // 1cm
        val textHeight = 10f // Altura para el texto del ID

        // Calcular número de columnas y filas que caben en la página
        val columnsPerPage = (pageWidth / (barcodeWidth + 5)).toInt()
        val rowsPerPage = (pageHeight / (barcodeHeight + textHeight + 5)).toInt()

        return columnsPerPage * rowsPerPage
    }

    fun generateBarcodesAndPDF(productos: List<Producto>): Uri {
        val fileName = "codigos_barra_${System.currentTimeMillis()}.pdf"
        val outputFile = File(context.getExternalFilesDir(null), fileName)

        val pdfWriter = PdfWriter(outputFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument, PageSize.A4)

        try {
            document.setMargins(10f, 10f, 10f, 10f)

            val pageWidth = PageSize.A4.width - 20f
            val pageHeight = PageSize.A4.height - 20f

            val barcodeWidth = 56.7f // 2cm
            val barcodeHeight = 28.35f // 1cm
            val textHeight = 10f

            val columnsPerPage = (pageWidth / (barcodeWidth + 5)).toInt()
            val rowsPerPage = (pageHeight / (barcodeHeight + textHeight + 5)).toInt()

            val table = Table(columnsPerPage)
            table.setMarginTop(0f)
            table.setMarginBottom(0f)

            productos.forEach { producto ->
                val barcodeWriter = Code128Writer()

                // Usar un ancho fijo grande para todos los códigos
                val bitMatrix = barcodeWriter.encode(
                    producto.idNumerico,
                    BarcodeFormat.CODE_128,
                    300,  // Ancho fijo para todos
                    100   // Altura fija
                )

                // Crear un bitmap más ancho para centrar el código
                val targetWidth = 400  // Ancho objetivo fijo para todos los códigos
                val padding = (targetWidth - bitMatrix.width) / 2
                val finalBitmap = Bitmap.createBitmap(targetWidth, bitMatrix.height, Bitmap.Config.ARGB_8888)

                // Llenar el bitmap con fondo blanco
                finalBitmap.eraseColor(0xFFFFFFFF.toInt())

                // Copiar el código de barras al centro del bitmap
                for (x in 0 until bitMatrix.width) {
                    for (y in 0 until bitMatrix.height) {
                        if (bitMatrix[x, y]) {
                            finalBitmap.setPixel(x + padding, y, 0xFF000000.toInt())
                        }
                    }
                }

                // Crear imagen para PDF usando el bitmap centrado
                val stream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val image = Image(ImageDataFactory.create(stream.toByteArray()))

                // Establecer dimensiones fijas para la imagen en el PDF
                image.setWidth(barcodeWidth)
                image.setHeight(barcodeHeight)
                image.setAutoScale(true)

                // Crear celda
                val cell = Cell().apply {
                    setPadding(2f)
                    setMargin(0f)
                    add(image)
                    add(Paragraph(producto.idNumerico).apply {
                        setFontSize(6f)
                        setTextAlignment(TextAlignment.CENTER)
                    })
                }
                table.addCell(cell)
            }

            document.add(table)
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