package com.benatt.businesscards.ui.nfc.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.provider.Settings
import java.io.IOException

sealed class NfcSendResult {
    data object Success : NfcSendResult()
    data object TagReadOnly : NfcSendResult()
    data class InsufficientCapacity(val requiredBytes: Int, val maxBytes: Int) : NfcSendResult()
    data object TagLost : NfcSendResult()
    data class Error(val message: String) : NfcSendResult()
}

object NfcSender {

    const val MAX_NFC_BYTES = 1024

    fun calculatePayloadSize(vCardString: String): Int {
        return vCardString.toByteArray(Charsets.UTF_8).size
    }

    fun calculateNdefSize(vCardString: String): Int {
        val payloadBytes = calculatePayloadSize(vCardString)
        // MIME NDEF record header for "text/vcard":
        // 1 byte header flags + 1 byte type_length + payload_length (1 byte if < 256 else 4 bytes) + 10 bytes type
        val headerBytes = if (payloadBytes < 256) 13 else 16
        return payloadBytes + headerBytes
    }

    fun isNfcSupported(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context) != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context)?.isEnabled == true
    }

    fun openNfcSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun createVCardNdefMessage(vCardString: String): NdefMessage {
        val bytes = vCardString.toByteArray(Charsets.UTF_8)
        val record = NdefRecord.createMime("text/vcard", bytes)
        return NdefMessage(arrayOf(record))
    }

    fun writeToNdefRecord(vCardString: String): NfcSendResult {
        val bytes = vCardString.toByteArray(Charsets.UTF_8)
        NdefRecord.createMime("text/vcard", bytes)
        return NfcSendResult.Success
    }

    fun writeNdefToTag(tag: Tag, message: NdefMessage): NfcSendResult {
        val messageBytes = message.byteArrayLength
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable) {
                    NfcSendResult.TagReadOnly
                } else if (ndef.maxSize < messageBytes) {
                    NfcSendResult.InsufficientCapacity(
                        requiredBytes = messageBytes,
                        maxBytes = ndef.maxSize
                    )
                } else {
                    ndef.writeNdefMessage(message)
                    NfcSendResult.Success
                }
            } catch (e: TagLostException) {
                NfcSendResult.TagLost
            } catch (e: FormatException) {
                NfcSendResult.Error("Tag format error: ${e.localizedMessage ?: "Invalid format"}")
            } catch (e: IOException) {
                NfcSendResult.Error("I/O error communicating with tag: ${e.localizedMessage ?: "Connection error"}")
            } catch (e: Exception) {
                NfcSendResult.Error(e.localizedMessage ?: "Failed to write NFC tag")
            } finally {
                try {
                    ndef.close()
                } catch (_: Exception) {}
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return try {
                formatable.connect()
                formatable.format(message)
                NfcSendResult.Success
            } catch (e: TagLostException) {
                NfcSendResult.TagLost
            } catch (e: FormatException) {
                NfcSendResult.Error("Format error: ${e.localizedMessage ?: "Cannot format tag"}")
            } catch (e: IOException) {
                NfcSendResult.Error("I/O error formatting tag: ${e.localizedMessage ?: "Connection error"}")
            } catch (e: Exception) {
                NfcSendResult.Error(e.localizedMessage ?: "Failed to format tag")
            } finally {
                try {
                    formatable.close()
                } catch (_: Exception) {}
            }
        }

        return NfcSendResult.Error("Tag does not support NDEF formatting.")
    }

    fun startSending(
        activity: Activity,
        vCardString: String,
        onResult: (NfcSendResult) -> Unit
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: run {
            onResult(NfcSendResult.Error("NFC is not supported on this device"))
            return
        }

        if (!adapter.isEnabled) {
            onResult(NfcSendResult.Error("NFC is disabled"))
            return
        }

        val message = createVCardNdefMessage(vCardString)
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        adapter.enableReaderMode(activity, { tag ->
            val result = writeNdefToTag(tag, message)
            activity.runOnUiThread {
                onResult(result)
            }
        }, flags, null)
    }

    fun stopSending(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (_: Exception) {}
    }
}
