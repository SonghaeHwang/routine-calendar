# 7단계 백업 사전 조사

> 조사일: 2026-11-03 · 대상: PLAN 7단계(백업) · 근거는 각 절 끝 출처(실제로 연 페이지만)
> 표시: **(확인 안 됨)** = 공식 문서에서 직접 확인하지 못한 내용 · **(추론)** = 문서 문장을 바탕으로 한 판단

## 결론 요약

1. **JSON 내보내기/가져오기**: `rememberLauncherForActivityResult` + `CreateDocument("application/json")` / `OpenDocument()` 로 파일을 고르고, `ContentResolver` 스트림으로 쓰고 읽는다. SAF 는 시스템 권한이 필요 없다(공식 문서 확인). `INTERNET` · 저장소 권한 추가 없음.
2. **형식**: 맨 위에 `format` · `schemaVersion`(정수, 1부터) · `exportedAt`(epoch millis). 루틴 안에 완료 기록을 **중첩**해 FK 와 ID 재매핑 문제를 없앤다. 빠른 선택(`preset`)과 설정(`week_start` · `start_screen`)도 넣는다. `LocalDate` 는 ISO 문자열(`"2026-11-02"`).
3. **가져오기 정책**: 1차는 **전체 교체(덮어쓰기)만**. 먼저 전부 파싱 · 검증하고, 통과하면 DAO 의 `@Transaction` 함수 하나에서 지우고 넣는다(기존 `RoutineDao` 와 같은 방식). 실패하면 아무것도 바뀌지 않는다. 합치기는 열린 질문으로 둔다.
4. **빌드**: 지금은 `kotlinx-serialization-core` 만 있다. JSON 은 **`kotlinx-serialization-json` 1.9.0 을 추가해야 한다**(같은 판, Kotlin 2.2.10 호환).
5. **allowBackup 추천: 켠다(`true`) + 암호화 조건을 건다.** Android 12+ 는 `<cloud-backup disableIfNoEncryptionCapabilities="true">`, Android 11 이하는 `requireFlags="clientSideEncryption"`. 그러면 Google Drive 로는 **종단간 암호화될 때만** 올라가고, 기기 간 직접 이전(D2D)은 서버를 거치지 않는다. 사용자는 새 폰에서 아무것도 안 해도 복원되고, JSON 은 수동 보험이 된다.
6. **데이터 보안 양식**: Play 도움말에 "Android 시스템 백업" 을 콕 집은 예외 문구는 **없다**(확인함). 다만 ① 종단간 암호화 데이터는 신고 대상이 아니라는 문장, ② 사용자가 자기 Google Drive 로 직접 올리고 앱이 그 데이터에 접근하지 않으면 수집 신고가 필요 없다는 FAQ 가 있다. 위 5번 설정이면 둘 다에 맞으므로 "수집 · 공유 없음" 유지가 가능하다고 본다 **(추론)**. 개인정보처리방침에 "Android 기본 백업 · 기기 이전으로 사용자 계정에 암호화 저장될 수 있음, 개발자는 접근 불가" 를 한 줄 적는다.
7. **시험**: JSON 왕복은 Robolectric + 메모리 DB 단위 시험(기존 시험 구조 그대로). 자동 백업은 `bmgr` 스크립트로 확인하되, **스크립트가 앱을 삭제 · 재설치하므로 실사용 S26 에서는 먼저 JSON 으로 내보내 둔다.**

---

## 1. SAF 로 JSON 내보내기 / 가져오기

- 권한: 공식 문서 — "Because the user is involved in selecting the files or directories that your app can access, this mechanism doesn't require any system permissions". minSdk 26 이면 전부 지원(API 19+).
- `ACTION_CREATE_DOCUMENT` 는 **기존 파일을 덮어쓰지 않는다** — 같은 이름이면 시스템이 `name(1).json` 처럼 번호를 붙인다. 그래서 제안 파일명에 날짜를 넣는다: `routine-calendar-2026-11-03.json`.
- `CreateDocument()` 무인자 생성자는 1.5.0 에서 폐기. **`CreateDocument("application/json")`** 처럼 구체 MIME 을 준다(확장자 자동 처리). 입력 = 제안 파일명(String), 결과 = `Uri?`(취소 시 null).
- 결과 콜백은 화면이 만들어질 때마다 **무조건 등록**해야 한다(프로세스가 죽었다 살아나도 결과를 받기 위해). Compose 에서는 `rememberLauncherForActivityResult` 를 컴포저블 최상단에서 부른다.
- `takePersistableUriPermission` 은 필요 없다(한 번 쓰고 끝). 문서: 열 때 받은 권한은 재부팅 전까지 유효.
- 입출력은 백그라운드 스레드(`Dispatchers.IO`)에서.

