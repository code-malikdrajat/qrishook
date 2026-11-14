# UI/UX Spec — QRISHook

**Design language:** Material Design 3 (M3) — Compose
**Target:** Android 8.0+

## 1. Design Tokens

### Spacing
`xs=4dp · sm=8dp · md=16dp · lg=24dp · xl=32dp`

### Typography (M3 defaults)
| Style | Penggunaan |
|---|---|
| `headlineSmall` | Judul wizard step |
| `titleLarge` | AppBar title |
| `titleMedium` | Nama automation di card |
| `bodyLarge` | Isi form |
| `bodyMedium` | Body list, subtitle |
| `labelSmall` | Caption, timestamp |
| `labelLarge` | Tombol |

### Color (semantic, dari MaterialTheme.colorScheme)
| Token | Penggunaan |
|---|---|
| `primary` | FAB, tombol utama, toggle aktif |
| `secondaryContainer` | Chip "Notifikasi" / "Interval" |
| `surface`, `surfaceVariant` | Card background |
| `error` | Status FAILED, validasi |
| `tertiary` | Status SUCCESS / heartbeat |

### Corner Radius
`Card=12dp · Button=8dp · TextField=8dp · FAB=16dp · Chip=8dp`

### Iconography
- Notif trigger: `Icons.Outlined.Notifications`
- Interval trigger: `Icons.Outlined.Schedule`
- Add: `Icons.Default.Add`
- Edit/Delete/Send/Success/Failed/Queued/Empty — pakai Material default

## 2. User Flow

```
Splash → Onboarding (cek 3 izin)
       → Home (list/empty)
       → [+FAB] → Wizard 5 step → Simpan → Home
       → [tap item] → Automation Detail → Edit → Wizard
       → [log icon] → Log List → Log Detail

Bottom Nav: TIDAK ADA. Pakai AppBar actions + FAB.
```

## 3. Screen Inventory

| # | Screen | Route | Kompleksitas |
|---|---|---|---|
| 1 | Onboarding | `onboarding` | S |
| 2 | Home | `home` | M |
| 3 | Wizard Create/Edit | `wizard/{id?}` | L (5 sub-step) |
| 4 | Automation Detail | `automation/{id}` | S |
| 5 | Log List | `logs?automationId` | M |
| 6 | Log Detail | `logs/{id}` | S |
| 7 | Settings | `settings` | S |

## 4. Global Reusable Components (di `/ui/components/`)

Wajib dibuat sebelum dipakai di screen:

1. `QRISHookTopAppBar(title, onBack?, actions)` — M3 TopAppBar
2. `QRISHookButton(text, onClick, loading?, enabled?, modifier)` — Button height 48dp, spinner saat loading
3. `QRISHookTextField(value, onValueChange, label, errorText?, helperText?, keyboardType?, maxLines?)` — OutlinedTextField wrapper
4. `QRISHookCard(content)` — Card elevated radius 12dp
5. `StatusChip(status)` — SUCCESS/FAILED/QUEUED (AssistChip)
   - SUCCESS → tertiary container + check_circle
   - FAILED → error container + error
   - QUEUED → surfaceVariant + schedule
6. `EmptyState(icon, title, description, actionLabel?, onAction?)` — icon 96dp center
7. `LoadingState()` — full-screen CircularProgressIndicator
8. `ErrorBanner(message, actionLabel?, onAction?)` — Card error color
9. `AutomationCard(automation, onToggle, onClick)` — Row(icon+text+Switch), alpha 0.6 kalau disabled
10. `LogItem(log, onClick)` — status icon + title + subtitle + timestamp
11. `TriggerOptionCard(type, selected, onClick)` — Card selectable border primary 2dp saat selected
12. `PermissionCard(icon, label, granted, onRequest)` — Row + button "Buka" / check
13. `HeaderRow(key, value, onKeyChange, onValueChange, onDelete)` — Row 2 TextField + delete icon
14. `PlaceholderChip(text, onClick)` — AssistChip, insert ke cursor
15. `SectionHeader(text)` — titleMedium uppercase
16. `KeyValueRow(label, value)` — Row untuk detail

## 5. Screen 1 — Onboarding

**Layout:**
- TopAppBar "QRISHook" (no back)
- Icon bell 96dp (primary tint)
- headlineSmall "Izinkan QRISHook bekerja"
- bodyMedium "Kami butuh 3 izin..."
- 3x PermissionCard:
  - Akses Notifikasi → buka Settings intent
  - Izin Notifikasi → POST_NOTIFICATIONS
  - Bebas Baterai → REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- QRISHookButton "Mulai Sekarang" (enabled hanya jika 3 granted)
- TextButton "Lewati untuk sekarang"

