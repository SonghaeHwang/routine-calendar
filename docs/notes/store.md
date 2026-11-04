# 9단계 스토어 등록 자료 · 업로드 키 사전 조사

> 조사일: 2026-11-04 · 대상: PLAN 9단계(출시 준비 중 「업로드 키 생성 · 스토어 문구 · 스크린샷」)
> 근거는 각 절 끝 출처(실제로 연 페이지만). 개인정보 · 데이터 보안은 [privacy.md](privacy.md), 아이콘은 [icon.md](icon.md) 에 있다.
> 표시: **(확인 안 됨)** = 공식 문서나 실물에서 직접 확인하지 못한 내용 · **(추론)** = 문서 문장을 바탕으로 한 판단

## 결론 요약 (추천)

1. **이름은 「때됐냥 - 자취 루틴 달력」 같은 「짧은 이름 + 뜻 풀이 한 마디」 를 추천.** 제목은 30자 이하. 「살림주기」 · 「살림루틴」 은 Play 에 이미 같은 이름의 비슷한 앱이 있어 뺀다. 「주기 달력」 은 검색 결과가 생리 주기 앱뿐이라 오해를 산다.
2. **스토어 문구는 한국어 하나로 시작.** Play Console 의 기본 언어가 en-US 로 잡히므로 **ko-KR 로 바꾼다**. 앱 화면이 한국어뿐이라 영어 등록정보는 지금 필요 없다 **(추론)**. 번역이 없으면 Play 가 자동 번역을 보여 준다.
3. **휴대폰 스크린샷은 S26 원본(1080×2340)을 그대로 못 쓴다.** 긴 변이 짧은 변의 2배를 넘으면 안 된다(2340 ÷ 1080 ≈ 2.17). **1080×1920(9:16)** 으로 잘라 4장 이상 올린다(큰 홍보 자리 조건). 형식은 JPEG 또는 알파 없는 24비트 PNG.
4. **시연 데이터는 이미 있는 백업 기능으로 넣는다.** 지금 상태 내보내기 → 시연용 JSON 「덮어쓰기」 가져오기 → 촬영 → 「가져오기 전으로 되돌리기」. 코드 변경이 필요 없다.
5. **업로드 키는 Android Studio 「Generate Signed Bundle」 로 만들고 저장소 밖에 둔다.** 새 앱은 Play App Signing 이 필수(2021-08 이후 앱)이고 AAB 로 올린다. 업로드 키를 잃어도 Play Console 에서 재설정을 요청할 수 있다. 키는 RSA 2048 이상, 유효 기간 25년 이상.
6. **서명 설정은 `keystore.properties` 를 읽되, 파일이 없으면 건너뛰게 쓴다.** 공식 예시는 파일이 없으면 빌드가 깨진다. `.gitignore` 에 `keystore.properties` · `*.jks` · `*.keystore` · `*.aab` 가 이미 있다(확인함).
7. **release 빌드 R8 설정을 첫 업로드 전에 정리한다.** 지금 `packageScope`(androidx · kotlin · kotlinx 만 최적화)는 실험 기능이고, 공식 문서는 `gradle.properties` 에 `android.r8.gradual.support=true` 를 요구하는데 **이 줄이 없다**. `bundleRelease` 를 한 번 돌려 보고, release APK 를 휴대폰에서 위젯 · 알림 · 백업까지 확인한다.
8. **targetSdk 37 은 요구(2026-08-31 부터 새 앱 36 이상)를 넘는다.** 카테고리는 「라이프스타일」, 태그는 최대 5개, 연락처는 이메일(필수 · 공개) + 웹사이트(방침 페이지).

---

## 0. 지금 상태 (프로젝트에서 읽은 것, 2026-11-04)

| 항목 | 값 | 확인한 곳 |
|---|---|---|
| `applicationId` | `com.kikilog.routinecalendar` | `app/build.gradle.kts` |
| `versionCode` · `versionName` | `1` · `"1.0"` | 같은 파일 `defaultConfig` |
| `minSdk` · `targetSdk` · `compileSdk` | 26 · 37 · 37 | 같은 파일 |
| AGP · Kotlin | 9.5.0 · 2.2.10 | `gradle/libs.versions.toml` |
| 서명 설정(`signingConfigs`) | **없음.** release 는 아직 서명되지 않는다 | `app/build.gradle.kts` |
| release 최적화 | `optimization { enable = true; packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") }` | 같은 파일 |
| keep 규칙 | `app/src/main/keepRules/rules.keep` (주석뿐, 규칙 없음) | 파일 |
| `gradle.properties` | `android.r8.gradual.support` 없음 | 파일 |
| `.gitignore` | `local.properties` · `*.jks` · `*.keystore` · `keystore.properties` · `*.env` · `/app/release/` · `*.aab` · `*.apk` | 파일 |
| 앱 버전 표시 | 설정 화면이 `packageManager.getPackageInfo(...).versionName` 을 보여 준다(AGP 9 는 `BuildConfig` 를 기본으로 안 만듦) | `ui/settings/SettingsScreen.kt` |
| 로그 호출 | `app/src/main` 에 `Log.` · `println` **없음** | 코드 검색 |
| 앱 이름 | `app_name` = 「Routine Calendar」(임시) | `res/values/strings.xml` (privacy.md 1절) |

## 1. 앱 이름

### 규칙

