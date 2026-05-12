package com.example.globalbiblion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SolicitudAdapter(
    private val lista: List<SolicitudPendiente>,
    private val onClick: (SolicitudPendiente) -> Unit
) : RecyclerView.Adapter<SolicitudAdapter.SolicitudViewHolder>() {

    class SolicitudViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLibro: TextView = itemView.findViewById(R.id.tvLibro)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
        val tvIdiomas: TextView = itemView.findViewById(R.id.tvIdiomas)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitudViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud_pendiente, parent, false)
        return SolicitudViewHolder(vista)
    }

    override fun onBindViewHolder(holder: SolicitudViewHolder, position: Int) {
        val solicitud = lista[position]

        holder.tvLibro.text = solicitud.bookTitle
        holder.tvUsuario.text = "Usuario: ${solicitud.userName}"
        holder.tvTipo.text = "Tipo: ${solicitud.requestType}"
        holder.tvIdiomas.text = "${solicitud.sourceLanguage} → ${solicitud.targetLanguage}"
        holder.tvEstado.text = "Estado: ${solicitud.status} | ${solicitud.createdAt}"

        holder.itemView.setOnClickListener {
            onClick(solicitud)
        }
    }

    override fun getItemCount(): Int = lista.size
}