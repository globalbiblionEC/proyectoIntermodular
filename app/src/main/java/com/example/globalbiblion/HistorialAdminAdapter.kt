package com.example.globalbiblion

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistorialAdminAdapter(
    private val lista: List<HistorialAdmin>,
    private val onClick: (HistorialAdmin) -> Unit
) : RecyclerView.Adapter<HistorialAdminAdapter.HistorialViewHolder>() {

    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contenedor: LinearLayout = itemView.findViewById(R.id.contenedorHistorial)
        val tvEtiqueta: TextView = itemView.findViewById(R.id.tvEtiqueta)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloHistorial)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuarioHistorial)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoHistorial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_admin, parent, false)
        return HistorialViewHolder(vista)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val item = lista[position]

        holder.tvEtiqueta.text = item.tipo
        holder.tvTitulo.text = item.titulo
        holder.tvUsuario.text = "Usuario: ${item.usuario}"

        if (item.estado == "verified" || item.estado == "approved") {
            holder.tvEstado.text = "Validado / Aprobado"
            holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))
            holder.tvEtiqueta.setBackgroundColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvEstado.text = "Rechazado"
            holder.tvEstado.setTextColor(Color.parseColor("#C62828"))
            holder.tvEtiqueta.setBackgroundColor(Color.parseColor("#C62828"))
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = lista.size
}