# PROMPTS — Task List untuk OpenCode

Eksekusi berurutan. Setiap task: **Mode Plan dulu → review → Mode Build**.

## Task #1 — Setup Project Skeleton

```
Eksekusi Task #1: Setup Android project QRISHook.

Baca dulu:
- AGENTS.md (utama)
- docs/PRD.md section 7 (Tech Stack)
- docs/UI_SPEC.md section 1 (Design Tokens)

Persyaratan:
1. Package: com.qrishook.app
2. App name: QRISHook
3. minSdk=26, targetSdk=34, compileSdk=34
4. Kotlin versi stabil terbaru (Compose compatible)
5. Dependencies WAJIB:
   - Jetpack Compose (BOM terbaru)
   - Material 3
   - Navigation Compose
   - Hilt (hilt-android, hilt-compiler, hilt-navigation-compose)
   - Room (room-runtime, room-ktx, room-compiler via KSP)
   - OkHttp
   - Kotlinx Serialization
   - kotlinx-coroutines-android
   - androidx.security:security-crypto
   - WorkManager
   - Lifecycle ViewModel Compose
6. Gunakan KSP (bukan kapt) untuk Room & Hilt compiler
7. Setup struktur folder sesuai AGENTS.md (data/domain/service/ui/di/navigation)
8. Buat Application class + @HiltAndroidApp
9. MainActivity dengan @AndroidEntryPoint + setContent { QRISHookTheme { ... } }
10. Theme M3: color scheme, typography, shape di /ui/theme/
    - Ikuti design tokens di UI_SPEC.md section 1
    - Support light + dark
11. Setup Navigation Compose dengan NavHost + route placeholder:
    onboarding, home, wizard, automation/{id}, logs, log/{id}, settings
    Isi sementara dengan Text "Screen X" untuk verifikasi navigasi
12. String resources (app_name="QRISHook")
13. AndroidManifest dengan permissions:
    - POST_NOTIFICATIONS
    - BIND_NOTIFICATION_LISTENER_SERVICE
    - REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    - INTERNET
    - ACCESS_NETWORK_STATE
14. Buat README.md singkat (cara build & run)

Setelah selesai:
- Jalankan ./gradlew assembleDebug untuk verifikasi build
- Jangan implement fitur apapun, hanya skeleton + theme + navigasi
- Commit: "feat: setup project skeleton"
```

## Task #2 — Data Layer (Room)

```
Task #2: Buat data layer Room.

Baca: docs/PRD.md section 2 (Model Data), AGENTS.md (arsitektur data/)

Buat:
1. Entities:
   - Automation (id auto, name, enabled, createdAt, trigger, action, lastRunAt, lastStatus)
   - Trigger (sealed class, disimpan via TypeConverter JSON)
   - Action (embedded dalam Automation)
   - ExecutionLog (id, automationId, automationName, triggerType, timestamp, status, httpCode, responseSnippet, errorMessage, payloadSent)
2. TypeConverters: Trigger <-> JSON string (pakai kotlinx.serialization)
3. DAO:
   - AutomationDao: getAll (Flow), getById, insert, update, delete, toggleEnabled
   - LogDao: getAll (Flow), getByAutomation, insert, deleteOlderThan
4. AppDatabase (Room)
5. Repository:
   - AutomationRepository (abstraksi DAO)
   - LogRepository
6. Hilt modules: DatabaseModule (provide AppDatabase, DAOs), RepositoryModule
7. Unit test minimal untuk insert+read

Commit: "feat(data): add Room entities, DAOs, and repositories"
```

## Task #3 — Secure Preferences

```
Task #3: EncryptedSharedPreferences wrapper.

Baca: AGENTS.md (Secure storage), docs/PRD.md (NFR)

Buat:
1. SecurePrefs interface:
   - saveWebhookSecret(automationId, url, headers)
   - getWebhookSecret(automationId): WebhookSecret?
   - deleteWebhookSecret(automationId)
2. Implementasi dengan EncryptedSharedPreferences (MasterKey AES256_GCM)
3. Hilt module provide SecurePrefs
4. Unit test (bisa pakai Robolectric atau instrumentation)

Commit: "feat(prefs): add encrypted storage for webhook secrets"
```

## Task #4 — Template Engine

```
Task #4: TemplateEngine untuk render placeholder.

Baca: docs/PRD.md section 4 (Payload Default)

Buat:
1. TemplateEngine.render(template: String, context: Map<String,String>): String
   - Replace {{key}} dengan context[key]
   - Kalau key tidak ada → biarkan as-is (jangan crash)
   - Support whitespace di dalam {{ }} → trim
2. Unit test:
   - Basic replace
   - Multiple placeholder
   - Missing key (return as-is)
   - Empty context
   - Placeholder di dalam JSON string (escape correctness)

Commit: "feat(webhook): add template engine for payload rendering"
```

