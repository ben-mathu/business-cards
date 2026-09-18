package com.benatt.businesscards.ui.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benatt.businesscards.R
import com.benatt.businesscards.data.dto.VCardDto
import com.benatt.businesscards.data.parser.toVCardString
import com.benatt.businesscards.ui.nfc.utils.NfcSendResult
import com.benatt.businesscards.ui.nfc.utils.NfcSender

sealed class NfcSendState {
    data object Ready : NfcSendState()
    data object Sending : NfcSendState()
    data object Success : NfcSendState()
    data class Error(val message: String) : NfcSendState()
    data object Disabled : NfcSendState()
    data object NotSupported : NfcSendState()
}

@Composable
fun NfcSendDialog(
    card: VCardDto,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Field selection state
    var includeName by remember { mutableStateOf(true) }
    var includeTitle by remember { mutableStateOf(card.title != null || card.role != null) }
    var includeOrg by remember { mutableStateOf(card.organization != null) }
    var selectedPhones by remember { mutableStateOf(card.phones.indices.toSet()) }
    var selectedEmails by remember { mutableStateOf(card.emails.indices.toSet()) }
    var selectedAddresses by remember { mutableStateOf(card.addresses.indices.toSet()) }
    var selectedWebsites by remember { mutableStateOf(card.websites.indices.toSet()) }
    var selectedSocials by remember { mutableStateOf(card.socialProfiles.indices.toSet()) }
    var includeNote by remember { mutableStateOf(card.note?.isNotBlank() == true) }

    // Filter card based on selection
    val filteredCard = remember(
        card, includeName, includeTitle, includeOrg,
        selectedPhones, selectedEmails, selectedAddresses,
        selectedWebsites, selectedSocials, includeNote
    ) {
        card.copy(
            formattedName = if (includeName) card.formattedName else "",
            name = if (includeName) card.name else null,
            title = if (includeTitle) card.title else null,
            role = if (includeTitle) card.role else null,
            organization = if (includeOrg) card.organization else null,
            phones = card.phones.filterIndexed { index, _ -> index in selectedPhones },
            emails = card.emails.filterIndexed { index, _ -> index in selectedEmails },
            addresses = card.addresses.filterIndexed { index, _ -> index in selectedAddresses },
            websites = card.websites.filterIndexed { index, _ -> index in selectedWebsites },
            socialProfiles = card.socialProfiles.filterIndexed { index, _ -> index in selectedSocials },
            note = if (includeNote) card.note else null
        )
    }

    val vCardString = remember(filteredCard) { filteredCard.toVCardString() }
    val currentNdefBytes = remember(vCardString) { NfcSender.calculateNdefSize(vCardString) }
    val hasAnyField = includeName || includeTitle || includeOrg ||
            selectedPhones.isNotEmpty() || selectedEmails.isNotEmpty() ||
            selectedAddresses.isNotEmpty() || selectedWebsites.isNotEmpty() ||
            selectedSocials.isNotEmpty() || includeNote

    val isExceeded = currentNdefBytes > NfcSender.MAX_NFC_BYTES
    val isValid = hasAnyField && !isExceeded

    var sendState by remember {
        mutableStateOf<NfcSendState>(
            when {
                !NfcSender.isNfcSupported(context) -> NfcSendState.NotSupported
                !NfcSender.isNfcEnabled(context) -> NfcSendState.Disabled
                else -> NfcSendState.Ready
            }
        )
    }

    // Auto-fit function to trim optional fields until size <= 144 B
    fun autoFitToNfcSize() {
        // Start by unchecking large optional fields: note, address, social profiles
        includeNote = false
        selectedAddresses = emptySet()
        selectedSocials = emptySet()

        // Check if still exceeded; if so, keep only primary phone, email, name, org
        var testCard = card.copy(
            formattedName = card.formattedName,
            name = card.name,
            title = card.title,
            role = card.role,
            organization = card.organization,
            phones = card.phones.take(1),
            emails = card.emails.take(1),
            addresses = emptyList(),
            websites = emptyList(),
            socialProfiles = emptyList(),
            note = null
        )

        selectedPhones = if (card.phones.isNotEmpty()) setOf(0) else emptySet()
        selectedEmails = if (card.emails.isNotEmpty()) setOf(0) else emptySet()
        selectedWebsites = emptySet()

        if (NfcSender.calculateNdefSize(testCard.toVCardString()) > NfcSender.MAX_NFC_BYTES) {
            // Trim title and org if necessary
            includeTitle = false
            testCard = testCard.copy(title = null, role = null)
            if (NfcSender.calculateNdefSize(testCard.toVCardString()) > NfcSender.MAX_NFC_BYTES) {
                includeOrg = false
            }
        }
    }

    // Controls NFC Reader Mode lifecycle: only active when valid (<= 144 bytes)
    DisposableEffect(isValid, vCardString, activity, sendState) {
        if (isValid && sendState is NfcSendState.Ready && activity != null) {
            val result = NfcSender.writeToNdefRecord(vCardString)
                sendState = when (result) {
                    is NfcSendResult.Success -> NfcSendState.Success
                    is NfcSendResult.TagReadOnly -> NfcSendState.Error("This NFC tag is read-only / locked.")
                    is NfcSendResult.InsufficientCapacity -> NfcSendState.Error(
                        "Tag capacity (${result.maxBytes} B) is smaller than card (${result.requiredBytes} B)."
                    )

                    is NfcSendResult.TagLost -> NfcSendState.Error("Connection lost. Hold the tag still.")
                    is NfcSendResult.Error -> NfcSendState.Error(result.message)
                }
            // TODO: Only creates an adapter to listen for another device's nfc broadcast
//            NfcSender.startSending(activity, vCardString) { result ->

//            }
        } else if (!isValid && activity != null) {
            NfcSender.stopSending(activity)
        }

        onDispose {
            if (activity != null) {
                NfcSender.stopSending(activity)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Send via NFC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Size & Limit Indicator Card
                SizeValidationCard(
                    currentBytes = currentNdefBytes,
                    maxBytes = NfcSender.MAX_NFC_BYTES,
                    isExceeded = isExceeded,
                    hasNoFields = !hasAnyField,
                    onAutoFit = { autoFitToNfcSize() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Field Selection Section
                Text(
                    text = "Select Fields to Include",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable container for field checkboxes
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Full Name
                        FieldCheckboxItem(
                            label = "Full Name",
                            value = card.displayName,
                            checked = includeName,
                            onCheckedChange = { includeName = it }
                        )

                        // Title & Role
                        val titleText = listOfNotNull(card.title, card.role).joinToString(" • ")
                        if (titleText.isNotBlank()) {
                            FieldCheckboxItem(
                                label = "Title & Role",
                                value = titleText,
                                checked = includeTitle,
                                onCheckedChange = { includeTitle = it }
                            )
                        }

                        // Organization
                        card.organization?.let { org ->
                            FieldCheckboxItem(
                                label = "Organization",
                                value = listOfNotNull(org.name, org.department).joinToString(" — "),
                                checked = includeOrg,
                                onCheckedChange = { includeOrg = it }
                            )
                        }

                        // Phone Numbers
                        card.phones.forEachIndexed { index, phone ->
                            FieldCheckboxItem(
                                label = "Phone (${phone.type.name})",
                                value = phone.number,
                                checked = index in selectedPhones,
                                onCheckedChange = { checked ->
                                    selectedPhones = if (checked) {
                                        selectedPhones + index
                                    } else {
                                        selectedPhones - index
                                    }
                                }
                            )
                        }

                        // Emails
                        card.emails.forEachIndexed { index, email ->
                            FieldCheckboxItem(
                                label = "Email (${email.type.name})",
                                value = email.address,
                                checked = index in selectedEmails,
                                onCheckedChange = { checked ->
                                    selectedEmails = if (checked) {
                                        selectedEmails + index
                                    } else {
                                        selectedEmails - index
                                    }
                                }
                            )
                        }

                        // Websites
                        card.websites.forEachIndexed { index, web ->
                            FieldCheckboxItem(
                                label = "Website",
                                value = web.url,
                                checked = index in selectedWebsites,
                                onCheckedChange = { checked ->
                                    selectedWebsites = if (checked) {
                                        selectedWebsites + index
                                    } else {
                                        selectedWebsites - index
                                    }
                                }
                            )
                        }

                        // Addresses
                        card.addresses.forEachIndexed { index, addr ->
                            val formatted = addr.formattedAddress()
                            if (formatted.isNotBlank()) {
                                FieldCheckboxItem(
                                    label = "Address (${addr.type.name})",
                                    value = formatted,
                                    checked = index in selectedAddresses,
                                    onCheckedChange = { checked ->
                                        selectedAddresses = if (checked) {
                                            selectedAddresses + index
                                        } else {
                                            selectedAddresses - index
                                        }
                                    }
                                )
                            }
                        }

                        // Social Profiles
                        card.socialProfiles.forEachIndexed { index, social ->
                            FieldCheckboxItem(
                                label = social.platform.displayName,
                                value = "@${social.username.removePrefix("@")}",
                                checked = index in selectedSocials,
                                onCheckedChange = { checked ->
                                    selectedSocials = if (checked) {
                                        selectedSocials + index
                                    } else {
                                        selectedSocials - index
                                    }
                                }
                            )
                        }

                        // Note / Bio
                        card.note?.takeIf { it.isNotBlank() }?.let { note ->
                            FieldCheckboxItem(
                                label = "Note",
                                value = note,
                                checked = includeNote,
                                onCheckedChange = { includeNote = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // NFC Status / Visual
                if (!isValid) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isExceeded) {
                                    "NFC writing paused: reduce payload size to 144 bytes or less."
                                } else {
                                    "NFC writing paused: select at least one field."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    NfcStatusVisual(state = sendState)
                    Spacer(modifier = Modifier.height(8.dp))

                    val stateDescription = when (val state = sendState) {
                        is NfcSendState.Ready -> "Hold an NFC tag to the back of your phone to send this contact card."
                        is NfcSendState.Sending -> "Writing contact card to NFC tag..."
                        is NfcSendState.Success -> "Contact card successfully sent to NFC tag!"
                        is NfcSendState.Error -> state.message
                        is NfcSendState.Disabled -> "NFC is turned off. Please enable NFC in system settings."
                        is NfcSendState.NotSupported -> "NFC is not supported on this device."
                    }

                    Text(
                        text = stateDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sendState is NfcSendState.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            when (sendState) {
                is NfcSendState.Success -> {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }

                is NfcSendState.Error -> {
                    Button(onClick = {
                        sendState = if (!NfcSender.isNfcEnabled(context)) {
                            NfcSendState.Disabled
                        } else {
                            NfcSendState.Ready
                        }
                    }) {
                        Text("Try Again")
                    }
                }

                is NfcSendState.Disabled -> {
                    Button(onClick = {
                        NfcSender.openNfcSettings(context)
                    }) {
                        Text("Open Settings")
                    }
                }

                else -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        },
        dismissButton = {
            if (sendState is NfcSendState.Success || sendState is NfcSendState.Error || sendState is NfcSendState.Disabled) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun SizeValidationCard(
    currentBytes: Int,
    maxBytes: Int,
    isExceeded: Boolean,
    hasNoFields: Boolean,
    onAutoFit: () -> Unit
) {
    val progress = (currentBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
    val statusColor = when {
        isExceeded || hasNoFields -> MaterialTheme.colorScheme.error
        else -> Color(0xFF2E7D32)
    }

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (isExceeded || hasNoFields) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isExceeded || hasNoFields) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NDEF Payload Size",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "$currentBytes / $maxBytes bytes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar showing capacity consumption
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Validation message & Quick Auto-Fit Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExceeded || hasNoFields) "⚠" else "✓",
                        fontSize = 14.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            hasNoFields -> "No fields selected."
                            isExceeded -> "Exceeds 144 B by ${currentBytes - maxBytes} B."
                            else -> "${maxBytes - currentBytes} B free."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }

                if (isExceeded) {
                    FilterChip(
                        selected = false,
                        onClick = onAutoFit,
                        label = {
                            Text(
                                text = "Fit to 144 B",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldCheckboxItem(
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NfcStatusVisual(state: NfcSendState) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(80.dp)
    ) {
        if (state is NfcSendState.Ready) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
            )
        }

        val containerColor = when (state) {
            is NfcSendState.Success -> MaterialTheme.colorScheme.primaryContainer
            is NfcSendState.Error -> MaterialTheme.colorScheme.errorContainer
            is NfcSendState.Disabled, is NfcSendState.NotSupported -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.primaryContainer
        }

        val iconTint = when (state) {
            is NfcSendState.Success -> MaterialTheme.colorScheme.primary
            is NfcSendState.Error -> MaterialTheme.colorScheme.error
            is NfcSendState.Disabled, is NfcSendState.NotSupported -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is NfcSendState.Sending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = iconTint
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nfc),
                        contentDescription = "NFC Contactless",
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}