```kotlin
// ui/settings — 런처는 조건 없이 등록
val export = rememberLauncherForActivityResult(CreateDocument("application/json")) { uri ->
    uri?.let(viewModel::exportTo)          // null = 사용자가 취소
}
val import = rememberLauncherForActivityResult(OpenDocument()) { uri -> uri?.let(viewModel::importFrom) }
// 버튼: export.launch("routine-calendar-$today.json") · import.launch(arrayOf("application/json"))

// backup/ — 쓰기 · 읽기
suspend fun write(resolver: ContentResolver, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    resolver.openOutputStream(uri, "wt")?.use { it.write(text.encodeToByteArray()) }
        ?: throw IOException("파일을 열 수 없음")
}
suspend fun read(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
    resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: throw IOException("파일을 열 수 없음")
}
```

- 메신저 · 드라이브에서 받은 파일은 MIME 이 `application/octet-stream` 이나 `text/plain` 으로 잡혀 `application/json` 필터에 안 보일 수 있다 **(확인 안 됨, 경험칙)**. 필요하면 `arrayOf("application/json", "text/plain", "application/octet-stream")` 로 넓히고 내용 검증으로 거른다.
- 공식 예시는 `openFileDescriptor(uri, "w")` 를 쓴다. `"wt"`(내용 비우고 쓰기) 권장은 관행 **(확인 안 됨)** — 새로 만든 문서라 어느 쪽이든 동작해야 한다.

