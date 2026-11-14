# PRD v1.0 — QRISHook

## 1. Ringkasan
QRISHook adalah aplikasi Android ringan untuk otomatisasi berbasis **Automation**:
ketika sebuah pemicu terjadi, kirim HTTP request ke URL webhook.

Dua pemicu:
1. **Notifikasi** — untuk transaksi QRIS masuk (event-driven)
2. **Interval** — heartbeat untuk monitoring HP masih online (periodik)

## 2. Model Data

```kotlin
Automation {
  id, name, enabled, createdAt
  trigger: Trigger
  action: Action
  lastRunAt, lastStatus
}

sealed class Trigger {
  data class NotificationTrigger(
    sourceApps: List<String>,   // packageName, kosong = semua app
    keywords: List<String>      // OR, case-insensitive
  ) : Trigger()
  
  data class IntervalTrigger(
    intervalMinutes: Int        // min 5
  ) : Trigger()
}

Action {
  url: String
  method: "POST" | "GET"
  headers: List<KeyValue>
  bodyTemplate: String
}

ExecutionLog {
  id, automationId, automationName, triggerType
  timestamp, status (SUCCESS/FAILED/QUEUED)
  httpCode, responseSnippet, errorMessage
  payloadSent
}
```

## 3. Fitur MVP (P0)

1. Onboarding permission (Notification Access, POST_NOTIFICATIONS, battery exclusion)
2. Daftar Automation (list, toggle, edit, hapus)
3. Wizard Buat Automation (5 step):
   - Step 1: Nama
   - Step 2: Pilih Pemicu (Notifikasi / Interval)
   - Step 3: Konfigurasi Pemicu
   - Step 4: Konfigurasi Webhook
   - Step 5: Test + Simpan
4. Trigger Notifikasi: multi-select app + filter keyword (OR)
5. Trigger Interval: input menit (min 5)
6. Action Webhook: URL, method, headers, body template
7. Placeholder: `{{title}}`, `{{text}}`, `{{app}}`, `{{timestamp}}`, `{{device}}`, `{{battery}}`, `{{notif_id}}`
8. Tombol Test (kirim dummy payload, tampilkan response)
9. Log eksekusi (list, filter status, lihat detail payload & response)
10. Foreground Service agar listener & worker hidup
11. Retry otomatis (3x exponential backoff)
12. Offline queue via Room

## 4. Payload Default

**Transaksi (trigger notifikasi):**
```json
{
  "app": "{{app}}",
  "text": "{{text}}",
  "notif_id": "{{notif_id}}"
}
```

**Heartbeat (trigger interval):**
```json
{
  "is_heartbeat": true,
  "battery": {{battery}},
  "device_name": "HP Android"
}
```

## 5. User Flow

```
Home (list automation + [+ Buat])
 └─ Wizard → Simpan & aktifkan
Service aktif di background
 ├─ Notif masuk → filter app+keyword → lolos? → kirim webhook (event=transaction)
 └─ Timer interval → kirim webhook (event=heartbeat)
Setiap eksekusi → log
```

## 6. Non-Functional Requirements

- APK < 10 MB, idle drain < 1%/jam
- URL & headers disimpan di EncryptedSharedPreferences
- 100% lokal, tanpa cloud/telemetry
- Android 8 (API 26) s/d 14 (API 34)
- Offline queue via Room, kirim saat online
- Dark mode + light mode

## 7. Tech Stack

| Layer | Pilihan |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Notif listener | `NotificationListenerService` |
| Scheduler | `WorkManager` (interval) |
| Background | Foreground Service |
| HTTP | OkHttp |
| Local DB | Room + KSP |
| Secure storage | `EncryptedSharedPreferences` |
| DI | Hilt |
| Navigation | Navigation Compose |

## 8. Out of Scope MVP
- Trigger lain (wifi, lokasi, jam, baterai, dsb.)
- Action lain (buka app, SMS, TTS, dsb.)
- AND filter, regex custom
- Multiple action per automation
- Cloud sync, akun
- Parsing nominal otomatis
- iOS

## 9. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| OEM kill background (Xiaomi/Oppo/Vivo) | Onboarding battery optimization + autostart guide |
| Notif format bank berubah | Selalu kirim `{{text}}` mentah |
| Interval < 5 menit boros baterai | Min 5 menit, warning UI |
| Secret webhook bocor | EncryptedSharedPreferences + mask di UI |
| Nominal gagal parse | Field `amount` opsional, kosong kalau gagal |

## 10. Roadmap

- **Fase 1 (MVP, ~3 minggu):** semua P0 di atas
- **Fase 2:** retry backoff hardening, offline queue polish, preset bank (BCA, Mandiri, GoPay, OVO, DANA, ShopeePay), export/import automation JSON
- **Fase 3:** AND filter, regex custom, multiple action, condition branching

## 11. Metrik Sukses

- User berhasil setup automation pertama < 2 menit
- Success rate pengiriman webhook ≥ 95%
- Waktu dari notif diterima → webhook terkirim < 3 detik