**State:**
| State | Behavior |
|---|---|
| Belum granted | Tombol "Buka" → Settings intent |
| Granted | Trailing icon ✓ hijau, Card secondaryContainer |
| Semua granted | "Mulai Sekarang" aktif → navigate home |
| "Lewati" | Navigate home, ErrorBanner muncul di home |
| Kembali dari Settings | `LifecycleEventObserver` ON_RESUME → re-check |

## 6. Screen 2 — Home

**Layout:**
- TopAppBar: "QRISHook" + actions [log icon, settings icon]
- ErrorBanner (conditional, kalau izin kurang)
- LazyColumn AutomationCard
- FAB bottom-end (icon +)

**AutomationCard:**
```
Row:
  Icon circle (40dp) │ Column(name, subtitle, "Terakhir: {rel} ✓") │ Switch
```
Subtitle:
- Notif: `"Notifikasi · {apps.joinToString(", ")}"` (kosong → "Semua aplikasi")
- Interval: `"Interval · {menit} menit"`

**State Matrix:**
| State | Tampilan |
|---|---|
| Loading | LoadingState |
| Empty | EmptyState inbox + "Buat Automation" |
| List | LazyColumn cards |
| Izin kurang | ErrorBanner di atas list |

**Interaksi:**
| Action | Behavior |
|---|---|
| Tap Card | → automation/{id} |
| Toggle Switch | Update Room, snackbar, kalau izin kurang → snackbar warning |
| Swipe left | Delete background + konfirmasi AlertDialog |
| Tap FAB | → wizard |
| Tap Log icon | → logs |
| Tap Settings | → settings |

## 7. Screen 3 — Wizard (5 Step)

**Struktur:**
- TopAppBar: back (mundur step), title "Buat Automation", action "Batal"
- LinearProgressIndicator di bawah AppBar: `(step+1)/5f`
- Label: "Step {n} dari 5 · {Nama Step}"
- Content scrollable per step
- Bottom bar fixed: [Kembali] [Lanjut/Simpan]

### Step 3.1 — Nama
- headlineSmall "Apa nama automation ini?"
- OutlinedTextField "Nama automation" (max 50 char, counter)
- Validasi: non-empty → enable "Lanjut"

### Step 3.2 — Pilih Pemicu
- 2x TriggerOptionCard:
  - Notifikasi (icon bell): "Kirim saat notifikasi masuk (mis. transaksi QRIS)"
  - Interval (icon schedule): "Kirim berkala sebagai heartbeat"
- Radio behavior, single choice

### Step 3.3a — Konfigurasi Notifikasi
**A. Aplikasi sumber:**
- FilterChip terpilih (removable X)
- LazyColumn ListItem dengan Checkbox — `PackageManager.getInstalledApplications()` filter LAUNCHER
- Helper text "Kosongkan untuk semua aplikasi"

**B. Filter kata kunci:**
- Row: OutlinedTextField + IconButton Add
- InputChip removable di bawah
- Validasi: no duplikat (case-insensitive) → snackbar
- Helper "Kosongkan untuk menerima semua notifikasi"

**Rules:** Boleh lanjut dengan 0 app (semua) atau 0 keyword (semua notif)

### Step 3.3b — Konfigurasi Interval
- OutlinedTextField numeric
- FilterChip quick pick: 5m, 15m, 30m, 1j, 6j, 24j
- Validasi: `parsed >= 5` → errorText "Minimal 5 menit"
- Helper "Interval lebih pendek = lebih boros baterai"

### Step 3.4 — Konfigurasi Webhook
**A. URL:** OutlinedTextField, keyboard URI, wajib valid `http(s)://`
**B. Method:** Row RadioButton POST/GET, default POST
**C. Headers:**
- HeaderRow: key TextField + value TextField + delete IconButton
- TextButton "+ Tambah header"
- Default 1 row: `Content-Type: application/json`
**D. Body template:**
- OutlinedTextField multiline, minLines 6, maxLines 12
- FontFamily.Monospace
- Disabled kalau GET
- Pre-fill sesuai trigger type
- Placeholder chips di bawah: tap → insert di cursor (TextFieldValue tracking)

### Step 3.5 — Test & Simpan
- SummaryCard: Nama/Pemicu/Keyword/Webhook/Header
- "Test Sekarang" OutlinedButton (loading spinner saat kirim)
- Dummy data:
  - Notif: title="QRIS Diterima", text="Anda menerima Rp 50.000 dari Budi", app=first selected
  - Interval: device=Build.MODEL, timestamp=now
- ResultCard 3 state:
  - Idle: "Belum ada test"
  - Loading: LinearProgressIndicator
  - Success: tertiaryContainer "✓ Berhasil (200) · 342ms" + response monospace
  - Failed: errorContainer "✗ Gagal (500)" + response
  - Network error: errorContainer
- Payload preview: read-only monospace, SelectionContainer
- Tombol: "Simpan & Aktifkan" — kalau belum test → AlertDialog konfirmasi

