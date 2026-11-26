package br.com.gastoscasal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GastosAdapter(
    private val moeda: NumberFormat,
    private val aoSegurar: (Gasto) -> Unit,
) : RecyclerView.Adapter<GastosAdapter.Linha>() {

    private var itens: List<Gasto> = emptyList()

    companion object {
        val VERDE = Color.parseColor("#2E7D32")
        val VERMELHO = Color.parseColor("#C62828")
    }
    private val formatoData = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))

    class Linha(v: View) : RecyclerView.ViewHolder(v) {
        val descricao: TextView = v.findViewById(R.id.txtDescricao)
        val info: TextView = v.findViewById(R.id.txtInfo)
        val valor: TextView = v.findViewById(R.id.txtValor)
    }

    fun mostrar(lista: List<Gasto>) {
        itens = lista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha =
        Linha(LayoutInflater.from(parent.context).inflate(R.layout.item_gasto, parent, false))

    override fun getItemCount(): Int = itens.size

    override fun onBindViewHolder(h: Linha, position: Int) {
        val g = itens[position]
        h.descricao.text = g.descricao
        val origem = if (g.origem == "manual") "manual" else g.banco
        val forma = if (g.forma.isBlank()) "" else " · ${g.forma}"
        h.info.text = "${g.pessoa} · $origem$forma · ${formatoData.format(Date(g.dataMs))}"
        val entrada = g.tipo == Parser.ENTRADA
        h.valor.text = (if (entrada) "+ " else "− ") + moeda.format(g.centavos / 100.0)
        h.valor.setTextColor(if (entrada) VERDE else VERMELHO)
        h.itemView.setOnLongClickListener { aoSegurar(g); true }
    }
}
