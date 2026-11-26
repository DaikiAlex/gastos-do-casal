package br.com.gastoscasal

import java.math.BigDecimal
import java.math.RoundingMode

/** Lê o texto de uma notificação de banco e decide se é uma movimentação (entrada ou saída). */
object Parser {

    /** Pacotes dos apps de banco no Android -> nome exibido. */
    val BANCOS = mapOf(
        "com.nu.production" to "Nubank",
        "com.picpay" to "PicPay",
        "br.com.neon" to "Neon",
        "com.mercadopago.wallet" to "Mercado Pago",
    )

    const val ENTRADA = "entrada"
    const val SAIDA = "saida"

    data class Resultado(
        val tipo: String,
        val centavos: Long,
        val descricao: String,
        val forma: String,
    )

    // Não é movimentação: avisos, tentativas que falharam, pedidos, fatura.
    // (Pagamento de fatura fica de fora para não contar a compra no crédito duas vezes.)
    private val IGNORAR = listOf(
        "negad", "recusad", "não aprovad", "nao aprovad", "cancelad", "agendad",
        "pedido de pix", "solicit", "cobrança", "cobranca", "fatura", "limite",
        "lembrete", "vence", "vencimento", "código", "codigo", "senha", "token",
    )

    // Dinheiro entrando.
    private val ENTRADAS = listOf(
        "receb", "estorn", "reembols", "cashback", "depósit", "deposit",
        "devolv", "te transferiu", "te enviou", "caiu na", "rendeu", "rendimento",
    )

    // Dinheiro saindo.
    private val SAIDAS = listOf(
        "compra", "pagamento", "pagou", "enviou", "enviad", "envio", "pix enviado",
        "débito", "debito", "saque", "transferiu", "pago", "transferência realizada",
        "transferencia realizada",
    )

    private val reValor =
        Regex("""R\$\s*(\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?|\d+(?:,\d{1,2})?)""")

    // Saídas: "... em LOJA ..." ou "... para FULANO ..."
    private val reEm = Regex(
        """\bem\s+(.+?)(?=\s+(?:para o|para seu|para sua|no|com o|com final)\s|\s+às\s|\.(?:\s|$)|,|\n|$)""",
        RegexOption.IGNORE_CASE,
    )
    private val rePara = Regex(
        """\bpara\s+(.+?)(?=\s+(?:no|com|via)\s|\.(?:\s|$)|,|\n|$)""",
        RegexOption.IGNORE_CASE,
    )

    // Entradas: "... de FULANO ..." (nome começando com maiúscula, não "de R$")
    private val reDe = Regex(
        """\bde\s+(?!R\$)([A-ZÀ-Ú][^.,\n]*?)(?=\s+(?:CPF|CNPJ|no valor|via)\b|\.(?:\s|$)|,|\n|$)"""
    )
    // Mercado Pago: "O valor que Fulano te transferiu"
    private val reQueTe = Regex("""\bque\s+(.+?)\s+te\s+(?:transferiu|enviou)""", RegexOption.IGNORE_CASE)

    // Remove " CPF ***.592.732-**" / " CNPJ ..." depois do nome.
    private val reDocumento = Regex("""\s+(?:CPF|CNPJ)\b.*$""", RegexOption.IGNORE_CASE)

    fun analisar(tituloOriginal: String, textoOriginal: String): Resultado? {
        val titulo = tituloOriginal.replace(' ', ' ').trim()
        val texto = textoOriginal.replace(' ', ' ').trim()
        val completo = "$titulo. $texto"
        val minusculo = completo.lowercase()

        if (IGNORAR.any { minusculo.contains(it) }) return null
        val tipo = when {
            ENTRADAS.any { minusculo.contains(it) } -> ENTRADA
            SAIDAS.any { minusculo.contains(it) } -> SAIDA
            else -> return null
        }

        val valor = reValor.find(completo) ?: return null
        val centavos = paraCentavos(valor.groupValues[1]) ?: return null
        if (centavos <= 0) return null

        val corpo = texto.ifBlank { completo }
        val achado = if (tipo == ENTRADA) {
            (reQueTe.find(corpo) ?: reDe.find(corpo))?.groupValues?.get(1)
        } else {
            (reEm.find(corpo) ?: rePara.find(corpo))?.groupValues?.get(1)
        }
        val limpo = achado?.replace(reDocumento, "")?.trim()?.trimEnd('.', '!', ' ')
            ?.takeIf { it.length >= 2 && !it.contains("R$") }
        // PicPay põe o nome de quem mandou no título.
        val reserva = if (titulo.isNotBlank() && !titulo.contains("R$")) titulo
        else if (tipo == ENTRADA) "Entrada" else "Saída"
        val descricao = (limpo ?: reserva).take(40)

        return Resultado(tipo, centavos, descricao, forma(minusculo))
    }

    private fun forma(t: String): String = when {
        t.contains("crédito") || t.contains("credito") -> "Crédito"
        t.contains("débito") || t.contains("debito") -> "Débito"
        t.contains("pix") -> "Pix"
        t.contains("transfer") -> "Transferência"
        t.contains("boleto") -> "Boleto"
        t.contains("saque") -> "Saque"
        t.contains("compra") -> "Cartão"
        t.contains("rend") -> "Rendimento"
        else -> "Outro"
    }

    /** "1.234,56" -> 123456 ; "45" -> 4500 ; "45.90" -> 4590 */
    fun paraCentavos(bruto: String): Long? {
        val s = bruto.replace("R$", "").replace(" ", "").trim()
        if (s.isEmpty()) return null
        val normal = if (s.contains(',')) s.replace(".", "").replace(',', '.') else s
        return try {
            BigDecimal(normal).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
