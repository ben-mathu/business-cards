# Business Cards

An open-source Android application designed to bring back contact card sharing and business card management without privacy hurdles, subscription paywalls, or third-party cloud lock-in.

---

## Architecture & Key Components

The app uses a direct-to-database architecture consuming Room ORM with zero network overhead.

### 1. Data Transfer Objects (DTO)
Located in `com.benatt.businesscards.data.dto`:
- **`VCardDto`**: Comprehensive model implementing `java.io.Serializable`, fully compliant with RFC 2426 (vCard 3.0) and RFC 6350 (vCard 4.0).
  - Identification: `uid`, `formattedName` (`FN`), `name` (`N`), `nickname`.
  - Organization: `organization` (`ORG`), `title`, `role`.
  - Contacts: `phones` (`TEL`), `emails` (`EMAIL`), `addresses` (`ADR`), `websites` (`URL`), `socialProfiles` (`X-SOCIALPROFILE`).
  - Media & Location: `photoUri`, `logoUri`, `geo` (coordinates), `timezone`.
  - Metadata: `birthday`, `anniversary`, `note`, `categories`, `revision`, `customFields`.
- **Sub-DTOs**: `VCardNameDto`, `OrganizationDto`, `PhoneDto`, `EmailDto`, `AddressDto`, `WebDto`, `SocialProfileDto`, `GeoLocationDto`.
- **Enums**: `PhoneType`, `EmailType`, `AddressType`, `WebType`, `SocialPlatform`.

### 2. VCard Parser & Serializer
Located in `com.benatt.businesscards.data.parser`:
- **`VCardParser`**:
  - `parse(inputStream: InputStream): List<VCardDto>`
  - `parse(file: File): List<VCardDto>`
  - `parse(vCardText: String): List<VCardDto>`
  - `parseSingle(vCardText: String): VCardDto?`
  - Features: Handles multi-card `.vcf` streams, RFC multiline unfolding (lines indented with space/tab), escape character decoding (`\,`, `\;`, `\\`, `\n`), and property parameter parsing (`TYPE=CELL,PREF`, `TYPE=WORK`).
- **`VCardSerializer`**:
  - `serialize(card: VCardDto): String`
  - `serializeAll(cards: List<VCardDto>): String`
  - Converts cards back into standard RFC vCard text for export or sharing.

### 3. Room ORM Persistence Layer
Located in `com.benatt.businesscards.data.local`:
- **`VCardEntity`** (`entity/VCardEntity.kt`):
  - Primary Key: `id: String` (UUID / UID).
  - Indexed columns for fast search: `formattedName`, `primaryPhone`, `primaryEmail`, `organization`.
  - Preserves the original raw `.vcf` text in `rawVcf` for lossless export.
  - Conversion helpers: `entity.toDto()` and `VCardEntity.fromDto(dto, rawVcf)`.
- **`VCardTypeConverters`** (`converter/VCardTypeConverters.kt`):
  - High-performance, zero-dependency serialization for lists (phones, emails, addresses, social profiles, categories) using ASCII Record and Unit delimiters (`\u001E` and `\u001F`).
- **`VCardDao`** (`dao/VCardDao.kt`):
  - CRUD: `insertCard()`, `insertCards()`, `updateCard()`, `deleteCard()`, `deleteCardById()`.
  - Reactive Queries: `getAllCards(): Flow<List<VCardEntity>>`, `searchCards(query): Flow<List<VCardEntity>>`.
- **`AppDatabase`** (`AppDatabase.kt`):
  - Singleton Room database instance configured for SQLite.

### 4. Direct DAO Importer
Located in `com.benatt.businesscards.data.local`:
- **`VCardImporter`**:
  - `dao.importAndSaveFromIntent(context, intent)`: Automatically inspects incoming Android `Intent` (from share sheet or file viewer) and saves all cards to Room.
  - `dao.importAndSaveVcf(context, uri)`: Reads directly from a Content or File URI.
  - `dao.importAndSaveVcf(inputStream)`: Reads directly from an input stream.
  - `dao.importAndSaveVcf(vcfContent)`: Reads directly from a raw vCard text string.

---

## Usage Examples

### 1. Handling Shared .vcf in `MainActivity`
```kotlin
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.benatt.businesscards.data.local.AppDatabase
import com.benatt.businesscards.data.local.importAndSaveFromIntent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vCardDao by lazy {
        AppDatabase.getInstance(applicationContext).vCardDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingVcf(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingVcf(intent)
    }

    private fun handleIncomingVcf(intent: Intent?) {
        lifecycleScope.launch {
            val importedCards = vCardDao.importAndSaveFromIntent(applicationContext, intent)
            if (importedCards.isNotEmpty()) {
                // Cards are now saved in Room and ready to display
            }
        }
    }
}
```

### 2. Reading from File Picker / URI
```kotlin
val vCardDao = AppDatabase.getInstance(context).vCardDao()

lifecycleScope.launch {
    val importedCards = vCardDao.importAndSaveVcf(context, selectedUri)
}
```

### 3. Observing Saved Cards in Jetpack Compose / ViewModel
```kotlin
val vCardDao = AppDatabase.getInstance(context).vCardDao()

// Observe all cards sorted by name
val cardsFlow = vCardDao.getAllCards()

// Live contact search
val searchFlow = vCardDao.searchCards(searchQuery)
```