출처: [Access documents and other files from shared storage](https://developer.android.com/training/data-storage/shared/documents-files) · [ActivityResultContracts.CreateDocument](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument) · [Get a result from an activity](https://developer.android.com/training/basics/intents/result)

## 2. 백업 JSON 형식 설계

### 추천 형식 (schemaVersion 1)

```json
{
  "format": "routine-calendar-backup",
  "schemaVersion": 1,
  "exportedAt": 1793664000000,
  "appVersion": "1.0",
  "settings": { "weekStart": "SUNDAY", "startScreen": "CALENDAR" },
  "routines": [
    { "title": "이불 세탁", "category": "HOUSE", "intervalAmount": 2, "intervalUnit": "WEEK",
      "repeats": true, "firstDue": "2026-10-05", "notify": true, "archived": false,
      "createdAt": 1791100000000,
      "completions": [ { "doneOn": "2026-10-19", "createdAt": 1792400000000 } ] }
  ],
  "presets": [ { "title": "각질 제거", "category": "BEAUTY", "intervalAmount": 10,
                 "intervalUnit": "DAY", "repeats": true, "position": 0 } ]
}
```

- **DB `id` 를 넣지 않는다.** 완료 기록을 루틴 안에 중첩하면 `routine_id` 가 필요 없고, 가져올 때 새 id 로 넣은 뒤 그 id 로 완료 기록을 넣으면 된다(ID 재매핑이 저절로 해결). Room 스키마 버전과 백업 `schemaVersion` 은 **따로** 관리한다(DB 버전이 올라도 JSON 형식이 같으면 1 유지).
- 시각은 기존 결정대로 epoch millis `Long`(`kotlin.time.Instant` opt-in 회피). enum 은 이름 문자열(`Category` 주석: "이름은 바꾸지 않는다" 와 맞음).
- DTO 는 `backup/` 에 따로 둔다(엔티티 · `:core` 모델에 `@Serializable` 를 붙이지 않음 — 저장 형식과 파일 형식을 분리).
- **2026-11-03 바뀜 (DB 버전 3):** 분류가 고정 enum 에서 사용자가 고치는 `category` 테이블(이름 · 색 · 순서)로 바뀌었다. 위 예시의 `"category": "HOUSE"` 는 더 이상 맞지 않는다. 7단계에서는 최상위에 `"categories": [{ "key": 1, "name", "color", "position", "removable" }]` 를 두고 루틴 · 빠른 선택은 그 `key` 로 가리키게 한다(루틴과 같은 방식으로 가져올 때 id 재매핑). 설정도 알림 켜기 · 시각이 더해졌다.

### LocalDate 직렬화

- kotlinx-datetime 의 `LocalDate` 는 기본 직렬화기가 있어 `@Serializable` 클래스 안에서 그대로 ISO 문자열로 나간다 — 0.2.0 발표 글 기준, 현재 판 이름은 **(확인 안 됨)**. 명시하려면 `@Serializable(with = LocalDateIso8601Serializer::class)` (API 문서: "uses the ISO 8601 representation"). 형식을 고정하려고 **명시를 추천**.
- kotlinx-datetime README: Instant 를 직렬화한다면 kotlinx-serialization 1.9.0 이상 필요 — 지금 1.9.0 이라 조건 충족.

```kotlin
@Serializable data class BackupFile(val format: String, val schemaVersion: Int, val exportedAt: Long,
    val appVersion: String, val settings: SettingsDto, val routines: List<RoutineDto>, val presets: List<PresetDto>)
@Serializable data class CompletionDto(
    @Serializable(with = LocalDateIso8601Serializer::class) val doneOn: LocalDate, val createdAt: Long)

val BackupJson = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
```

### 가져올 때 검증 (DB 를 건드리기 전에 전부)

| 경우 | 처리 |
|---|---|
| JSON 아님 · 필드 누락 · 모르는 enum | `SerializationException` / `IllegalArgumentException` → "백업 파일이 아니거나 손상됨" |
| `format` 불일치 | 거부 |
| `schemaVersion` > 현재 | 거부 — "새 버전 앱에서 만든 파일, 앱을 업데이트하세요" |
| `schemaVersion` < 현재 | 버전별 변환 함수로 올린 뒤 진행 |
| 값 범위 | 주기 1~365, 제목 공백 아님, `doneOn` 이 미래 아님(선택) |
| 같은 루틴 같은 날 중복 | 중복 제거(유일 인덱스와 충돌 방지) |
| 파일 크기 | 예: 5MB 넘으면 거부(비정상 파일 방어) |

- `ignoreUnknownKeys = true` 로 같은 버전 안의 추가 필드는 무시(앞으로 필드를 더해도 옛 앱이 깨지지 않게). 날짜 파싱 실패 예외 종류는 **(확인 안 됨)** — 시험으로 고정.
- 가져오기 전 확인 창: "루틴 N개 · 기록 M개로 **지금 데이터를 모두 바꿉니다**". 미리 현재 상태를 자동 내보내기할지는 열린 질문.

### 원자적 교체

기존 `RoutineDao.insertWithInitialCompletion` 처럼 추상 DAO 의 `@Transaction open suspend fun` 하나로 묶는다. 중간에 예외가 나면 전체가 롤백된다.

```kotlin
@Transaction
open suspend fun replaceAll(routines: List<Pair<RoutineEntity, List<CompletionEntity>>>, presets: List<PresetEntity>) {
    deleteAllRoutines()            // completion 은 FK CASCADE 로 함께 삭제
    deleteAllPresets()
    for ((routine, completions) in routines) {
        val id = insertRoutine(routine.copy(id = 0))
        insertCompletions(completions.map { it.copy(id = 0, routineId = id) })
    }
    insertPresets(presets.map { it.copy(id = 0) })
}
```

- 설정(DataStore)은 Room 트랜잭션에 묶이지 않는다. DB 교체 성공 뒤 `dataStore.edit {}` 한 번으로 둘 다 쓴다(설정만 실패해도 피해가 작음).
- 참고: developer.android.com 의 Room DAO 페이지는 이제 **Room 3** 기준(`withWriteTransaction` · `useWriterConnection`)으로 바뀌었다. 지금 쓰는 Room 2.8.5 에서는 `@Transaction` DAO 함수가 이미 프로젝트에서 검증된 방법이다.

출처: [kotlinx.datetime.serializers API](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.serializers/) · [kotlinx-datetime README](https://github.com/Kotlin/kotlinx-datetime/blob/master/README.md) · [Accessing data using Room DAOs](https://developer.android.com/training/data-storage/room/accessing-data)

## 3. Android 자동 백업 / 기기 간 이전(D2D)

- **기본값**: `allowBackup` 기본 `true`, API 23+ 앱은 자동 참여. 지금 Manifest 는 `true` + 비어 있는 템플릿 규칙 2개(= 사실상 전부 백업).
- **기본 포함**: SharedPreferences, `getFilesDir()`, `getDatabasePath()`, `getExternalFilesDir()`. 제외: cache · code cache · `no_backup`. → Room DB(`databases/routine-calendar.db`)와 DataStore(`files/datastore/settings.preferences_pb`)는 **기본 포함**.
- **저장 · 암호화**: 사용자 Google Drive 의 비공개 폴더(앱당 25MB, 사용자 용량 미차감, 사용자 · 다른 앱이 읽을 수 없음, 최신 1개만). 원문: "The backup is end-to-end encrypted on devices running Android 9 or higher using the device's PIN, pattern, or password." 잠금 화면이 없으면 종단간 암호화가 안 된다.
- **시점**: 백업 켜짐 + 지난 백업 후 24시간 + 기기 유휴 + Wi-Fi. 백업 중 앱을 종료시킨다(그래서 Room WAL 파일도 일관된 상태로 복사될 것 — **(추론)**). 복원은 앱 설치 직후 · 실행 전.
- **두 설정 파일**:
  - `fullBackupContent`(`backup_rules.xml`): Android 11 이하 기기용. `requireFlags="clientSideEncryption|deviceToDeviceTransfer"`(API 28+) 가능. 조건을 걸면 API 26~27 에서는 백업이 아예 안 된다(문서: "prevents backups from working on previous Android versions").
  - `dataExtractionRules`(`data_extraction_rules.xml`): targetSdk 31+ 앱이 Android 12+ 기기에서 사용. 이걸 지정하면 그 기기에서 `fullBackupContent` 는 무시. `<cloud-backup>` 과 `<device-transfer>` 를 **따로** 정한다. 섹션이 없으면 그 방식은 전부 허용. `disableIfNoEncryptionCapabilities="true"` 면 암호화 불가 기기에서 클라우드 백업 중단, D2D 는 계속.
  - `<include>` 를 하나라도 쓰면 **그것만** 백업된다(기본 포함이 꺼짐).
- **주의**: Android 12+ 에서 일부 제조사 기기는 `allowBackup="false"` 여도 **D2D 는 막히지 않는다**(공식 문서). 즉 꺼도 "기기 밖으로 절대 안 나감" 을 보장할 수 없다. 삼성 Smart Switch 가 여기에 해당하는지는 **(확인 안 됨)**.
- 크기 한도 표기가 문서마다 다르다: 자동 백업 페이지는 25MB, 시험 페이지는 "cloud backup 2 MB, transfer 2 GB". 이 앱 데이터는 수십 KB 수준이라 어느 쪽이든 문제없다.
- 데이터 백업 개요 문서는 사용자 생성 데이터와 설정을 백업하라고 권하고, **URI 는 백업하지 말라**고 한다(이 앱은 해당 없음).

| | 켬 (추천: 암호화 조건 포함) | 끔 |
|---|---|---|
| 새 폰 | 설치만 하면 루틴 · 기록 · 설정 자동 복원 | JSON 을 잊으면 전부 잃음 |
| 개인정보 | 사용자 계정에 종단간 암호화, 개발자 접근 불가 | 가장 단순한 설명 |
| 양식 | 근거 문장 2개로 "수집 없음" 가능 **(추론)** | 걱정 없음(단, 일부 기기 D2D 는 여전히 일어남) |
| 일 | XML 두 개 작성 + bmgr 시험 | Manifest 한 줄 |
| 위험 | 복원된 옛 DB → Room 마이그레이션 필요(이미 `exportSchema` 로 대비), 알림 예약 재등록 필요 | 없음 |

추천 설정(안):

```xml
<!-- data_extraction_rules.xml (Android 12+) -->
<data-extraction-rules>
  <cloud-backup disableIfNoEncryptionCapabilities="true">
    <include domain="database" path="routine-calendar.db" />
    <include domain="file" path="datastore/" />
  </cloud-backup>
  <device-transfer>
    <include domain="database" path="routine-calendar.db" />
    <include domain="file" path="datastore/" />
  </device-transfer>
</data-extraction-rules>
<!-- backup_rules.xml (Android 11 이하) -->
<full-backup-content>
  <include domain="database" path="routine-calendar.db" requireFlags="clientSideEncryption" />
  <include domain="file" path="datastore/" requireFlags="clientSideEncryption" />
</full-backup-content>
```

- `path` 는 와일드카드가 없다. DB 파일 하나만 지정하면 `-wal` · `-shm` 이 빠질 수 있으니 `database` 도메인 `path="."` 로 둘지 시험으로 정한다 **(확인 안 됨)**. WorkManager 내부 DB 의 위치(백업 제외 폴더인지)도 **(확인 안 됨)** — 5단계 알림은 앱 시작 때 유일 작업으로 다시 등록하게 해 두면 안전.

출처: [Back up user data with Auto Backup](https://developer.android.com/identity/data/autobackup) · [Android 12 behavior changes — Backup and restore](https://developer.android.com/about/versions/12/backup-restore) · [Data backup overview](https://developer.android.com/guide/topics/data/backup) · [PreferenceDataStoreFile](https://developer.android.com/reference/androidx/datastore/preferences/PreferenceDataStoreFile)

## 4. Play 데이터 보안 양식과의 관계

- 수집의 정의: "'Collect' means transmitting data from your app off a user's device."
- 신고하지 않아도 되는 것(원문):
  - "On-device access/processing: User data accessed by your app that is only processed locally on the user's device and not sent off device does not need to be disclosed."
  - "End-to-end encryption: User data that is sent off device, but that is unreadable by you or anyone other than the sender and recipient as a result of end-to-end encryption does not need to be disclosed." (단, "The encrypted data must not be readable by any intermediary entity, including the developer")
- FAQ(원문): 질문 "My app enables users to upload their data directly to Google Drive or Dropbox for backup or storage. My app does not access any of this data. Should that still be disclosed as "collection"?" — 답 "It depends on the particular implementation. If the user chooses to upload their data directly to their own external drive or cloud storage account (such as Google Drive, Dropbox, or similar services) and this upload is governed by the external drive or cloud storage provider's terms of service and privacy policy, and your app never collects or accesses the data in question, then your app does not need to declare the collection of this data."
- **"Android 시스템 백업 · Auto Backup · allowBackup" 를 직접 언급한 문장은 이 도움말에 없다**(페이지에서 "backup" 은 위 FAQ 한 곳뿐). 따라서 자동 백업이 예외라는 **명시적 공식 문구는 확인 안 됨**.
- 판단 **(추론)**: 자동 백업은 OS 가 사용자 Google 계정으로 보내고, 개발자는 받지도 읽지도 못한다. `disableIfNoEncryptionCapabilities` / `clientSideEncryption` 조건을 걸면 클라우드로 가는 데이터는 늘 종단간 암호화 상태이므로 위 "End-to-end encryption" 문장에 그대로 맞고, FAQ 의 "앱이 접근하지 않음" 조건에도 맞는다. JSON 내보내기는 사용자가 고른 위치에 쓰는 기기 내 동작이다. → "수집 · 공유 없음" 으로 적되, 9단계 제출 직전에 도움말을 다시 확인한다.

출처: [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)

## 5. 시험 방법

### JSON 왕복 단위 시험 (`app/src/test`, Robolectric + 메모리 DB — 기존 `RoutineDaoTest` 구조)

- `BackupCodecTest`(순수): DTO → 문자열 → DTO 가 같음, `LocalDate` 가 `"2026-01-31"` 로 나감, 모르는 키 무시, `schemaVersion` 초과 거부, 깨진 JSON · 모르는 enum · 주기 0 거부, 중복 완료 제거.
- `BackupRepositoryTest`: 루틴(반복/1회/보관) · 완료 기록 · 프리셋 · 설정 저장 → 내보내기 → **DB 비우기** → 가져오기 → 같은 상태(id 제외 비교, 다음 예정일 계산 결과도 같음). 검증 실패 파일 가져오기 시 기존 데이터가 **그대로**인지(원자성). 완료 기준("지우고 다시 가져와 같은 상태")을 그대로 시험으로.
- SAF 자체는 기기에서 수동 확인(내 파일 · Google Drive 앱 선택기 둘 다).

### 자동 백업 · D2D (adb, 공식 시험 페이지)

```bash
adb shell bmgr enable true
adb shell bmgr transport com.android.localtransport/.LocalTransport     # 로컬 전송으로
adb shell settings put secure backup_local_transport_parameters 'is_encrypted=true'
adb shell bmgr backupnow com.kikilog.routinecalendar                    # "Success" 확인
# → 앱 삭제 후 같은 APK 재설치(설치 시 복원) → 데이터 확인
adb shell bmgr transport com.google.android.gms/.backup.BackupTransportService  # 원래대로
```

- D2D 는 `settings put secure backup_enable_d2d_test_mode 1` + `bmgr transport/init com.google.android.gms/.backup.migrate.service.D2dTransport` 후 같은 흐름(공식 `test_d2d.sh`). Android 12+ 필요(S26 충족).
- 클라우드 시험은 기기에 인터넷 · Google 계정 백업 설정 필요(앱 권한과 무관).
- 문제 해결 로그: `Full backup not currently possible` → `adb shell bmgr run` 후 재시도. `doesn't have any backup data` → include 규칙 확인.
- **주의**: 공식 스크립트는 `pm uninstall` 로 앱을 지운다 — CLAUDE.md 기준 "거절 추천" 명령이고 실사용 데이터가 사라진다. 시험 전 반드시 JSON 내보내기, 사용자에게 먼저 확인. 삼성 기기에 LocalTransport 가 있는지는 **(확인 안 됨)** — `adb shell bmgr list transports` 로 먼저 본다.

출처: [Test backup and restore](https://developer.android.com/identity/data/testingbackup)

---

## 구현 순서 체크리스트

- [ ] ① `libs.versions.toml` 에 `kotlinx-serialization-json`(1.9.0, `kotlinxSerialization` 참조) 추가 → `app` 의존성
- [ ] ② `backup/` DTO + `BackupJson` + 엔티티 ↔ DTO 변환 + 검증 함수 → `BackupCodecTest`
- [ ] ③ DAO: 전체 조회(루틴 + 완료 + 프리셋), `replaceAll` `@Transaction` → DAO 시험
- [ ] ④ `BackupRepository`(내보내기 문자열 생성 · 가져오기 검증 → 교체 → 설정 쓰기) + `AppContainer` 등록 → 왕복 · 원자성 시험
- [ ] ⑤ 설정 화면: 「내보내기」 · 「가져오기」 + 덮어쓰기 확인 창 + 결과 스낵바(성공 개수 / 실패 이유)
- [ ] ⑥ S26 수동 확인: 내보내기 → 앱 데이터 삭제 → 가져오기 → 같은 상태
- [ ] ⑦ `allowBackup` 결정 반영: 두 XML 작성(위 추천안), PLAN "5. 아직 정하지 않은 것" 갱신
- [ ] ⑧ (사용자 확인 후) `bmgr` 백업 · 복원 시험, D2D 시험
- [ ] ⑨ 개인정보처리방침 초안에 백업 문구 메모(9단계용), DEVLOG

## 정한 것 (2026-11-03 사용자 답)

- 질문 1 → **합치기도 지원**한다(덮어쓰기와 둘 다). 같은 이름 루틴 · 분류를 어떻게 합칠지 규칙은 7단계 계획 때 정한다
- 질문 2 → 가져오기 직전 현재 상태를 앱 안에 **자동 저장**한다(되돌리기용)
- 질문 3 → `allowBackup` **켬 + 암호화 조건**(위 추천 XML)
- 순서: 6단계 위젯을 먼저 하고 7단계 백업은 그 뒤. `bmgr` 시험(앱 삭제 포함)은 실행 전 반드시 사용자 확인, 그 전에 JSON 내보내기로 데이터를 먼저 저장

## 열린 질문

1. 가져오기를 **덮어쓰기만** 할지, 합치기(같은 제목 루틴 처리 규칙 필요)도 둘지.
2. 가져오기 직전 현재 상태를 앱 내부에 자동 저장(되돌리기용)할지 — 간단함 원칙과 저울질.
3. `allowBackup` 을 추천대로 켤지 최종 결정(사용자). 끈다면 Android 12+ 일부 기기의 D2D 는 여전히 일어난다는 점을 받아들일지.
4. 5단계 알림 시각(`notify_time`)이 생기면 `settings` 에 추가 — `schemaVersion` 은 그대로 두고 선택 필드로 넣을지.
5. 백업 파일에 `appVersion` 외에 루틴 수 같은 요약을 넣어 가져오기 확인 창에 보일지.
6. 실사용 S26 에서 `bmgr` 시험(앱 삭제 포함)을 할지, 내보내기 확인 후에만 할지.
