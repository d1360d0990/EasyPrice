package com.example.easyprice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// El adapter toma una lista inicial y un "callback" para notificar cuando se hace clic en eliminar.
class FavoritesAdapter(
    private var favorites: MutableList<Product>,
    private val onDeleteClicked: (Product) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    // Esta clase interna representa las vistas para cada fila (nombre, precio, botón de borrar).
    class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.text_view_product_name)
        val productPrice: TextView = itemView.findViewById(R.id.text_view_product_price)
        val deleteButton: ImageView = itemView.findViewById(R.id.image_view_delete)
    }

    // Se llama para crear una nueva fila vacía.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.favorites_item, parent, false)
        return FavoritesViewHolder(view)
    }

    // Se llama para rellenar una fila con los datos de un producto.
    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val product = favorites[position]

        // Asignamos los datos del producto a las vistas.
        holder.productName.text = product.name ?: "Nombre no disponible"
        holder.productPrice.text = "$ ${product.price ?: "0.00"}"
        
        // Configuramos el botón de eliminar para que notifique a la Activity a través del callback.
        holder.deleteButton.setOnClickListener { 
            onDeleteClicked(product)
        }
    }

    // Devuelve el número total de elementos en la lista.
    override fun getItemCount(): Int = favorites.size

    // Nuevo método para actualizar la lista de datos del adapter de forma segura.
    fun updateData(newFavorites: List<Product>) {
        favorites.clear()
        favorites.addAll(newFavorites)
        notifyDataSetChanged() // Notifica a la RecyclerView que los datos han cambiado.
    }
}