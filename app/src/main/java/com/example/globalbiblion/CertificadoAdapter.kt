package com.example.globalbiblion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CertificadoAdapter(
    private val lista: List<CertificadoPendiente>,
    private val onClick: (CertificadoPendiente) -> Unit
) : RecyclerView.Adapter<CertificadoAdapter.CertificadoViewHolder>() {

    class CertificadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvRolEstado: TextView = itemView.findViewById(R.id.tvRolEstado)
        val tvCertificado: TextView = itemView.findViewById(R.id.tvCertificado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificadoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_certificado_pendiente, parent, false)
        return CertificadoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: CertificadoViewHolder, position: Int) {
        val certificado = lista[position]

        holder.tvNombre.text = certificado.nombreCompleto
        holder.tvEmail.text = certificado.email
        holder.tvRolEstado.text = "Rol: ${certificado.rol} | Estado: ${certificado.estado}"
        holder.tvCertificado.text =
            "Emisor: ${certificado.emisor} | Idioma: ${certificado.idioma} | Nivel: ${certificado.nivel}"

        holder.itemView.setOnClickListener {
            onClick(certificado)
        }
    }

    override fun getItemCount(): Int = lista.size
}