## Task #5 — Webhook Client

```
Task #5: WebhookClient (OkHttp).

Baca: AGENTS.md (domain/webhook), docs/PRD.md (HTTP)

Buat:
1. WebhookClient interface:
   - suspend fun send(url, method, headers, body): WebhookResult
2. WebhookResult sealed class:
   - Success(httpCode, body, durationMs)
   - Failed(httpCode, body, error)
   - NetworkError(message)
3. Implementasi dengan OkHttp (timeout 15s connect/read)
   - POST: body dari template
   - GET: query dari body template (parse as query string)
4. Retry 3x exponential backoff (1s, 2s, 4s) — hanya untuk network error & 5xx
5. Unit test pakai MockWebServer:
   - 200 success
   - 500 failed
   - Timeout network error
   - Header dikirim benar
   - POST body benar

Commit: "feat(webhook): add OkHttp client with retry"
```

## Task #6 — Notification Listener

```
Task #6: NotificationListenerService.

Baca: AGENTS.md, docs/PRD.md section 2 (Trigger)

Buat:
1. NotifListenerService extends NotificationListenerService
2. onNotificationPosted(sbn: StatusBarNotification):
   - Ambil packageName, title, text, timestamp
   - Query AutomationRepository.getAll().filter { enabled && trigger is NotificationTrigger }
   - TriggerEvaluator.matches(notif, trigger):
     - App filter: sourceApps empty OR contains packageName
     - Keyword filter: keywords empty OR any keyword in (title+text).lowercase()
   - Kalau match → AutomationExecutor.execute(automation, notifContext)
3. TriggerEvaluator class + unit test
4. Daftarkan di AndroidManifest dengan intent-filter BIND_NOTIFICATION_LISTENER_SERVICE
5. Companion object dengan helper untuk cek permission granted

Commit: "feat(service): add notification listener with filter"
```

## Task #7 — Interval Worker

```
Task #7: IntervalWorker (WorkManager).

Baca: docs/PRD.md section 4 (heartbeat payload)

Buat:
1. IntervalWorker extends CoroutineWorker
2. doWork():
   - Query automation yang trigger is IntervalTrigger & enabled
   - Untuk setiap: build context {device: Build.MODEL, timestamp: ISO8601 now}
   - AutomationExecutor.execute(automation, context)
3. Helper: scheduleAll(context) — enqueueUniquePeriodicWork per automation dengan interval yang benar
4. Min interval 15 menit (WorkManager constraint)
5. Integration test (minimal enqueue + verify)

Commit: "feat(service): add interval heartbeat worker"
```

## Task #8 — AutomationExecutor & Foreground Service

```
Task #8: Orchestrator.

Baca: AGENTS.md (domain/execution)

Buat:
1. AutomationExecutor class:
   - suspend fun execute(automation, contextMap: Map<String,String>)
   - Render payload via TemplateEngine
   - Panggil WebhookClient.send
   - Simpan ExecutionLog (SUCCESS/FAILED)
   - Kalau network error & offline → status QUEUED, simpan di Room queue
2. ForegroundService:
   - Notifikasi persistent "QRISHook aktif · N automation berjalan"
   - Start/stop berdasarkan jumlah automation aktif
   - Auto-start saat boot (BroadcastReceiver)
3. Sync worker untuk proses offline queue saat online
4. Integration test dengan fake repository

Commit: "feat(execution): add executor, offline queue, foreground service"
```

## Task #9 — UI Components Reusable

```
Task #9: 16 UI components reusable.

Baca: docs/UI_SPEC.md section 4 (Global Reusable Components)

Buat di /ui/components/:
1. QRISHookTopAppBar
2. QRISHookButton (dengan loading)
3. QRISHookTextField
4. QRISHookCard
5. StatusChip (SUCCESS/FAILED/QUEUED)
6. EmptyState
7. LoadingState
8. ErrorBanner
9. AutomationCard
10. LogItem
11. TriggerOptionCard
12. PermissionCard
13. HeaderRow
14. PlaceholderChip
15. SectionHeader
16. KeyValueRow

Semua dengan @Preview. Ikuti design tokens (spacing, typography).

Commit: "feat(ui): add reusable components"
```

## Task #10 — Onboarding + Home

