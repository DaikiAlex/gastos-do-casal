package br.com.gastoscasal

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificacaoService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val banco = Parser.BANCOS[sbn.packageName] ?: return
        val n = sbn.notification ?: return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = n.extras ?: return
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val texto = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (titulo.isBlank() && texto.isBlank()) return

        try {
            val db = Db.get(this)
            val r = Parser.analisar(titulo, texto)
            if (r == null) {
                db.registrarNotificacao(banco, titulo, texto, "ignorada")
                return
            }
            val agora = System.currentTimeMillis()
            val assinatura = "$banco|${r.tipo}|${r.centavos}|${r.descricao}"
            // O mesmo aviso costuma chegar mais de uma vez: ignora repetição em 3 minutos.
            if (db.existeAssinaturaRecente(assinatura, agora - 3 * 60_000L)) {
                db.registrarNotificacao(banco, titulo, texto, "repetida")
                return
            }
            val pessoa = Prefs.pessoa(this).ifBlank { "Sem nome" }
            db.inserirGasto(pessoa, r.centavos, r.descricao, banco, agora, "automatico", assinatura, r.tipo, r.forma)
            db.registrarNotificacao(banco, titulo, texto, "registrada")
            Eventos.avisar()
        } catch (e: Exception) {
            // Nunca derrubar o serviço por causa de uma notificação estranha.
            runCatching { Db.get(this).registrarNotificacao(banco, titulo, texto, "erro: ${e.message}") }
        }
    }
}
