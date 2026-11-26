package br.com.gastoscasal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class Gasto(
    val id: Long,
    val uid: String,
    val pessoa: String,
    val centavos: Long,
    val descricao: String,
    val banco: String,
    val dataMs: Long,
    val origem: String,
    val tipo: String,
    val forma: String,
)

data class NotifBruta(
    val banco: String,
    val titulo: String,
    val texto: String,
    val dataMs: Long,
    val status: String,
)

class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "gastos.db", null, 2) {

    companion object {
        @Volatile private var instancia: Db? = null
        fun get(ctx: Context): Db =
            instancia ?: synchronized(this) {
                instancia ?: Db(ctx.applicationContext).also { instancia = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE gastos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid TEXT NOT NULL UNIQUE,
                pessoa TEXT NOT NULL,
                centavos INTEGER NOT NULL,
                descricao TEXT NOT NULL,
                banco TEXT NOT NULL,
                data_ms INTEGER NOT NULL,
                origem TEXT NOT NULL,
                assinatura TEXT,
                sincronizado INTEGER NOT NULL DEFAULT 0,
                tipo TEXT NOT NULL DEFAULT 'saida',
                forma TEXT NOT NULL DEFAULT ''
            )"""
        )
        db.execSQL("CREATE INDEX idx_gastos_data ON gastos(data_ms)")
        db.execSQL(
            """CREATE TABLE notificacoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                banco TEXT, titulo TEXT, texto TEXT, data_ms INTEGER, status TEXT
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE gastos ADD COLUMN tipo TEXT NOT NULL DEFAULT 'saida'")
            db.execSQL("ALTER TABLE gastos ADD COLUMN forma TEXT NOT NULL DEFAULT ''")
        }
    }

    fun inserirGasto(
        pessoa: String, centavos: Long, descricao: String, banco: String,
        dataMs: Long, origem: String, assinatura: String?,
        tipo: String, forma: String,
    ): Long {
        val v = ContentValues().apply {
            put("uid", UUID.randomUUID().toString())
            put("pessoa", pessoa)
            put("centavos", centavos)
            put("descricao", descricao)
            put("banco", banco)
            put("data_ms", dataMs)
            put("origem", origem)
            put("assinatura", assinatura)
            put("tipo", tipo)
            put("forma", forma)
        }
        return writableDatabase.insert("gastos", null, v)
    }

    fun existeAssinaturaRecente(assinatura: String, desdeMs: Long): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM gastos WHERE assinatura = ? AND data_ms >= ? LIMIT 1",
            arrayOf(assinatura, desdeMs.toString()),
        ).use { it.moveToFirst() }

    fun gastosEntre(inicioMs: Long, fimMs: Long): List<Gasto> {
        val lista = mutableListOf<Gasto>()
        readableDatabase.rawQuery(
            "SELECT id, uid, pessoa, centavos, descricao, banco, data_ms, origem, tipo, forma FROM gastos " +
                "WHERE data_ms >= ? AND data_ms < ? ORDER BY data_ms DESC",
            arrayOf(inicioMs.toString(), fimMs.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Gasto(
                        c.getLong(0), c.getString(1), c.getString(2), c.getLong(3),
                        c.getString(4), c.getString(5), c.getLong(6), c.getString(7),
                        c.getString(8), c.getString(9),
                    )
                )
            }
        }
        return lista
    }

    fun excluirGasto(id: Long) {
        writableDatabase.delete("gastos", "id = ?", arrayOf(id.toString()))
    }

    fun registrarNotificacao(banco: String, titulo: String, texto: String, status: String) {
        val db = writableDatabase
        db.insert("notificacoes", null, ContentValues().apply {
            put("banco", banco)
            put("titulo", titulo)
            put("texto", texto)
            put("data_ms", System.currentTimeMillis())
            put("status", status)
        })
        db.execSQL(
            "DELETE FROM notificacoes WHERE id NOT IN " +
                "(SELECT id FROM notificacoes ORDER BY id DESC LIMIT 300)"
        )
    }

    fun ultimasNotificacoes(): List<NotifBruta> {
        val lista = mutableListOf<NotifBruta>()
        readableDatabase.rawQuery(
            "SELECT banco, titulo, texto, data_ms, status FROM notificacoes ORDER BY id DESC LIMIT 100",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    NotifBruta(
                        c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: "",
                        c.getLong(3), c.getString(4) ?: "",
                    )
                )
            }
        }
        return lista
    }
}