```
Task #10: Screen Onboarding + Home.

Baca: docs/UI_SPEC.md section 5 (Onboarding), section 6 (Home)

Buat:
1. OnboardingScreen + ViewModel:
   - 3 PermissionCard dengan status detection
   - LifecycleEventObserver untuk re-check on resume
   - Navigate ke home saat "Mulai Sekarang" atau "Lewati"
2. HomeScreen + ViewModel:
   - StateFlow<List<Automation>>
   - Loading/Empty/List state
   - AutomationCard dengan toggle
   - FAB → wizard
   - AppBar action → logs, settings
   - ErrorBanner kalau izin kurang
3. Wire navigasi dari MainActivity

Commit: "feat(ui): add onboarding and home screens"
```

## Task #11 — Wizard

```
Task #11: Wizard 5 step.

Baca: docs/UI_SPEC.md section 7

Buat:
1. CreateAutomationScreen (single screen, currentStep state):
   - ViewModel dengan wizardState: WizardState
   - Step 1-5 sesuai spec
   - LinearProgressIndicator
   - Bottom bar Kembali/Lanjut
2. Sub-composables per step:
   - StepNameStep, TriggerSelectStep, NotificationConfigStep, IntervalConfigStep, WebhookConfigStep, TestAndSaveStep
3. Validasi per step
4. Test button di step 5 (panggil WebhookClient dengan dummy)
5. Simpan → Room + start ForegroundService kalau aktif

Commit: "feat(ui): add automation creation wizard"
```

## Task #12 — Automation Detail

```
Task #12: AutomationDetailScreen.

Baca: docs/UI_SPEC.md section 8

Buat:
1. AutomationDetailScreen + ViewModel:
   - Load automation by id
   - 4 InfoCard (Status, Pemicu, Tindakan, Statistik)
   - Edit → wizard?automationId={id}
   - Delete → AlertDialog → pop back
   - "Lihat Log" → logs?automationId={id}

Commit: "feat(ui): add automation detail screen"
```

## Task #13 — Log Screens + Settings

```
Task #13: Log list, Log detail, Settings.

Baca: docs/UI_SPEC.md section 9, 10, 11

Buat:
1. LogListScreen + ViewModel:
   - Filter chips
   - LazyColumn LogItem
   - Query param automationId → filter & title
2. LogDetailScreen:
   - Header card status
   - Info section
   - Payload/Response monospace
   - "Kirim Ulang" → AutomationExecutor.execute
3. SettingsScreen:
   - 3 izin ListItem (open settings)
   - Troubleshooting (cek service, panduan OEM)
   - Tentang (versi, source code link)
   - Disclaimer Card

Commit: "feat(ui): add log and settings screens"
```

## Task #14 — Wire Up & Polish

```
Task #14: Final wiring & polish.

Baca: docs/UI_SPEC.md section 13 (State matrix), section 14 (A11y)

Buat:
1. Cek semua route navigasi berfungsi
2. Snackbar host di Scaffold
3. Empty/loading/error state sesuai state matrix
4. Dark mode otomatis dari system
5. A11y: contentDescription di semua IconButton
6. String resources (jangan hardcode)
7. Preview @Preview per screen
8. Manual smoke test semua flow

Commit: "feat: wire up all screens and polish"
```

## Task #15 — GitHub Actions CI

```
Task #15: GitHub Actions workflow build APK.

Baca: build.gradle.kts (verifikasi AGP/JDK version)

Buat .github/workflows/build-apk.yml:
- name: "Build QRISHook APK"
- on: [push (main/master), pull_request, workflow_dispatch]
- runs-on: ubuntu-latest
- steps:
  1. checkout@v4
  2. setup-java@v4 (JDK 17, temurin)
  3. gradle/actions/setup-gradle@v4
  4. chmod +x gradlew
  5. ./gradlew testDebugUnitTest (continue-on-error: true)
  6. ./gradlew assembleDebug
  7. upload-artifact@v4:
     name: qrishook-debug-apk
     path: app/build/outputs/apk/debug/*.apk
     retention-days: 7
     if: always()

Commit: "ci: add GitHub Actions workflow for debug APK build"
```

---

## 🎯 Alur Eksekusi

1. Task #1 → verify build sukses → commit
2. Task #2 → ... (berurutan sampai #15)
3. Setiap task: **Mode Plan** → review → **Mode Build** → verify → commit
4. Kalau stuck: ESC → back to Plan → tanya "apa yang bikin stuck?"
5. Test di HP fisik mulai Task #6

## 💡 Tips

- Jangan skip task — tiap task dibangun di atas yang sebelumnya
- Kalau build fail setelah task tertentu: `git checkout .` lalu fix di task itu
- Setiap task selesai: `./gradlew assembleDebug` harus hijau
