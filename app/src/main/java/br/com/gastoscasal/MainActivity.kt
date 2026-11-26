package br.com.gastoscasal

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: Db
    private lateinit var adaptador: GastosAdapter
    private val ptBR: Locale = Locale.forLanguageTag("pt-BR")
    private val moeda: NumberFormat = NumberFormat.getCurrencyInstance(ptBR)
    private val mes: Calendar = Calendar.getInstance()
    private val atualizar: () -> Unit = { carregar() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = Db.get(this)

        adaptador = GastosAdapter(moeda) { g -> confirmarExclusao(g) }
        findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = adaptador
        }

        findViewById<View>(R.id.btnMesAnterior).setOnClickListener { mes.add(Calendar.MONTH, -1); carregar() }
        findViewById<View>(R.id.btnMesProximo).setOnClickListener { mes.add(Calendar.MONTH, 1); carregar() }
        findViewById<View>(R.id.btnAtivar).setOnClickListener { abrirAcessoNotificacoes() }
        findViewById<View>(R.id.btnRestrito).setOnClickListener { explicarBloqueio() }
        findViewById<View>(R.id.btnAdicionar).setOnClickListener { dialogoNovoGasto() }

        if (Prefs.pessoa(this).isBlank()) escolherPessoa()
    }

    override fun onResume() {
        super.onResume()
        Eventos.ouvir(atualizar)
        carregar()
    }

    override fun onPause() {
        super.onPause()
        Eventos.parar(atualizar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.principal, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_pessoa -> escolherPessoa()
            R.id.menu_permissao -> abrirAcessoNotificacoes()
            R.id.menu_diagnostico -> startActivity(Intent(this, DiagnosticoActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ---------- Tela ----------

    private fun carregar() {
        val inicio = (mes.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fim = (inicio.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val lista = db.gastosEntre(inicio.timeInMillis, fim.timeInMillis)

        findViewById<TextView>(R.id.txtMes).text =
            SimpleDateFormat("MMMM 'de' yyyy", ptBR).format(inicio.time)
                .replaceFirstChar { it.uppercase() }

        val eu = Prefs.pessoa(this)
        supportActionBar?.subtitle = if (eu.isBlank()) null else "Celular de $eu"

        // Soma por pessoa (entrou / saiu) + total do casal
        val entrou = linkedMapOf<String, Long>()
        val saiu = linkedMapOf<String, Long>()
        if (eu.isNotBlank()) { entrou[eu] = 0L; saiu[eu] = 0L }
        for (g in lista) {
            entrou.putIfAbsent(g.pessoa, 0L)
            saiu.putIfAbsent(g.pessoa, 0L)
            if (g.tipo == Parser.ENTRADA) entrou[g.pessoa] = entrou.getValue(g.pessoa) + g.centavos
            else saiu[g.pessoa] = saiu.getValue(g.pessoa) + g.centavos
        }

        val caixa = findViewById<LinearLayout>(R.id.totais)
        caixa.removeAllViews()
        for (nome in entrou.keys) {
            caixa.addView(blocoTotal(nome, entrou.getValue(nome), saiu.getValue(nome), false))
        }
        if (entrou.size > 1 || eu.isBlank()) {
            caixa.addView(blocoTotal("Casal", entrou.values.sum(), saiu.values.sum(), true))
        }

        adaptador.mostrar(lista)
        findViewById<View>(R.id.txtVazio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.bannerPermissao).visibility =
            if (acessoAtivo()) View.GONE else View.VISIBLE
    }

    private fun blocoTotal(nome: String, entrou: Long, saiu: Long, destaque: Boolean): View {
        val bloco = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val saldo = entrou - saiu
        val cima = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cima.addView(TextView(this).apply {
            text = nome
            textSize = if (destaque) 20f else 17f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        cima.addView(TextView(this).apply {
            text = "Saldo " + (if (saldo < 0) "− " else "") + moeda.format(Math.abs(saldo) / 100.0)
            textSize = if (destaque) 18f else 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (saldo < 0) GastosAdapter.VERMELHO else GastosAdapter.VERDE)
        })
        bloco.addView(cima)
        val baixo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        baixo.addView(TextView(this).apply {
            text = "Entrou + " + moeda.format(entrou / 100.0)
            textSize = 14f
            setTextColor(GastosAdapter.VERDE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        baixo.addView(TextView(this).apply {
            text = "Saiu − " + moeda.format(saiu / 100.0)
            textSize = 14f
            setTextColor(GastosAdapter.VERMELHO)
        })
        bloco.addView(baixo)
        return bloco
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- Permissão ----------

    private fun acessoAtivo(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun abrirAcessoNotificacoes() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Abra Configurações > Notificações > Acesso a notificações", Toast.LENGTH_LONG).show()
        }
    }

    private fun explicarBloqueio() {
        AlertDialog.Builder(this)
            .setTitle("Permissão bloqueada?")
            .setMessage(
                "Em Android 13 ou mais novo, apps instalados fora da Play Store vêm com essa permissão travada.\n\n" +
                    "1. Toque em \"Abrir informações do app\"\n" +
                    "2. Toque nos três pontinhos (⋮) lá em cima\n" +
                    "3. Escolha \"Permitir configurações restritas\"\n" +
                    "4. Volte aqui e toque em \"Ativar\" de novo"
            )
            .setPositiveButton("Abrir informações do app") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    // ---------- Pessoa ----------

    private fun escolherPessoa() {
        val opcoes = arrayOf("Alex", "Mika", "Outro nome…")
        AlertDialog.Builder(this)
            .setTitle("Quem usa este celular?")
            .setCancelable(Prefs.pessoa(this).isNotBlank())
            .setItems(opcoes) { _, i ->
                if (i < 2) {
                    Prefs.definirPessoa(this, opcoes[i])
                    carregar()
                } else {
                    nomePersonalizado()
                }
            }
            .show()
    }

    private fun nomePersonalizado() {
        val campo = EditText(this).apply { hint = "Seu nome" }
        AlertDialog.Builder(this)
            .setTitle("Seu nome")
            .setView(envolver(campo))
            .setCancelable(false)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = campo.text.toString().trim()
                if (nome.isBlank()) escolherPessoa() else {
                    Prefs.definirPessoa(this, nome)
                    carregar()
                }
            }
            .show()
    }

    // ---------- Gastos manuais ----------

    private fun dialogoNovoGasto() {
        val valor = EditText(this).apply {
            hint = "Valor (ex: 25,90)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            keyListener = DigitsKeyListener.getInstance("0123456789,.")
        }
        val descricao = EditText(this).apply {
            hint = "Descrição (ex: Feira, Salário)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val grupo = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rbSaida = RadioButton(this).apply { id = View.generateViewId(); text = "Saída" }
        val rbEntrada = RadioButton(this).apply { id = View.generateViewId(); text = "Entrada" }
        grupo.addView(rbSaida)
        grupo.addView(rbEntrada)
        grupo.check(rbSaida.id)
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(grupo)
            addView(valor)
            addView(descricao)
        }
        AlertDialog.Builder(this)
            .setTitle("Nova movimentação (dinheiro, etc.)")
            .setView(caixa)
            .setPositiveButton("Salvar") { _, _ ->
                val centavos = Parser.paraCentavos(valor.text.toString())
                if (centavos == null || centavos <= 0) {
                    Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show()
                } else {
                    val pessoa = Prefs.pessoa(this).ifBlank { "Sem nome" }
                    val entrada = grupo.checkedRadioButtonId == rbEntrada.id
                    val desc = descricao.text.toString().trim()
                        .ifBlank { if (entrada) "Entrada" else "Saída" }
                    db.inserirGasto(pessoa, centavos, desc.take(40), "Manual",
                        System.currentTimeMillis(), "manual", null,
                        if (entrada) Parser.ENTRADA else Parser.SAIDA, "Dinheiro")
                    carregar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExclusao(g: Gasto) {
        AlertDialog.Builder(this)
            .setTitle("Apagar movimentação?")
            .setMessage("${g.descricao} — ${moeda.format(g.centavos / 100.0)}")
            .setPositiveButton("Apagar") { _, _ -> db.excluirGasto(g.id); carregar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun envolver(v: View): View = LinearLayout(this).apply {
        setPadding(dp(20), dp(8), dp(20), 0)
        addView(v, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
}
