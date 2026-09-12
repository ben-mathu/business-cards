package com.benatt.businesscards.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.local.dao.VCardDao
import com.benatt.businesscards.data.local.entity.VCardEntity
import com.benatt.businesscards.data.parser.VCardParser
import com.benatt.businesscards.data.parser.VCardSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Helper utilities and DAO extensions for directly importing and saving .vcf files
 * into Room without needing a repository layer.
 */
object VCardImporter {

    /**
     * Extracts a .vcf [Uri] from an incoming Android [Intent] (handles ACTION_VIEW, ACTION_SEND).
     */
    fun extractVcfUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri ?: intent.data
            }
            else -> intent.data
        }
    }

    /**
     * Extracts raw text from an incoming Android [Intent] if shared as text.
     */
    fun extractVcfText(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_SEND) {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }
}

/**
 * Directly reads an [InputStream] containing .vcf data, parses all cards,
 * and saves them using [VCardDao].
 */
suspend fun VCardDao.importAndSaveVcf(
    inputStream: InputStream,
    preserveRawVcf: Boolean = true
): List<VCardDto> = withContext(Dispatchers.IO) {
    val rawText = inputStream.bufferedReader().use { it.readText() }
    importAndSaveVcf(rawText, preserveRawVcf)
}

/**
 * Directly reads a .vcf file from an Android [Uri], parses all cards,
 * and saves them using [VCardDao].
 */
suspend fun VCardDao.importAndSaveVcf(
    context: Context,
    uri: Uri,
    preserveRawVcf: Boolean = true
): List<VCardDto> = withContext(Dispatchers.IO) {
    val stream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Could not open input stream for URI: $uri")
    importAndSaveVcf(stream, preserveRawVcf)
}

/**
 * Directly reads a .vcf string, parses all cards, and saves them using [VCardDao].
 */
suspend fun VCardDao.importAndSaveVcf(
    vcfContent: String,
    preserveRawVcf: Boolean = true
): List<VCardDto> = withContext(Dispatchers.IO) {
    val dtos = VCardParser.parse(vcfContent)
    if (dtos.isNotEmpty()) {
        val entities = dtos.map { dto ->
            val raw = if (preserveRawVcf) {
                if (dtos.size == 1) vcfContent else VCardSerializer.serialize(dto)
            } else null
            VCardEntity.fromDto(dto, rawVcf = raw)
        }
        insertCards(entities)
    }
    dtos
}

/**
 * Handles an incoming [Intent] (e.g. from share sheet or file open),
 * reads its .vcf content, and saves it directly to the database.
 */
suspend fun VCardDao.importAndSaveFromIntent(
    context: Context,
    intent: Intent?,
    preserveRawVcf: Boolean = true
): List<VCardDto> = withContext(Dispatchers.IO) {
    val uri = VCardImporter.extractVcfUri(intent)
    if (uri != null) {
        return@withContext importAndSaveVcf(context, uri, preserveRawVcf)
    }

    val text = VCardImporter.extractVcfText(intent)
    if (!text.isNullOrBlank()) {
        return@withContext importAndSaveVcf(text, preserveRawVcf)
    }

    emptyList()
}
