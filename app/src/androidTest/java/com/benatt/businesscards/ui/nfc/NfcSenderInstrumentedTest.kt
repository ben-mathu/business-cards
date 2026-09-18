package com.benatt.businesscards.ui.nfc

import android.nfc.NdefRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benatt.businesscards.ui.nfc.utils.NfcSender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NfcSenderInstrumentedTest {

    @Test
    fun createVCardNdefMessage_createsValidMimeNdefRecord() {
        val sampleVCard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Jane Doe
            TEL;TYPE=CELL:+1-555-123-4567
            EMAIL;TYPE=WORK:jane.doe@example.com
            END:VCARD
        """.trimIndent()

        val ndefMessage = NfcSender.createVCardNdefMessage(sampleVCard)
        assertNotNull(ndefMessage)

        val records = ndefMessage.records
        assertEquals(1, records.size)

        val record = records[0]
        assertEquals(NdefRecord.TNF_MIME_MEDIA, record.tnf)

        val mimeType = String(record.type, Charsets.US_ASCII)
        assertEquals("text/vcard", mimeType)

        val payloadText = String(record.payload, Charsets.UTF_8)
        assertEquals(sampleVCard, payloadText)
    }
}