- 제목은 **30자 이하**. 전각 · 반각 글자 모두 1자로 센다(한글도 1자).
- 메타데이터 정책: 이모지 · 이모티콘 · 반복 특수문자 금지. 브랜드가 아니면 전부 대문자 금지. 「#1」 · 「인기」 · 「올해의 앱」 같은 성적 표시 금지. 가격 · 할인 · 「한정 무료」 금지. 「에디터 추천」 · 「New」 같은 Play 프로그램 암시 금지.
- 스토어 문구 모범 사례: 제목에 「Free」 · 「No Ads」 를 넣지 말라고 한다(할인 홍보로 봄). → 「광고 없음」 은 제목이 아니라 설명에 둔다.
- 설명에는 「반복되거나 관련 없는 키워드」 를 쓰지 말라는 규정이 있다. 제목에도 「루틴 · 습관 · 달력 · 할일 · 위젯 · 플래너」 처럼 단어를 늘어놓는 것은 피한다 **(추론 — 제목 키워드 나열을 직접 금지하는 문장은 연 페이지에서 찾지 못함)**. 경쟁 앱 다수가 이렇게 쓰지만 따라 하지 않는다.
- 개발자 이름(kikilog)에도 같은 규칙이 걸린다.
- 이름은 언어마다 하나씩 따로 둘 수 있다.
- 런처 아이콘 아래 이름(`app_name`)과 스토어 제목은 달라도 된다. 홈 화면에서는 짧은 앞부분만 쓴다(예: 「때됐냥」) **(추론)**.

### 후보와 Play 검색 결과 (2026-11-04, play.google.com 한국 검색을 직접 열어 봄)

검색 결과는 「관련도」 순이라 완전 일치 검색이 아니다. 아래 「같은 이름 없음」 은 **첫 화면 10~15개 결과 안에 없었다는 뜻**이다.

| 후보 | 글자 수 | 뜻이 보이나 | Play 검색 결과 | 판단 |
|---|---|---|---|---|
| **때됐냥** (+ 「- 자취 루틴 달력」) | 3 (부제 포함 14) | 부제가 있으면 보임. 고양이 아이콘과 맞음 | 「때됐냥」 결과 1개(무관한 게임 「플라잉 캣」). 같은 · 비슷한 이름 없음 | **추천 1** |
| **루틴 달력** | 5 | 바로 보임 | 같은 제목 없음. 「갓생 루틴: 습관, 목표, 운동 캘린더」 · 「마이루틴」 · 「오늘의루틴」 등 일반 루틴 앱이 위에 뜸 | 추천 2 — 너무 일반어라 검색에 묻힘 **(추론)** |
| **자취 루틴** | 5 | 바로 보임(대상이 분명) | 같은 제목 없음. 「Jedero: 청소루틴」 · 「ADHD 정리 플래너: 집청소 루틴」 등 청소 루틴 앱이 뜸 | 추천 3 |
| **챙김 달력** | 5 | 조금 보임 | 같은 제목 없음(일반 달력 앱만 뜸) | 가능 |
| **돌아오는 날** | 6 | 「주기」 느낌은 있으나 무엇이 돌아오는지 모호 | 같은 제목 없음. 디데이 · 기념일 앱(「TheDayBefore」 등)이 뜸 | 가능 — 디데이 앱으로 오해 소지 **(추론)** |
| **냥루틴** | 3 | 고양이 루틴처럼 읽힘 | 같은 제목 없음. **「멍냥루틴」(Petnow, 반려동물)** 이 비슷 | 비추천 — 반려동물 앱으로 오해 |
| **주기 달력** | 5 | 「생리 주기」 로 읽힘 | 같은 제목 없음. 결과가 「더데이 - 생리 달력」 · 「심플 생리주기 달력」 등 생리 주기 앱 위주 | 비추천 |
| **살림 주기** · **살림 루틴** | 5 | 바로 보임 | **이미 있음**: 「살림주기: 교체일과 여분 기록」(위즈오션) · 「살림루틴: 집안일·교체주기」(더숲) — 기능도 비슷 | 제외 |
| (지금) **Routine Calendar** | 16 | 보임 | 같은 제목 없음. 「Routine: Calendars & Tasks」(routine.co) 가 비슷 | 한국어 앱에 영어 이름은 비추천 **(추론)** |

- 「살림주기」 · 「살림루틴」 은 주기 집안일 · 교체 주기 앱이라 **직접 경쟁 앱**이다. 출시 전에 두 앱의 기능 · 화면을 한 번 보고, 우리 앱 설명에서 다른 점(완료일 기준 다시 계산 · 월 달력 · 수집 없음)을 분명히 하는 것이 좋다 **(추론)**.
- 상표 등록 여부(KIPRIS)는 **(확인 안 됨)**. 포트폴리오 · 무료 앱이라 우선순위는 낮지만, 이름을 정하면 KIPRIS 에서 한 번 검색해 본다.
- 이름은 출시 뒤에도 바꿀 수 있다 **(추론 — 패키지 이름만 고정)**. 방침 주소도 패키지 이름을 쓰므로 영향 없다(privacy.md 4절).

