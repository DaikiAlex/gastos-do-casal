package br.com.gastoscasal

import android.content.Context
import android.os.Handler
import android.os.Looper

object Prefs {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("config", Context.MODE_PRIVATE)
    fun pessoa(ctx: Context): String = sp(ctx).getString("pessoa", "") ?: ""
    fun definirPessoa(ctx: Context, nome: String) {
        sp(ctx).edit().putString("pessoa", nome.trim()).apply()
    }
}

/** Avisa a tela aberta quando um gasto novo chega pela notificação. */
object Eventos {
    private val ouvintes = mutableSetOf<() -> Unit>()
    private val principal = Handler(Looper.getMainLooper())

    fun ouvir(f: () -> Unit) { ouvintes.add(f) }
    fun parar(f: () -> Unit) { ouvintes.remove(f) }
    fun avisar() { principal.post { ouvintes.toList().forEach { it() } } }
}
