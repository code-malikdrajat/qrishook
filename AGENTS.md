# AGENTS.md — QRISHook

## Project Overview
- **Name:** QRISHook
- **Package:** `com.qrishook.app`
- **Platform:** Android native (Kotlin)
- **Fungsi:** Automation — ketika pemicu (notifikasi/interval) terjadi, kirim HTTP request ke webhook
- **Entitas inti:** **Automation** (1 trigger + 1 action)

## 📚 Dokumen Referensi (WAJIB dibaca sebelum mulai)
- `docs/PRD.md` — Product Requirements (fitur, scope, model data, roadmap)
- `docs/UI_SPEC.md` — Detail setiap screen, komponen reusable, state matrix
- `docs/mockup.html` — Mockup visual (buka di browser untuk lihat target UI)
- `PROMPTS.md` — Task list & prompt untuk eksekusi OpenCode

## Arsitektur
Clean-ish architecture, single-module:
```
app/
 └─ src/main/java/com/qrishook/app/
     ├─ data/
     │   ├─ db/           # AppDatabase, AutomationDao, LogDao
     │   ├─ model/        # Automation, Trigger (sealed), Action, ExecutionLog
     │   ├─ prefs/        # EncryptedSharedPreferences wrapper
     │   └─ repository/   # AutomationRepository, LogRepository
     ├─ domain/
     │   ├─ webhook/      # WebhookClient (OkHttp), TemplateEngine
     │   ├─ trigger/      # TriggerEvaluator
     │   └─ execution/    # AutomationExecutor
     ├─ service/
     │   ├─ NotifListenerService.kt
     │   ├─ ForegroundService.kt
     │   └─ SyncWorker.kt
     ├─ ui/
     │   ├─ theme/        # Color, Type, Shape, Theme
     │   ├─ components/   # Reusable UI components (lihat UI_SPEC.md section 9)
     │   ├─ home/         # HomeScreen, HomeViewModel
     │   ├─ wizard/       # CreateAutomationScreen (5 step)
     │   ├─ log/          # LogScreen, LogDetailScreen
     │   ├─ automation/   # AutomationDetailScreen
     │   ├─ settings/     # SettingsScreen
     │   └─ onboarding/   # OnboardingScreen
     ├─ navigation/       # NavGraph, Routes
     ├─ di/               # Hilt modules
     ├─ QRISHookApp.kt    # @HiltAndroidApp
     └─ MainActivity.kt
```

## Konvensi Kode
- **Compose only** (no XML layout)
- **ViewModel + StateFlow + UiState sealed interface** per screen
- **Repository pattern** — tidak ada business logic di UI
- Semua HTTP via `WebhookClient` (satu entry point)
- Placeholder renderer via `TemplateEngine.render(template, context)`
- Coroutine + Flow untuk async, tidak pakai callback
- Istilah di kode: **"Automation"** (bukan "Macro", bukan "Hook" untuk entitas)

## Domain Rules (WAJIB)
- 1 Automation = 1 Trigger + 1 Action
- `Trigger` = sealed class: `NotificationTrigger | IntervalTrigger`
- Interval minimum **5 menit** (validate di UI & domain)
- Keyword filter = **OR**, case-insensitive
- Payload default mengikuti contoh MacroDroid (`is_heartbeat` / `notif_id`); placeholder bebas dipakai di body template
- URL/method invalid → **tolak simpan**, tidak boleh runtime error
- Webhook gagal (non-2xx / exception) → log FAILED, **tidak crash service**
- Tidak boleh log PII di luar payload yang user lihat
- Headers & URL disimpan di **EncryptedSharedPreferences**

## Permission Flow
- `POST_NOTIFICATIONS` (Android 13+)
- `BIND_NOTIFICATION_LISTENER_SERVICE` (via Settings intent)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (via Settings intent)
- Semua wajib granted sebelum automation bisa aktif

## UI/UX Rules (WAJIB dipatuhi)
- Semua screen mengikuti spec di `docs/UI_SPEC.md`
- Komponen reusable WAJIB diimplement di `/ui/components/` sebelum dipakai:
  - QRISHookTopAppBar, QRISHookButton, QRISHookTextField, QRISHookCard
  - StatusChip, EmptyState, LoadingState, ErrorBanner
  - AutomationCard, LogItem, TriggerOptionCard, PermissionCard
  - HeaderRow, PlaceholderChip, SectionHeader, KeyValueRow
- Design tokens WAJIB pakai `MaterialTheme` — jangan hardcode
- Icon WAJIB pakai `Icons.Default.*` atau `Icons.Outlined.*`
- Empty/loading/error state WAJIB ada per screen sesuai state matrix

## UI Copy (WAJIB konsisten)
- List: **"Automation Saya"**
- FAB: **"Buat Automation"**
- Wizard title: **"Buat Automation Baru"**
- Empty state: **"Belum ada automation. Yuk buat yang pertama."**
- Toggle: **"Aktif" / "Nonaktif"**
- Log: **"Riwayat Eksekusi"**

## Testing
- Unit test untuk `TriggerEvaluator`, `TemplateEngine`, `WebhookClient` (MockWebServer)
- Integration test untuk `AutomationExecutor` (fake repository)
- UI test minimal untuk wizard flow (compose test)

## Out of Scope (jangan diimplement)
- Multiple action per automation
- Condition branching (IF/ELSE)
- Trigger wifi/lokasi/jam/baterai
- Backend/server/cloud sync
- Parsing nominal otomatis
- iOS

## Commit Convention
`feat|fix|refactor|test|chore(scope): message`
Contoh:
- `feat(wizard): add keyword filter step`
- `feat(home): add automation toggle`
- `ci: add GitHub Actions workflow for debug APK build`