출처: [Metadata (Play 정책)](https://support.google.com/googleplay/android-developer/answer/9898842?hl=en) · [Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en) · [Create and set up your app / store listing](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en) · Play 검색: [루틴 달력](https://play.google.com/store/search?q=%EB%A3%A8%ED%8B%B4%20%EB%8B%AC%EB%A0%A5&c=apps&hl=ko&gl=KR) · [자취 루틴](https://play.google.com/store/search?q=%EC%9E%90%EC%B7%A8%20%EB%A3%A8%ED%8B%B4&c=apps&hl=ko&gl=KR) · [때됐냥](https://play.google.com/store/search?q=%EB%95%8C%EB%90%90%EB%83%A5&c=apps&hl=ko&gl=KR) · [주기 달력](https://play.google.com/store/search?q=%EC%A3%BC%EA%B8%B0%20%EB%8B%AC%EB%A0%A5&c=apps&hl=ko&gl=KR) · [살림 주기](https://play.google.com/store/search?q=%EC%82%B4%EB%A6%BC%20%EC%A3%BC%EA%B8%B0&c=apps&hl=ko&gl=KR) · [자취 달력](https://play.google.com/store/search?q=%EC%9E%90%EC%B7%A8%20%EB%8B%AC%EB%A0%A5&c=apps&hl=ko&gl=KR) · [냥루틴](https://play.google.com/store/search?q=%EB%83%A5%EB%A3%A8%ED%8B%B4&c=apps&hl=ko&gl=KR) · [챙김 달력](https://play.google.com/store/search?q=%EC%B1%99%EA%B9%80%20%EB%8B%AC%EB%A0%A5&c=apps&hl=ko&gl=KR) · [돌아오는 날](https://play.google.com/store/search?q=%EB%8F%8C%EC%95%84%EC%98%A4%EB%8A%94%20%EB%82%A0&c=apps&hl=ko&gl=KR) · [주기 루틴](https://play.google.com/store/search?q=%EC%A3%BC%EA%B8%B0%20%EB%A3%A8%ED%8B%B4&c=apps&hl=ko&gl=KR) · [Routine Calendar (US)](https://play.google.com/store/search?q=Routine%20Calendar&c=apps&hl=en&gl=US)

## 2. 스토어 문구 규칙 · 언어

- 간단한 설명 **80자 이하**. 상세 페이지에서 처음 보이는 글. 「가장 큰 장점 요약」 을 쓰라고 한다.
- 자세한 설명 **4,000자 이하**. 간단한 설명을 그대로 되풀이하지 않는다.
- 설명은 기능을 정확히. 근거 없는 최상급(「최고의」) 금지. 출처 없는 사용자 후기 금지. 다른 앱과 비교 금지. 길이 · 반복 · 지나친 서식도 위반이 될 수 있다 → 짧게.
- 「수집 없음 · 인터넷 연결 없음 · 광고 없음」 은 사실이므로 설명에 넣는다. 데이터 보안 양식 답(privacy.md)과 같은 말이어야 한다 **(추론)**.
- 언어
  - 앱을 처음 만들면 기본 언어가 **영어(미국, en-US)** 다 → **한국어(ko-KR)** 로 바꾼다.
  - 번역을 따로 넣지 않으면 Play 가 Google 번역본 또는 기본 언어를 보여 준다.
  - 메타데이터 정책은 모든 번역에 똑같이 걸린다. 글자가 든 스크린샷은 언어마다 따로 만들라고 한다.
  - 앱 화면이 한국어뿐인데 영어 등록정보를 넣으면 영어 사용자가 설치 뒤 실망할 수 있다 → **지금은 한국어만** **(추론)**. 포트폴리오용 영어 소개는 README 에 둔다.

출처: [Create and set up your app / store listing](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en) · [Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en) · [Metadata (Play 정책)](https://support.google.com/googleplay/android-developer/answer/9898842?hl=en)

## 3. 그래픽 자료 규격

| 자료 | 규격 | 이 앱 |
|---|---|---|
| 앱 아이콘 | **512×512 px**, 32비트 PNG(알파 가능), 최대 1024KB | 벡터 고양이를 다시 그린 뒤 Image Asset Studio 의 Play Store 아이콘(`ic_launcher-playstore.png`)을 쓴다(icon.md 5절). 뒤 층 흰색 결정 → Play 흰 화면과 섞이는지 확인 |
| 그래픽 이미지(feature graphic) | **1024×500 px**, JPEG 또는 24비트 PNG(**알파 없음**) | 고양이 아이콘 + 앱 이름 + 달력 화면 일부. 필수 여부는 **(확인 안 됨)** — Console 에서 확인. 성적 · 가격 문구 금지 |
| 휴대폰 스크린샷 | 기기 종류마다 **최대 8장**. 게시하려면 기기 종류를 합쳐 **최소 2장**. JPEG 또는 24비트 PNG(알파 없음). 변 **320~3840 px**, **긴 변 ≤ 짧은 변 × 2** | 아래 |
| (권장) 큰 홍보 자리 | **1080 px 이상 4장 이상**, 세로 **9:16(1080×1920 이상)** 또는 가로 16:9 | 1080×1920 4~6장 |
| 태블릿 · 크롬북 스크린샷 | 4장 이상, 변 1080~7680 px, 16:9 · 9:16 | 태블릿 기기가 없음 → 휴대폰만 올린다. 휴대폰만 올려도 게시되는지는 「최소 2장 (기기 종류 합산)」 문장상 된다고 본다 **(추론)** |
| 미리보기 동영상 | 선택. YouTube 주소(공개 또는 일부 공개, 광고 끔) | PLAN 4절 시연 영상을 나중에 연결해도 됨 |

### S26 스크린샷을 그대로 쓸 수 있나

- S26 화면 1080×2340 → 2340 ÷ 1080 ≈ **2.17 > 2** → **그대로는 규칙 위반**.
- 방법: 위아래를 잘라 **1080×1920**(9:16, 권장 조건 충족)으로 맞춘다. 상태 표시줄과 아래 제스처 막대가 먼저 잘려 나가므로 시계 · 배터리 · 알림 아이콘이 함께 사라진다 **(추론)**. 내용이 많이 잘리면 차선으로 **1080×2160**(정확히 2:1) — 허용되지만 9:16 은 아니다.
- One UI 스크린샷 파일 형식(JPG · PNG)과 PNG 알파 채널 여부는 **(확인 안 됨)** → 자를 때 JPEG 또는 알파 없는 PNG 로 다시 저장한다.
- 정책: 스크린샷은 **실제 앱 화면**이어야 한다. 짧은 문구(태그라인)는 최소로. 「#1」 · 할인 표시 금지.

### 찍을 화면 (안, 4~6장)

1. 달력 — 이번 달, 분류 색 점 · 완료 ✓ · 놓친 날 빨간 테두리가 고루 보이게, 아래에 오늘 목록
2. 루틴 목록 — 분류 색 점 · 주기 · 다음 예정일
3. 루틴 등록 — 빠른 선택(프리셋) 칩과 주기 입력
4. 루틴 정보 — 앞으로 예정일 3개
5. 홈 화면 위젯 — 오늘 + 지난 것, 체크
6. (선택) 설정 — 알림 시각 · 백업 · 「인터넷에 연결하지 않아요」 안내 문구

다크 모드 1장을 섞을지는 열린 질문.

### 개인 데이터가 안 나오게 — 시연용 루틴 넣기

- **이미 있는 백업 기능만으로 한다(코드 변경 없음)**:
  1. 설정 → 내보내기로 지금 실제 데이터를 저장(보험).
  2. PC 에서 시연용 JSON 을 만든다(형식은 7단계 `schemaVersion` 1). 루틴은 PLAN 프리셋 이름(청소기 · 빨래 · 이불 세탁 · 모델링팩 · 각질제거 · 화장지 등)으로, 완료 기록은 이번 달에 고루 흩어 놓는다.
  3. 휴대폰으로 옮겨 설정 → 가져오기 → **덮어쓰기**.
  4. 촬영.
  5. 설정 → 「가져오기 전으로 되돌리기」 로 원래 데이터 복구. 7단계에서 S26 으로 확인한 흐름이다(PLAN 7단계).
- 시연 JSON 을 저장소에 둘지(예: `docs/store/demo-backup.json`)는 열린 질문. 개인 데이터가 없으므로 커밋해도 된다 **(추론)**. 시연 JSON 만들기는 코드 단계에서 Claude 가 도울 수 있다.
- 촬영 전: 알림 켜기 카드(「알림 켤까요?」)가 보이면 닫거나 알림을 켠다. 상태 표시줄 알림 아이콘은 잘라 내면 사라진다.
- 다른 길: 앱 데이터 삭제 후 손으로 등록 — 실제 데이터를 잃으므로 비추천. 디버그 전용 「시연 데이터」 버튼 — 코드가 늘어 비추천(간단함 우선).

출처: [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) · [Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en) · [Metadata (Play 정책)](https://support.google.com/googleplay/android-developer/answer/9898842?hl=en)

## 4. 앱 카테고리 · 태그 · 연락처

### 카테고리 (앱 유형: 앱, 게임 아님)

- 앱 카테고리 목록(원문): Art and Design · Auto and Vehicles · **Beauty** · ... · **House and Home** · **Lifestyle** · ... · **Productivity** · ... · **Tools** · ... · Weather.
- 이 앱은 집안일 · 뷰티 · 구매가 섞인 자취 생활 루틴이다.

| 후보 | 장점 | 단점 |
|---|---|---|
| **Lifestyle (라이프스타일)** (추천) | 자취 생활 전체를 덮는다 | 범위가 넓다 |
| Productivity (생산성) | 루틴 · 달력 앱 다수가 있는 곳 | 할 일 · 플래너 앱과 섞여 「할 일 앱」 으로 보임(PLAN 「하지 않을 것」 과 어긋남) **(추론)** |
| House and Home (집/홈) | 집안일과 맞음 | 뷰티 · 구매가 빠진다. 부동산 · 인테리어 앱 위주일 수 있음 **(확인 안 됨)** |

- 한국어 Console 의 카테고리 표기(예: 「라이프스타일」)는 **(확인 안 됨)**.

### 태그

- **최대 5개.** 「처음 보는 사람이 등록정보나 첫 화면만 보고 왜 붙었는지 알 수 있어야」 한다. 비슷한 장르로 사용자를 끌려는 태그는 안 된다.
- 위치: Play Console → Grow users > Store presence > Store settings → 「Manage tags」. 반영까지 최대 24시간.
- 태그 목록은 Console 이 정해 준 것에서 고른다 **(추론)**. 실제 이름은 **(확인 안 됨)** — 「달력 · 일정」 · 「집안일」 · 「미리 알림」 비슷한 것이 있으면 고른다. 「습관 추적」 은 기능이 없으므로 고르지 않는다(통계 · 연속 기록 없음).
- 접근성 태그(화면 낭독기 지원 등)는 TalkBack 을 끝까지 확인한 뒤에만 붙인다 **(추론)**.

### 연락처 (Store settings > Contact details)

- **이메일: 필수.** 「연락처는 스토어 등록정보에 사용자에게 공개된다」. → 방침의 `<연락 이메일>` 과 같은 주소를 쓴다(privacy.md 열린 질문 2: 앱 전용 주소 추천).
- **웹사이트: 권장.** → 방침 GitHub Pages 주소(또는 그 상위 폴더)를 쓴다 **(추론)**.
- **전화: 선택.** → 비운다(개인 번호 공개 방지).

출처: [Choose a category and tags for your app](https://support.google.com/googleplay/android-developer/answer/9859673?hl=en) · [Create and set up your app / store listing](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en)

## 5. 업로드 키 · Play 앱 서명 · AAB

### 꼭 알아야 할 사실

- **AAB 필수**: 「2021년 8월부터 새 앱은 Android App Bundle 로 게시」. → `.\gradlew.bat bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- **Play App Signing 필수**: 「2021년 8월 이후 만든 앱은 모두 Play App Signing 을 써야」 한다. AAB 는 업로드 키로 서명해서 올리고, 사용자 기기에 깔리는 APK 는 Google 이 앱 서명 키로 다시 서명한다.
- 새 앱 설정: 첫 출시 때 「Google 이 앱 서명 키를 만든다」 를 고르면 따로 할 일이 없다. **첫 출시에 쓴 키가 업로드 키가 된다.** Play Console 도움말은 새 앱이 「Google 이 만든 키로 양자 대비 하이브리드 서명에 자동 등록」 된다고 쓴다.
- 업로드 키 조건: **RSA 2048비트 이상**, Java 키 저장소(`.jks` · `.keystore`). 유효 기간 **25년 이상** 권장, Play 는 **2033-10-22 이후 만료**를 요구.
- 앱 서명 키는 Google 이 보관하고 사본을 받을 수 없다. 같은 키를 다른 스토어에서도 쓰려면 첫 설정 때 자기 키를 올려야 한다 → Play 만 쓰므로 해당 없음.
- (참고) Android 개발자 인증: 「2026년부터 인증된 개발자가 등록한 앱만 인증 기기에 설치」. Play Console 개인 계정 인증과 같은 일인지는 **(확인 안 됨)** — Console 안내를 따른다.

### 업로드 키 만들기 (손으로, 둘 중 하나)

**A. Android Studio (추천)**

1. **Build > Generate Signed Bundle / APK** → **Android App Bundle** → Next.
2. Key store path 아래 **Create new**.
3. Key store path: **저장소 밖** 경로, 예: `C:\Users\<사용자>\keys\routine-calendar-upload.jks` (`.jks` 확장자).
4. Password: 강한 비밀번호. Key 의 Password 는 **키 저장소 비밀번호와 같게** 하라고 문서가 권한다(알려진 문제 때문).
5. Alias: 예 `upload`. Validity: **25년 이상**(Studio 기본값 확인). Certificate: 이름 · 「kikilog」 정도만(인증서에 들어가 APK 에 포함됨).
6. 키만 만들려면 OK 뒤 **Cancel**. 이어서 서명하려면 그대로 진행.

**B. keytool (명령줄)** — 공식 페이지에는 인증서 내보내기 명령만 있다. 키 생성 명령은 일반적인 형태다 **(추론, 아래 생성 명령 자체는 연 페이지에 없음)**.

```text
keytool -genkeypair -v -keystore C:\Users\<사용자>\keys\routine-calendar-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

- `keytool` 은 Android Studio 내장 JBR 의 `bin` 에 있다 **(추론)**. 처음 보는 프로그램이라 권한 확인 창이 뜬다 → 사용자가 직접 실행하는 편이 낫다.

### 키 보관

- 저장소 밖에 둔다(이 프로젝트 폴더 안에 두지 않는다). `.gitignore` 에 `*.jks` 가 있어도 실수 방지를 위해 밖에 둔다 **(추론)**.
- 비밀번호는 비밀번호 관리자에 저장. `.jks` 파일은 **두 곳 이상** 백업(예: 외장 드라이브 + 개인 클라우드 암호화 폴더) **(추론)**.
- 「강한 비밀번호 · 남에게 주지 않기 · 안전한 곳에 보관」(공식 문서).
- 이 문서 · DEVLOG · 커밋 메시지에 비밀번호 · 경로의 사용자 이름 · 지문 값을 적지 않는다(CLAUDE.md).

### 잃어버렸을 때 (업로드 키 재설정)

1. Android Studio 로 새 업로드 키를 만든다.
2. 인증서를 PEM 으로 내보낸다:

   ```text
   keytool -export -rfc -keystore <새 키 파일>.jks -alias upload -file upload_certificate.pem
   ```

3. Play Console → 앱 → Play 앱 서명 페이지(도움말 표기: Protected with Play > Play Store protection > Manage Play app signing) → 「Upload key certificate」 → **Request upload key reset** → 이유 입력 → PEM 올리기.
4. 재설정은 앱 서명 키에 영향이 없어 업데이트를 계속 낼 수 있다. 새 키를 언제부터 쓸 수 있는지(대기 기간)는 도움말에 적혀 있지 않았다 **(확인 안 됨)**.

### `keystore.properties` 서명 설정 (안 — 코드 단계)

- 공식 문서 방식: 프로젝트 루트에 `keystore.properties` → `build.gradle.kts` 가 읽어 `signingConfigs` 에 넣는다. `.gitignore` 에 이미 있다(확인함).
- 공식 예시는 파일이 없으면 `FileInputStream` 에서 빌드가 실패한다. 이 프로젝트는 `test assembleDebug` 를 늘 돌리므로 **파일이 있을 때만** 서명 설정을 붙인다 **(추론)**.

`keystore.properties` (저장소에 넣지 않음, 값은 자리표시):

```text
storeFile=C:/Users/<사용자>/keys/routine-calendar-upload.jks
storePassword=<키 저장소 비밀번호>
keyAlias=upload
keyPassword=<키 비밀번호>
```

- Windows 경로는 `/` 로 쓴다. `\` 는 properties 파일에서 이스케이프 문자다 **(추론)**.

`app/build.gradle.kts` (안):

```kotlin
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("upload") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            optimization { /* 아래 6절 */ }
        }
    }
}
```

- 버전 문자열이 없으므로 CLAUDE.md 규칙 5 와 맞다. 새 라이브러리 없음.
- 확인: `.\gradlew.bat signingReport` 로 release 변형이 업로드 키를 쓰는지 본다(지문 값은 기록하지 않는다).
- Configuration Cache(`org.gradle.configuration-cache=true`)와 함께 파일을 읽어도 되는지는 **(확인 안 됨)** — 빌드 경고가 나오면 그때 고친다.

출처: [Sign your app](https://developer.android.com/studio/publish/app-signing) · [Use Play App Signing (Play Console 도움말)](https://support.google.com/googleplay/android-developer/answer/7384423?hl=en) · [About Android App Bundles](https://developer.android.com/guide/app-bundle) · [Prepare your app for release](https://developer.android.com/studio/publish/preparing)

## 6. release 빌드 최적화(R8) 상태

- AGP 9.3 이상 새 DSL: `optimization { enable = true }` 하나로 **코드 · 리소스 최적화**를 켜고, 기본 Android keep 규칙(`proguard-android-optimize.txt` 상당)도 들어간다. keep 규칙은 `src/<variant>/keepRules/*.keep`. → 이 프로젝트는 이 구조를 이미 쓴다(`app/src/main/keepRules/rules.keep`).
- **`packageScope`** (지금 설정):
  - 「R8 을 아직 안 쓰는 앱이 **점진적으로** 들이기 위한 선택적 첫걸음」. AGP 9.0 의 **실험** 기능. R8 full mode 에서만 된다.
  - 문서 절차는 `gradle.properties` 에 **`android.r8.gradual.support=true`** 를 넣으라고 한다 → **이 프로젝트에 없다.** 없을 때 오류가 나는지, 무시되는지, AGP 9.5 에서 필요 없어졌는지는 **(확인 안 됨)** → `bundleRelease` 로 확인.
  - 지금 범위로는 **앱 코드(`com.kikilog.**`)가 최적화되지 않는다.** 문서는 안정되면 범위를 넓혀 결국 `packageScope` 를 지우고 앱 전체를 최적화하라고 한다.
- 추천 **(추론)**:
  1. 먼저 지금 설정 그대로 `bundleRelease` · release APK 를 만들어 휴대폰에서 확인한다.
  2. 그다음 `packageScope` 를 지워 앱 전체를 최적화해 본다. 반사(reflection)를 쓰는 곳 — Room 생성 코드 · `@Serializable` 백업 형식 · Glance `ActionCallback`(클래스 이름으로 찾음) · WorkManager `Worker` · Navigation 3 화면 키 — 는 각 라이브러리의 consumer 규칙이 대부분 처리한다고 보지만 **(확인 안 됨)**, 휴대폰에서 기능별로 직접 눌러 본다.
  3. 문제가 나면 `rules.keep` 에 좁은 규칙을 더하거나 `packageScope` 로 되돌린다.
- release 확인 항목: 앱 시작 · 루틴 등록/수정/삭제 · 달력 체크 · 위젯 체크(앱 → 위젯, 위젯 → 앱) · 알림 예약/도착 · 백업 내보내기/가져오기(합치기 · 덮어쓰기 · 되돌리기) · 분류 편집 · 빠른 선택 편집.
- release 는 기본으로 `debuggable` 이 꺼져 있다 **(추론 — 문서는 `isDebuggable = false` 예시만 보여 줌)**. 따로 적지 않아도 된다.
- 크래시 스택은 R8 로 이름이 바뀐다. Play Console 은 AAB 안의 매핑 파일로 되돌려 보여 준다 **(확인 안 됨 — 매핑 업로드 방식 미확인)**. 크래시 수집 SDK 는 쓰지 않는다(CLAUDE.md 규칙 3) — Play Console 의 「Android vitals」 는 기기 쪽 Google 기능이라 앱의 수집이 아니다 **(추론)**.

출처: [Enable app optimization (shrink-code)](https://developer.android.com/build/shrink-code) · [Optimize specified packages with packageScope](https://developer.android.com/topic/performance/app-optimization/optimize-specified-packages)

## 7. targetSdk 요구 사항

- **2026-08-31 부터**: 새 앱과 앱 업데이트는 **Android 16(API 36) 이상**을 타깃해야 제출된다(Wear OS · Automotive 35, TV · XR 34 예외). 연장 신청은 2026-11-01 까지였다.
- 기존 앱은 API 35 이상이어야 새 사용자에게 계속 보인다.
- **이 앱은 targetSdk 37 → 충족.** 다음 해(2027) 요구는 아직 발표를 보지 못했다 **(확인 안 됨)**. 보통 해마다 한 단계씩 오른다 **(추론)**.

출처: [Meet Google Play's target API level requirement](https://developer.android.com/google/play/requirements/target-sdk)

## 8. 첫 업로드 전 점검 목록 (앱 콘텐츠 외)

| 항목 | 기준 | 이 앱 |
|---|---|---|
| `applicationId` | 출시 뒤 못 바꿈 | `com.kikilog.routinecalendar` — 확정 |
| `versionCode` | 양의 정수, 올릴 때마다 커져야 함, 최대 2,100,000,000. **한 번 쓴 값은 다시 올릴 수 없음** | 1. 내부 · 비공개 테스트 트랙에 올릴 때도 매번 +1 해야 한다 **(추론)** |
| `versionName` | 사용자에게 보이는 유일한 버전 문자열 | 「1.0」. 첫 비공개 테스트를 「1.0.0」 으로 시작할지 열린 질문. 설정 화면 · 백업 파일(`appVersion`)에 이미 표시됨 |
| 디버그 로그 | release 전 `Log` 호출 · `Debug` 추적 호출 제거 | `app/src/main` 에 없음(확인함) |
| 권한 | 필요한 것만 | release 병합 Manifest(`processReleaseManifest`)에서 `INTERNET` · `AD_ID` · 정확한 알람 없음 다시 확인(privacy.md ⑨) |
| `android:icon` · `android:label` | 꼭 지정 | 있음. 새 아이콘 · 새 이름 반영 후 다시 확인 |
| 안 쓰는 파일 | `res/` 의 옛 drawable 등 정리 | Studio 기본 `ic_launcher_foreground/background.xml` 정리(icon.md 체크리스트), Lint UnusedResources |
| 서명 키 유효 기간 | 2033-10-22 이후 만료 | 25년 이상으로 생성 |
| release 실기기 시험 | 휴대폰 · 태블릿 크기 각 1대 권장 | S26 만. 태블릿은 없음 → 큰 화면은 Studio 미리보기 정도 **(추론)** |
| AAB 크기 | 압축 다운로드 4GB 이하 | 해당 없음 |
| 앱 안 방침 링크 | privacy.md 8절 | 2026-11-04 설정 화면에 「개인정보처리방침」(브라우저) · 「의견 보내기」(메일 앱) 넣음 |
| 비공개 테스트 | 개인 계정: 테스터 12명 이상 · 14일 연속 | PLAN 10단계 |

출처: [Version your app](https://developer.android.com/studio/publish/versioning) · [Prepare your app for release](https://developer.android.com/studio/publish/preparing) · [About Android App Bundles](https://developer.android.com/guide/app-bundle)

---

## 스토어 문구 초안 (한국어)

> `<앱 이름>` 은 자리표시. 글자 수는 손으로 센 대략값이라 Console 입력 칸의 글자 수 표시로 다시 확인한다.

### 간단한 설명 (80자 이하)

```text
이불 세탁은 2개월, 팩은 1주마다. 한 번 등록하면 달력에서 할 날과 한 날을 함께 봐요.
```

대안:

```text
자취 집안일 · 뷰티 · 소모품 주기를 한 번 등록하고, 월 달력에서 예정일과 완료를 확인하세요.
```

### 자세한 설명 (4,000자 이하)

```text
「이불 세탁 언제 했더라?」 「화장지 살 때 됐나?」
<앱 이름>은 자취 생활의 주기 루틴을 한 번 등록해 두면, 다음에 할 날을 계산해 월 달력에 보여 주는 앱이에요.

■ 이렇게 써요
- 루틴을 등록해요. 예: 청소기 1주 · 화장실 청소 2주 · 이불 세탁 2개월 · 모델링팩 1주 · 각질제거 10일 · 칫솔 교체 3개월
- 했으면 체크해요. 다음 예정일은 실제로 한 날부터 다시 계산돼요. 이틀 늦게 했으면 다음 예정도 그만큼 뒤로 가요.
- 반복하지 않는 일도 등록할 수 있어요. 예: 「화장지 2개월 뒤 한 번」

■ 달력
- 한 달 달력에 예정일은 분류 색 점으로, 한 날은 체크로, 놓친 날은 빨간 테두리로 보여요.
- 날짜를 누르면 그날 할 루틴 목록이 나와요. 지난 날짜에서도 체크할 수 있어요.
- 옆으로 밀어 달을 넘겨요. 주 시작 요일(일요일 · 월요일)을 고를 수 있어요.

■ 루틴과 분류
- 집안일 · 뷰티 · 구매 · 기타 분류가 기본으로 있고, 분류 이름과 색을 바꾸거나 새로 만들 수 있어요.
- 자주 쓰는 루틴은 빠른 선택에서 바로 고르고, 빠른 선택 목록도 직접 고칠 수 있어요.
- 루틴마다 다음 예정일, 마지막으로 한 날, 앞으로 예정일 3개를 볼 수 있어요.
- 안 쓰는 루틴은 보관해 두면 기록은 남기고 목록에서 숨겨져요.

■ 알림 (선택)
- 하루 한 번, 정한 시각에 「오늘 할 루틴」을 모아 알려 줘요. 시각은 30분 단위로 고를 수 있어요.
- 할 루틴이 없는 날은 알림을 보내지 않아요.
- 알림을 허용하지 않아도 다른 기능은 모두 쓸 수 있어요.

■ 홈 화면 위젯
- 오늘 할 루틴과 지난 루틴을 홈 화면에서 보고 바로 체크해요.

■ 백업
- 설정에서 내 기록을 파일(JSON)로 내보내고 다시 가져올 수 있어요. 저장 위치는 직접 골라요.
- 가져올 때 지금 기록과 합치거나, 파일 내용으로 바꿀 수 있어요. 가져오기 전 상태로 되돌릴 수도 있어요.
- 휴대폰의 Google 백업이 켜져 있으면, 암호화가 가능한 경우에만 내 Google 계정에 백업돼요.

■ 개인정보
- 모든 기록은 이 휴대폰에만 저장돼요.
- 앱에 인터넷 연결 권한이 없어요. 아무 정보도 수집하거나 공유하지 않아요.
- 회원가입 · 로그인 · 광고가 없어요.

■ 이런 앱은 아니에요
- 시간 단위 알람, 할 일 목록, 습관 통계 앱이 아니에요. 「며칠 · 몇 주 · 몇 달마다」 돌아오는 일에 맞춰 만들었어요.

문의: 스토어 등록정보의 개발자 이메일로 보내 주세요.
```

- 사실 확인: 기능 목록은 PLAN 1절 화면 표 · 3~7단계 결과와 맞췄다. 「오늘」 탭은 숨겨져 있으므로 넣지 않았다. 「할 루틴이 없는 날은 알림 없음」 은 5단계 결정(0개인 날 보내지 않음).
- 「놓친 날 빨간 테두리」 · 「분류 색 점」 은 PLAN 1절 표 기준. 8단계 다듬기에서 바뀌면 고친다.
- 자동 백업 문장은 privacy.md 방침 초안 4절과 같은 뜻이다. Android 8.x 에서는 클라우드 백업이 안 될 수 있다(privacy.md 1절) — 설명에는 적지 않았다.
- 비교 · 최상급 · 후기 · 「무료」 표현 없음.

---

## 구현 순서 체크리스트

**사용자가 손으로 할 단계**

- [ ] ① 앱 이름 고르기(1절 후보) — 런처 이름(짧게)과 스토어 제목(부제 포함)을 같이 정함
- [ ] ② 고른 이름을 Play 앱 검색 · KIPRIS 에서 한 번 더 검색
- [ ] ③ 업로드 키 만들기(5절 A): 저장소 밖 경로, RSA 2048, 25년 이상, 비밀번호는 비밀번호 관리자에. `.jks` 두 곳 백업
- [ ] ④ 프로젝트 루트에 `keystore.properties` 만들기(5절 자리표시 채움). `git status` 에 안 보이는지 확인
- [ ] ⑤ Play Console 앱 만들기: 기본 언어 **한국어** · 앱(게임 아님) · 무료 · 이름
- [ ] ⑥ 스토어 설정: 카테고리(라이프스타일 추천) · 태그 최대 5개 · 연락처(이메일 필수, 웹사이트 = 방침 주소, 전화 비움)
- [ ] ⑦ 시연 데이터로 스크린샷 4~6장 찍기(3절: 내보내기 → 시연 JSON 덮어쓰기 → 촬영 → 되돌리기)
- [ ] ⑧ 스크린샷을 1080×1920(또는 1080×2160)으로 자르고 JPEG/알파 없는 PNG 로 저장
- [ ] ⑨ 그래픽 이미지 1024×500(알파 없음) 만들기, Play 아이콘 512×512 확인(벡터 아이콘 완성 뒤)
- [ ] ⑩ 등록정보 문구 붙여 넣기(초안의 `<앱 이름>` 채움), 글자 수 표시 확인
- [ ] ⑪ 첫 AAB 를 내부 테스트 트랙에 올리기 → Play 앱 서명 「Google 이 생성」 그대로 → 서명 페이지에서 업로드 인증서 등록 확인

**코드로 할 단계 (Claude)**

- [x] ⑫ `app_name` 등 이름 문자열 바꾸기(이름 확정 뒤, 위젯 설명 · 알림 채널 이름 등 함께 검색) — 2026-11-04 「때됐냥」
- [x] ⑬ `app/build.gradle.kts` 에 `keystore.properties` 읽기 + `signingConfigs` (파일 없으면 건너뜀). 새 라이브러리 · 버전 문자열 없음 — 2026-11-04
- [x] ⑭ `bundleRelease` 시험: `packageScope` + `android.r8.gradual.support` 여부 확인, 오류 · 경고 기록 — 2026-11-04 키 없이 통과(약 5분). `android.r8.gradual.support` 없이도 R8 오류 · 경고 없음(AGP 9.5). 나온 것: Kotlin 경고 1건(`EditRoutineScreen.kt` when 의 null 비교), 네이티브 라이브러리 2개 strip 불가 안내(그대로 포장)
- [ ] ⑮ release APK 를 S26 에 설치해 6절 확인 항목 전부 눌러 보기(디버그 앱과 서명이 달라 먼저 디버그 앱 삭제가 필요할 수 있음 → **데이터가 지워지므로 JSON 내보내기 먼저**) **(추론)**
- [ ] ⑯ (안정되면) `packageScope` 지우고 앱 전체 최적화 → ⑮ 다시
- [x] ⑰ release 병합 Manifest 권한 다시 확인(privacy.md ⑨) — 2026-11-04 `POST_NOTIFICATIONS` · `WAKE_LOCK` · `ACCESS_NETWORK_STATE` · `RECEIVE_BOOT_COMPLETED` · `FOREGROUND_SERVICE` · 앱 전용 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 뿐. `INTERNET` · 정확한 알람 · `AD_ID` 없음
- [ ] ⑱ 시연용 JSON 만들기(저장소에 둘지 열린 질문)
- [ ] ⑲ `versionCode` · `versionName` 확정(첫 업로드 1 / 「1.0.0」 등)
- [ ] ⑳ `.\gradlew.bat test assembleDebug` → 커밋 · DEVLOG(키 경로 · 비밀번호 · 지문 적지 않음) → PLAN 9단계 체크 갱신

## 정한 것 (2026-11-04 사용자 답)

- 컨셉: 귀여운 캐릭터 앱. 앱 이름 「때됐냥」(홈 화면 이름). 스토어 제목은 「때됐냥 - 자취 루틴 달력」을 기본으로 9단계에서 확정
- 아이콘: 삼색 고양이 귀가 달린 크림색 달력(벡터, 흰 뒤 층)
- 연락 이메일: privacy.md 「정한 것」 참고

## 열린 질문

1. 앱 이름 — 「때됐냥 - 자취 루틴 달력」 · 「루틴 달력」 · 「자취 루틴」 · 「챙김 달력」 중 무엇으로? 런처 이름은 짧은 앞부분만 쓸지?
2. 경쟁 앱 「살림주기」 · 「살림루틴」 을 직접 깔아 보고 다른 점을 설명에 더 드러낼지?
3. 카테고리 — 라이프스타일(추천) · 생산성 · 집/홈 중 무엇으로?
4. 스토어 등록정보를 한국어만 둘지, 영어도 넣을지(앱 화면은 한국어뿐)?
5. 스크린샷에 짧은 설명 글(태그라인)을 넣을지, 화면만 둘지? 다크 모드 화면을 1장 섞을지?
6. 시연용 JSON 을 저장소(`docs/` 아래 등)에 둘지, 휴대폰에만 둘지?
7. 업로드 키 · 비밀번호를 어디에 백업할지(외장 드라이브 · 비밀번호 관리자 등)?
8. release 를 S26 에서 시험할 때 디버그 앱을 지워야 하면 그 시점(지우기 전 JSON 내보내기 필수)?
9. R8 을 `packageScope` 로 둘지, 앱 전체 최적화까지 갈지?
10. 첫 `versionName` 을 「1.0」 그대로 둘지, 「1.0.0」 으로 할지?
