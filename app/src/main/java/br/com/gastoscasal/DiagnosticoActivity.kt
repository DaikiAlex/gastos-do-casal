package br.com.gastoscasal

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Mostra as últimas notificações dos bancos e o que o app fez com cada uma. */
class DiagnosticoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
        val itens = Db.get(this).ultimasNotificacoes()
        val relatorio = if (itens.isEmpty()) {
            "Nenhuma notificação de banco capturada ainda.\n\n" +
                "Confira se a permissão está ativa e faça uma compra ou Pix de teste."
        } else {
            itens.joinToString("\n\n") {
                "[${it.status.uppercase()}] ${it.banco} · ${fmt.format(Date(it.dataMs))}\n${it.titulo}\n${it.texto}"
            }
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val texto = TextView(this).apply {
            text = relatorio
            setTextIsSelectable(true)
            setPadding(pad, pad, pad, pad)
            textSize = 14f
        }
        val copiar = Button(this).apply {
            text = "Copiar tudo"
            setOnClickListener {
                val cm = getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("notificacoes", relatorio))
                Toast.makeText(this@DiagnosticoActivity, "Copiado", Toast.LENGTH_SHORT).show()
            }
        }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(copiar)
            addView(ScrollView(this@DiagnosticoActivity).apply { addView(texto) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(raiz)
    }
}
