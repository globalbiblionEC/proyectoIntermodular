package com.example.globalbiblion

import android.graphics.Color
import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//Acá creamos un RecyclerView Adapter para el historial del administrador
class HistorialAdminAdapter(//Creamos el adapter
    private val lista: List<HistorialAdmin>,
    private val onClick: (HistorialAdmin) -> Unit //Ventana superpuesta para ver detalles
) : RecyclerView.Adapter<HistorialAdminAdapter.HistorialViewHolder>() {//Inicialización del Recycler view

    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {//Tarjetas individuales
        val tvEtiqueta: TextView = itemView.findViewById(R.id.tvEtiqueta)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloHistorial)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuarioHistorial)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoHistorial) //Estado del proceso
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_admin, parent, false)//Acá llamamos al XML
        return HistorialViewHolder(vista)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val item = lista[position]

        holder.tvEtiqueta.text = item.tipo //Solicitud o certificado
        holder.tvTitulo.text = item.titulo
        holder.tvUsuario.text = "Usuario: ${item.usuario}"

        when (item.estado) {
            "verified", "approved", "published" -> {
                holder.tvEstado.text = "Validado / Aprobado"
                holder.tvEstado.setTextColor(Color.parseColor("#2E7D32"))//color según el estado
                holder.tvEtiqueta.setBackgroundColor(Color.parseColor("#2E7D32"))
            }

            "rejected", "changes_requested" -> {
                holder.tvEstado.text = "Rechazado"
                holder.tvEstado.setTextColor(Color.parseColor("#C62828"))
                holder.tvEtiqueta.setBackgroundColor(Color.parseColor("#C62828"))
            }

            "waiting_for_proofreader", "translation_vacancy_open" -> {
                holder.tvEstado.text = "Pendiente"
                holder.tvEstado.setTextColor(Color.parseColor("#F2C94C"))
                holder.tvEtiqueta.setBackgroundColor(Color.parseColor("#F2C94C"))
            }

            else -> {
                holder.tvEstado.text = item.estado
                holder.tvEstado.setTextColor(Color.DKGRAY)
                holder.tvEtiqueta.setBackgroundColor(Color.DKGRAY)
            }
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = lista.size
}