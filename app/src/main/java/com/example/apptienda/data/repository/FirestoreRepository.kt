package com.example.apptienda.data.repository

import android.util.Log
import androidx.annotation.Keep
import com.example.apptienda.domain.model.Categoria
import com.example.apptienda.domain.model.Producto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
@Keep
class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productosCollection = db.collection("productos")
    private val categoriasCollection = db.collection("categorias")
    private val contadoresCollection = db.collection("contadores")
    private val camposCollection = db.collection("campos")

    // Productos
    fun getProductos(): Flow<List<Producto>> = callbackFlow {
        val subscription = productosCollection
            .orderBy("idOrdenNumerico", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error obteniendo productos: ${error.message}")
                    return@addSnapshotListener
                }

                val productos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Producto::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(productos)
            }

        awaitClose { subscription.remove() }
    }

    fun getCategorias(): Flow<List<Categoria>> = callbackFlow {
        val subscription = categoriasCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error obteniendo categorías: ${error.message}")
                    return@addSnapshotListener
                }

                val categorias = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Categoria::class.java)?.copy(id = doc.id)
                }?.sortedWith(
                    compareBy { it.nombre.lowercase() }
                ) ?: emptyList()

                trySend(categorias)
            }

        awaitClose { subscription.remove() }
    }

    fun getCampos(): Flow<List<String>> = callbackFlow {
        val subscription = camposCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error obteniendo campos: ${error.message}")
                    return@addSnapshotListener
                }

                val campos = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("nombre")
                } ?: emptyList()

                trySend(campos)
            }

        awaitClose { subscription.remove() }
    }

    suspend fun agregarProducto(producto: Producto): Result<String> = try {
        val docRef = productosCollection.add(producto).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Log.e(TAG, "Error agregando producto: ${e.message}")
        Result.failure(e)
    }

    suspend fun actualizarProducto(producto: Producto): Result<Unit> = try {
        productosCollection
            .document(producto.id)
            .set(producto, SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error actualizando producto: ${e.message}")
        Result.failure(e)
    }

    suspend fun eliminarProducto(productoId: String): Result<Unit> = try {
        productosCollection
            .document(productoId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error eliminando producto: ${e.message}")
        Result.failure(e)
    }

    suspend fun agregarCategoria(categoria: Categoria): Result<String> = try {
        val docRef = categoriasCollection.add(categoria).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Log.e(TAG, "Error agregando categoría: ${e.message}")
        Result.failure(e)
    }

    suspend fun obtenerSiguienteOrden(): Long {
        return try {
            val contadorDoc = contadoresCollection.document("ordenProductos")
            db.runTransaction { transaction ->
                val snapshot = transaction.get(contadorDoc)
                val currentId = snapshot.getLong("ultimoOrden") ?: 0L
                val nextId = currentId + 1L
                transaction.set(contadorDoc, hashMapOf("ultimoOrden" to nextId))
                nextId
            }.await()
        } catch (e: Exception) {
            val contadorDoc = contadoresCollection.document("ordenProductos")
            val snapshot = contadorDoc.get().await()
            val lastId = snapshot.getLong("ultimoOrden") ?: 0L
            val nextId = lastId + 1L
            contadorDoc.set(hashMapOf("ultimoOrden" to nextId)).await()
            nextId
        }
    }

    companion object {
        private const val TAG = "FirestoreRepository"
    }
}