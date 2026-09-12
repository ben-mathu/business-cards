package com.benatt.businesscards.ui.cards

import android.R.attr.text
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.benatt.businesscards.data.dto.EmailDto
import com.benatt.businesscards.data.dto.OrganizationDto
import com.benatt.businesscards.data.dto.PhoneDto
import com.benatt.businesscards.data.dto.PhoneType
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.dto.VCardNameDto
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.set
import com.benatt.businesscards.data.parser.toVCardString

/**
 * Renders an individual contact card.
 * Adapts its layout seamlessly between Portrait and Landscape orientations:
 * - In Portrait: Centered elevated card with structured contact sections.
 * - In Landscape: Authentic horizontal business card layout with dual-column presentation.
 */
@Composable
fun ContactCardItem(
    card: VCardDto,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = if (isLandscape) {
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
            },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (isLandscape) {
                LandscapeCardContent(card = card, context = context)
            } else {
                PortraitCardContent(card = card, context = context)
            }
        }
    }
}

@Composable
private fun PortraitCardContent(
    card: VCardDto,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Initials Avatar
        QRCodeImage(
            vCardDetails = card.toVCardString(),
            size = 200
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Full Name
        Text(
            text = card.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Job Title & Role
        val titleText = listOfNotNull(card.title, card.role).joinToString(" • ")
        if (titleText.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        // Organization
        card.organization?.let { org ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = listOfNotNull(org.name, org.department).joinToString(" — "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Contact Information Sections
        ContactDetailsSection(card = card, context = context)
    }
}

@Composable
private fun LandscapeCardContent(
    card: VCardDto,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Identity & Branding
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            QRCodeImage(
                vCardDetails = card.toVCardString(),
                size = 150
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = card.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val titleText = listOfNotNull(card.title, card.role).joinToString(" • ")
            if (titleText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            card.organization?.let { org ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = org.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Right Column: Contact Details (Scrollable if content overflows)
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ContactDetailsSection(card = card, context = context)
        }
    }
}

@Composable
private fun ContactDetailsSection(
    card: VCardDto,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Phones
        for (phone in card.phones) {
            ContactInfoRow(
                label = phone.type.name,
                value = phone.number,
                badge = if (phone.isPrimary) "Primary" else null,
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.number}"))
                    context.startActivity(dialIntent)
                }
            )
        }

        // Emails
        for (email in card.emails) {
            ContactInfoRow(
                label = email.type.name,
                value = email.address,
                badge = if (email.isPrimary) "Primary" else null,
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email.address}"))
                    context.startActivity(emailIntent)
                }
            )
        }

        // Addresses
        for (addr in card.addresses) {
            val formatted = addr.formattedAddress()
            if (formatted.isNotBlank()) {
                ContactInfoRow(
                    label = addr.type.name,
                    value = formatted,
                    onClick = {
                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(formatted)}"))
                        context.startActivity(mapIntent)
                    }
                )
            }
        }

        // Websites
        for (web in card.websites) {
            ContactInfoRow(
                label = "Website",
                value = web.url,
                onClick = {
                    val url = if (web.url.startsWith("http")) web.url else "https://${web.url}"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(browserIntent)
                }
            )
        }

        // Social Profiles
        for (social in card.socialProfiles) {
            val fullUrl = social.url ?: social.platform.buildFullUrl(social.username)
            ContactInfoRow(
                label = social.platform.displayName,
                value = "@${social.username.removePrefix("@")}",
                onClick = {
                    if (fullUrl.startsWith("http")) {
                        val socialIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                        context.startActivity(socialIntent)
                    }
                }
            )
        }

        // Note / Bio
        card.note?.takeIf { it.isNotBlank() }?.let { note ->
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactInfoRow(
    label: String,
    value: String,
    badge: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (badge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun QRCodeImage(
    vCardDetails: String,
    size: Int
) {
    val qrBitmap = remember(vCardDetails, size) {
        generateQrCodeBitmap(vCardDetails, size)
    }

    val initials = vCardDetails.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code for $text",
                modifier = Modifier.size(size.dp)
            )
        }
//        ImageBitmap(size, size)
//        Text(
//            text = initials,
//            color = Color.White,
//            fontSize = (size * 0.4).sp,
//            fontWeight = FontWeight.Bold
//        )
    }
}

fun generateQrCodeBitmap(
    content: String,
    sizePx: Int = 512
): Bitmap? {
    if (content.isEmpty()) return null
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = createBitmap(sizePx, sizePx)

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(
    name = "Landscape Mode",
    device = "spec:parent=pixel_5,orientation=landscape",
    showBackground = true
)
@Composable
fun PreviewContactCardItemLandscape() {
    val dto = VCardDto(
        uid = "card-123",
        version = "3.0",
        formattedName = "Morgan Lee",
        name = VCardNameDto(givenName = "Morgan", familyName = "Lee"),
        organization = OrganizationDto(name = "OpenSource Labs", department = "Core"),
        title = "Architect",
        phones = listOf(PhoneDto(number = "+1999888777", type = PhoneType.CELL, isPrimary = true)),
        emails = listOf(EmailDto(address = "morgan@example.org", isPrimary = true)),
        note = "Frequent collaborator",
        categories = listOf("Colleague", "Tech")
    )
    ContactCardItem(dto)
}

@Preview()
@Composable
fun PreviewContactCardItem() {
    val dto = VCardDto(
        uid = "card-123",
        version = "3.0",
        formattedName = "Morgan Lee",
        name = VCardNameDto(givenName = "Morgan", familyName = "Lee"),
        organization = OrganizationDto(name = "OpenSource Labs", department = "Core"),
        title = "Architect",
        phones = listOf(PhoneDto(number = "+1999888777", type = PhoneType.CELL, isPrimary = true)),
        emails = listOf(EmailDto(address = "morgan@example.org", isPrimary = true)),
        note = "Frequent collaborator",
        categories = listOf("Colleague", "Tech")
    )
    ContactCardItem(dto)
}