## 8. Screen 4 — Automation Detail

**Layout:**
- TopAppBar: back + nama + [edit icon, delete icon]
- InfoCard Status: Row label + Switch
- InfoCard Pemicu: icon + label, divider, key-value (apps, keywords)
- InfoCard Tindakan: method badge + URL truncated (2 lines)
- InfoCard Statistik: terakhir jalan, total, success/failed
- TextButton "Lihat Log Automation ini"

**Delete dialog:** "Hapus automation {nama}? Log terkait tetap disimpan."

## 9. Screen 5 — Log List

**Layout:**
- TopAppBar: back + "Riwayat Eksekusi"
- Filter chips row (scrollable): Semua/Sukses/Gagal/Antrian
- LazyColumn LogItem:
  - Row: status icon + Column(nama automation, "TriggerType · Status {code}") + timestamp
  - Icon: SUCCESS tertiary / FAILED error / QUEUED secondary

**State:**
| State | Tampilan |
|---|---|
| Loading | LoadingState |
| Empty (no filter) | EmptyState receipt_long "Belum ada riwayat" |
| Empty (filtered) | "Tidak ada log {filter}" |
| List | LazyColumn |

**Query param:** `?automationId={id}` → title "Log: {nama}" + chip "Semua"

## 10. Screen 6 — Log Detail

**Layout:**
- TopAppBar back + "Detail Eksekusi"
- Header card status color (tertiary/error/surfaceVariant):
  - Icon + "Sukses"/"Gagal"/"Antrian"
  - Nama automation
  - Timestamp
- Info section: Pemicu / HTTP / Durasi
- Payload section: monospace Card + TextButton "Copy payload"
- Response section: monospace Card + TextButton "Copy response"
- OutlinedButton "Kirim Ulang" (retry)

## 11. Screen 7 — Settings

**Sections:**
- **Izin:** 3 ListItem (Akses Notif, Izin Notif, Bebas Baterai) — trailing ✓ atau "Buka >"
- **Troubleshooting:**
  - "Cek service berjalan" (status indicator)
  - "Panduan OEM (Xiaomi, Oppo, dst)" > navigate ke webview
- **Tentang:**
  - Versi
  - "Buka source code" > browser
- **Disclaimer** Card surfaceVariant:
  "Aplikasi ini tidak berafiliasi dengan bank atau penyedia e-wallet manapun. Penggunaan sepenuhnya tanggung jawab pengguna."

## 12. Navigation Map

```kotlin
NavGraph:
- onboarding     (no back)
- home           (start after onboarding)
- wizard?automationId  (null=create, ada=edit)
- automation/{id}
- logs?automationId
- log/{id}
- settings
```

**Back behavior:**
- Home → exit app
- Wizard step > 1 → mundur step
- Wizard step 1 → konfirmasi kalau data terisi
- Other → pop

## 13. State Matrix Master

| Screen | Loading | Empty | Error | Success |
|---|---|---|---|---|
| Onboarding | - | - | Snackbar permission deny | Tombol aktif |
| Home | LoadingState | EmptyState inbox | ErrorBanner izin | Cards |
| Home toggle | - | - | Snackbar fail | Snackbar aktif/nonaktif |
| Wizard | Loading fetch apps | - | ErrorText, snackbar | Snackbar tersimpan |
| Wizard Test | Spinner tombol + ResultCard loading | - | ResultCard failed | ResultCard success |
| Automation Detail | LoadingState | - | ErrorBanner not found | Content |
| Log List | LoadingState | EmptyState per filter | ErrorBanner + retry | Items |
| Log Detail | LoadingState | - | ErrorBanner | Content |
| Settings | - | - | - | Static |

## 14. Accessibility Checklist
- [ ] Semua IconButton punya contentDescription
- [ ] Switch pakai contentDescription "Aktifkan automation {nama}"
- [ ] Min touch target 48dp
- [ ] Contrast ratio ≥ 4.5:1
- [ ] MaterialTheme.typography (no hardcode fontSize)
- [ ] Form field pakai label (bukan hanya placeholder)
- [ ] Snackbar & error text screen-reader friendly

## 15. Prioritas Sprint UI (align dengan Task OpenCode)

| Sprint | Screens | Component baru |
|---|---|---|
| 1 | Setup tokens + 6 komponen dasar | TopAppBar, Button, TextField, Card, EmptyState, LoadingState |
| 2 | Onboarding + Home | PermissionCard, AutomationCard |
| 3 | Wizard step 1-2 | TriggerOptionCard, SectionHeader |
| 4 | Wizard step 3 | FilterChip list, Checkbox |
| 5 | Wizard step 4-5 | HeaderRow, PlaceholderChip, ResultCard |
| 6 | Log list + detail | LogItem, StatusChip |
| 7 | Settings + polish | ListItem, ErrorBanner |
