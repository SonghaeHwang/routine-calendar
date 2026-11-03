# 8단계 앱 아이콘 사전 조사

> 조사일: 2026-11-03 · 대상: PLAN 8단계(앱 아이콘) · 근거는 각 절 끝 출처(실제로 연 페이지만)
> 표시: **(확인 안 됨)** = 공식 문서나 실물에서 직접 확인하지 못한 내용 · **(추론)** = 문서 문장을 바탕으로 한 판단

## 결론 요약 (추천)

1. **적응형 아이콘 구조는 그대로 둔다.** `mipmap-anydpi/ic_launcher.xml` 의 `<adaptive-icon>` 에 앞(foreground) · 뒤(background) · 단색(monochrome) 세 층. minSdk 26 이라 모든 기기가 적응형 아이콘을 쓴다. `mipmap-anydpi` 는 `-v26` 을 붙이지 않아도 된다.
2. **앞 층 = 오려 낸 고양이(배경 투명), 뒤 층 = 단색(크림 · 연한 파스텔).** 지금 사진은 흰 배경이라 그대로 앞 층에 넣으면 흰 네모가 생긴다. 고양이만 오려 투명 PNG 로 만들고, 뒤 층은 색 하나(`values` 의 color)로 둔다.
3. **고양이는 108dp 캔버스 가운데 지름 66dp 원 안에 넣는다.** 귀 끝까지 원 안에 들어가야 One UI 등 어떤 모양 마스크에서도 안 잘린다. 가로가 긴 그림(241×199)이라 가로 약 56~60dp 로 줄여 맞춘다 **(추론)**.
4. **원본 해상도가 부족하다.** 원본은 **241×199 px**. xxxhdpi 앞 층은 432×432 px 이고 그 안의 66dp 원은 264 px 이라 살짝 늘려야 한다. Play 스토어 512×512 아이콘은 고양이가 380 px 안팎이 돼 1.5배 넘게 늘어나 흐려진다 **(추론)**. **가로 1024 px 이상 원본**을 구하거나, 벡터로 다시 그리는 것을 추천한다.
5. **단색 층은 사진으로 만들지 않는다.** 단색 층은 모양(투명도)만 쓰고 색은 시스템이 칠한다 **(추론)**. 사진을 넣으면 고양이 머리 덩어리 하나가 된다. 귀가 있는 고양이 머리 실루엣을 **벡터 XML 로 따로** 그린다(코드로 가능, 새 라이브러리 없음).
6. **Image Asset Studio 로 만든다.** 앞 층 = Image(오려 낸 PNG), 뒤 층 = Color, 단색 층 = 따로 그린 실루엣. Options 에서 Play Store 아이콘(512×512)도 함께 만든다.
7. **알림 작은 아이콘(`ic_today`)은 바꾸지 않는다.** 상태 표시줄 아이콘은 단색으로 그려지므로 사진은 못 쓴다. 지금의 달력 모양 단색 벡터가 규칙에 맞는다.
8. **출시 전에 사진 권리를 확인한다.** 사용자가 직접 만들거나 찍은 사진이 아니면 Play 지식재산권 정책 위반이 될 수 있다. 확인이 안 되면 같은 느낌으로 직접 그린 그림(벡터)을 쓴다.

---

## 0. 지금 상태 (프로젝트에서 읽은 것)

| 파일 | 내용 |
|---|---|
| `AndroidManifest.xml` | `android:icon="@mipmap/ic_launcher"` · `android:roundIcon="@mipmap/ic_launcher_round"`. 위젯 receiver 는 `android:icon="@drawable/ic_widget_cat"` |
| `mipmap-anydpi/ic_launcher.xml` · `ic_launcher_round.xml` | 두 파일 내용이 같다. `<background @drawable/ic_launcher_background>` · `<foreground @drawable/ic_launcher_foreground>` · `<monochrome @drawable/ic_launcher_foreground>` (앞 층을 단색 층으로 재사용) |
| `drawable/ic_launcher_foreground.xml` | 108×108dp 벡터. 흰색 Android 로봇 머리 + 그림자 그라데이션 (Studio 기본) |
| `drawable/ic_launcher_background.xml` | 108×108dp 벡터. 초록(`#3DDC84`) 바탕 + 격자선 (Studio 기본) |
| `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.webp` · `ic_launcher_round.webp` | 래스터 아이콘 10개(약 1~8KB). 적응형 아이콘이 없는 옛 기기용 |
| `drawable-nodpi/ic_widget_cat.png` | **241×199 px**, 8비트 RGBA PNG, 84KB. 흰 바탕 위 삼색 털실 고양이 얼굴, 아래에 옅은 그림자. 흰 바탕이 실제로 불투명인지 투명인지는 **(확인 안 됨)** — Asset Studio 미리보기에서 확인 |
| `drawable/ic_today.xml` | 24dp 단색 벡터(달력). 알림 작은 아이콘 |

- `anydpi` 는 「모든 밀도에 맞고 다른 한정자보다 우선한다」. minSdk 26 이면 모든 기기가 `mipmap-anydpi` 의 적응형 XML 을 고르므로 `mipmap-*dpi/*.webp` 는 실제로 쓰이지 않는다 **(추론)**. 그래도 Asset Studio 가 다시 만들어 주므로 그대로 둔다.
- `minSdk 26` 이면 `-v26` 한정자는 의미가 없다. 공식 문서 예시는 `mipmap-anydpi-v26` 이지만, 지금처럼 `mipmap-anydpi` 하나로 충분하다 **(추론)**. 둘이 함께 생기면 하나만 남긴다.

출처: [App resources overview](https://developer.android.com/guide/topics/resources/providing-resources)

## 1. 적응형 아이콘 규격

- 층: 색 아이콘은 **앞 · 뒤 두 층**이 필수. 벡터 · 비트맵 모두 되지만 「벡터를 권장」.
- 크기: **모든 층 108×108dp**. 바깥 사방 18dp 는 마스크와 시각 효과(시차 · 맥동)용.
- 안전 영역: 가운데 **66×66dp** 는 어떤 제조사 마스크에서도 잘리지 않는다. 로고는 **48dp 이상, 66dp 이하**.
- 모양: 원 · 둥근 사각형(squircle) 등은 제조사가 정한 마스크로 잘린다. 층에 마스크나 외곽 그림자를 넣지 않는다(「clean edges」).
- `roundIcon`: 선택 사항. 원형 아이콘을 쓰는 런처가 쓰며 「대부분의 앱은 `android:icon` 만 지정」. 지금처럼 두 XML 을 같은 내용으로 두면 문제없다.
- 단색 층: 테마 아이콘을 지원하려면 단색 층 **하나**를 준다(아래 2절).

래스터(PNG/WebP) 앞 층을 쓸 때 밀도별 크기 (108dp × 배율, 비율 3:4:6:8:12:16):

| 밀도 | 배율 | 층 전체(108dp) | 안전 원(66dp) | 로고 최소(48dp) |
|---|---|---|---|---|
| mdpi | 1x | 108 px | 66 px | 48 px |
| hdpi | 1.5x | 162 px | 99 px | 72 px |
| xhdpi | 2x | 216 px | 132 px | 96 px |
| xxhdpi | 3x | 324 px | 198 px | 144 px |
| xxxhdpi | 4x | 432 px | 264 px | 192 px |

- 런처 아이콘은 `mipmap` 에 둔다. 런처가 밀도보다 최대 25% 크게 그릴 수 있어서, 한 단계 높은 해상도를 쓰도록 `mipmap` 은 모든 밀도가 APK 에 남는다.

출처: [Adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive) · [Support different pixel densities](https://developer.android.com/training/multiscreen/screendensities)

## 2. 단색(테마) 아이콘 `<monochrome>`

- Android 13(API 33)부터 사용자가 「테마 아이콘」을 켜고 런처가 지원하면, 배경화면 · 테마 색으로 **단색 층을 칠해** 보여 준다.
- 단색 층이 없으면: 테마 아이콘을 켜도 **원래 색 아이콘**이 그대로 보인다(Android 16 QPR2 이전). **Android 16 QPR2 부터는 단색 층이 없는 앱도 시스템이 자동으로 테마를 입힌다.** 자동 결과가 어떤 모양인지는 **(확인 안 됨)**.
- 사진 같은 그림: 단색 층은 색을 버리고 모양(알파)만 쓰는 방식이라 **(추론)**, 털 무늬 · 그림자는 사라지고 윤곽만 남는다. 흰 바탕이 불투명하면 네모 덩어리가 된다. 그래서
  - 귀 두 개가 있는 고양이 머리 **실루엣**(속은 채우고, 얼룩 무늬 일부를 구멍으로 뚫어 삼색 느낌)을 벡터 XML 로 따로 그린다 **(추론)**.
  - 크기 규칙은 앞 층과 같다(108dp 캔버스, 66dp 안).
  - 지금 XML 처럼 앞 층을 단색 층으로 재사용하면 안 된다. Asset Studio 의 「Monochrome Layer」 기본값도 앞 층 재사용이므로 직접 바꿔야 한다.
- 미리보기: Studio 에서 `ic_launcher.xml` 을 열고 도구 막대 **System UI Mode** 로 배경화면을 바꿔 확인한다.
- One UI(Galaxy S26, Android 17)에서 테마 아이콘이 서드파티 앱의 `<monochrome>` 을 쓰는지, 자동 테마를 쓰는지 **(확인 안 됨)** → 휴대폰에서 직접 확인.

출처: [Adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive) · [Create app icons](https://developer.android.com/studio/write/create-app-icons)

## 3. 사진을 아이콘으로 쓸 때 문제와 처리

- **작은 크기**: 홈 화면 아이콘은 실제로 48dp 남짓 크기다. 털실 질감 · 작은 그림자는 거의 안 보이고, 주황 · 갈색 얼룩과 귀 모양이 알아보는 단서가 된다 **(추론)**. 공식 문서도 벡터는 「사진이 아닌 아이콘 같은 그림에 알맞다」고 한다.
- **흰 배경**:
  - 고양이만 오려 **투명 PNG** 로 만든다(사용자가 그림 도구로 직접, 아래 체크리스트). 아래 바닥 그림자도 지운다(층에 그림자를 넣지 말라는 규칙).
  - 뒤 층은 색 하나. 흰색이면 고양이 흰 털과 경계가 없어진다. **연한 크림 · 살구 · 민트 같은 파스텔**을 추천 **(추론)**. 다크 모드에서도 뒤 층 색은 그대로 보인다.
  - 오리기가 어렵다면 차선책: 사진 그대로를 앞 층에, 뒤 층을 흰색으로 두면 흰 바탕이 이어져 보인다 **(추론)**. 대신 Play 아이콘이 Play 의 흰 화면과 섞인다.
- **가로가 긴 그림(241×199, 약 1.2:1)**: 지름 66dp 원 안에 귀 끝 · 볼 양 끝이 모두 들어가야 한다. 얼굴 아래쪽은 둥글어 네모 모서리가 비므로, 가로 약 56~60dp · 세로 약 46~50dp 로 놓고 원형 미리보기에서 귀가 안 잘리는지 본다 **(추론)**. 눈으로 보면 세로 가운데보다 조금 아래(귀 때문에)로 내리는 것이 균형이 맞다 **(추론)**.
- **해상도**:
  - xxhdpi(3x)에서 66dp 원은 198 px → 원본(241 px)을 줄이므로 괜찮다.
  - xxxhdpi(4x)에서는 264 px → 약 1.1배 늘린다. 런처가 25% 더 크게 그리면 더 흐릴 수 있다.
  - Play 512×512 에서는 고양이가 약 380~410 px → **1.6배 이상** 늘어나 흐려진다 **(추론)**.
  - 대안 ① 같은 사진의 **큰 원본(가로 1024 px 이상)** 을 구한다. ② **벡터로 다시 그린다**(머리 윤곽 + 얼룩 3~4개 + 귀). 크기 문제 · 단색 층 · 알림 아이콘이 한 번에 풀리고 저작권 문제도 줄어든다. 털실 질감은 잃는다.

출처: [Support different pixel densities](https://developer.android.com/training/multiscreen/screendensities) · [Adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive)

## 4. Image Asset Studio 로 만드는 순서 (손으로)

1. Project 창을 **Android** 보기로 → `res` 오른쪽 클릭 → **New > Image Asset**.
2. **Icon Type**: `Launcher Icons (Adaptive and Legacy)`. Name: `ic_launcher` 그대로(같은 이름은 덮어씀).
3. **Foreground Layer**: Asset Type `Image` → 오려 낸 투명 PNG. **Trim = Yes**(투명 여백 제거, 비율 유지) → **Resize** 슬라이더로 고양이가 안전 원(미리보기의 원) 안에 들어오게 줄인다.
4. **Background Layer**: Asset Type `Color` → 파스텔 색 하나.
5. **Monochrome Layer**: 기본은 앞 층 재사용이니 바꾼다. 실루엣 PNG 를 `Image` 로 지정하거나, 비워 두고 나중에 벡터 XML 로 바꾼다(코드 단계).
6. **Options**: legacy · round · **Google Play Store** 아이콘 생성 확인.
7. 미리보기(원 · squircle · 둥근 사각 · 사각 · full bleed)에서 귀가 안 잘리는지 확인 → **Next**.
8. Res Directory 는 `src/main/res` → **Finish**.

생기는 파일(이름 · 위치는 Studio 버전에 따라 다를 수 있음 **(확인 안 됨)**):

- `mipmap-anydpi/ic_launcher.xml` · `ic_launcher_round.xml` (또는 `mipmap-anydpi-v26/`)
- `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.*` (앞 층 비트맵, 밀도별)
- `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.*` · `ic_launcher_round.*` (옛 기기용 합친 그림)
- 단색 층을 Image 로 줬으면 `mipmap-*dpi/ic_launcher_monochrome.*`
- 뒤 층을 Color 로 줬으면 `values/ic_launcher_background.xml` (color 리소스)
- `src/main/ic_launcher-playstore.png` (512×512, Play 업로드용)

생성 뒤 정리 **(추론)**:

- 예전 `drawable/ic_launcher_foreground.xml` · `drawable/ic_launcher_background.xml` 가 더 이상 안 쓰이면 지운다(이름은 같아도 종류가 drawable · color 로 달라 충돌은 없음). Lint 의 UnusedResources 로 확인.
- `ic_launcher.xml` 의 `<monochrome>` 이 실루엣을 가리키는지 확인.

출처: [Create app icons (Image Asset Studio)](https://developer.android.com/studio/write/create-app-icons)

## 5. Google Play 스토어 아이콘

- 크기 **512×512 px**, **32비트 PNG**, 색 공간 **sRGB**, 최대 **1024KB**.
- 모양: **꽉 찬 정사각형**. 둥근 모서리(크기의 30%)와 그림자는 Play 가 입힌다 → 직접 둥글리거나 그림자를 넣지 않는다. 그림 안의 명암은 괜찮다.
- 배경: 투명하지 않은 색을 고른다(투명이면 Play 화면 색이 비친다). 흰색이면 Play 흰 화면과 섞인다 → 런처와 같은 파스텔 배경 추천 **(추론)**.
- 배치: 일러스트는 꽉 채워도 되고, 로고는 키라인 격자에 맞춘다. 고양이 얼굴은 키라인 안에 두는 쪽이 런처 아이콘과 같아 보인다 **(추론)**.
- 금지: 순위 · 할인 · Play 프로그램 참여 · 사용자를 속이는 글자나 그림.
- 런처 아이콘(APK)과는 **별개 파일**. Play Console 에 따로 올린다(9단계).

출처: [Google Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)

## 6. 알림 작은 아이콘

- 알림의 앱 아이콘은 「상태 표시줄에서 **단색**으로 보인다」. 사진 · 여러 색 그림은 덩어리로 보인다.
- Android 12 부터 아이콘 색은 알림 색(없으면 시스템 테마 색)으로 칠해진다.
- 알림 종류가 여럿이면 앱 아이콘 대신 목적을 나타내는 기호를 쓰라고 한다. 우리는 「오늘 할 루틴」 요약 하나라 달력 기호(`ic_today`, 24dp · 단색 벡터)가 알맞다 → **바꿀 필요 없음**.
- 원하면 3절의 고양이 실루엣 벡터를 알림 아이콘으로도 쓸 수 있다(열린 질문 4).

출처: [Notifications (디자인 가이드)](https://developer.android.com/design/ui/mobile/guides/home-screen/notifications) · [Create a notification](https://developer.android.com/develop/ui/views/notifications/build-notification)

## 7. 저작권 (Play 지식재산권 정책)

- Play 는 남의 지식재산권을 침해하는 앱 · 스토어 등록정보를 허용하지 않는다. 아이콘도 포함.
- 「직접 만든 독창적 콘텐츠만 쓰라」, 남의 것을 쓰면 **서면 허락 · 라이선스**를 받아 두고, Play 가 **권리 증거를 요구할 수 있다**. 「고쳐 써도 위반일 수 있다」.
- 확인할 것:
  - 사진을 **누가 찍었나**(사용자 본인인가).
  - 펠트 고양이 **인형을 누가 만들었나**. 남이 만든 상품이면 사진을 직접 찍어도 인형 디자인 권리 문제가 남을 수 있다 **(추론)**.
  - 흰 배경 · 바닥 그림자가 상품 사진처럼 보인다 **(추론)**. 인터넷 · 쇼핑몰에서 받은 사진이면 출시용으로 쓰지 않는다.
- 권리가 불분명하면: 직접 그린 벡터 고양이(3절 대안 ②)로 바꾼다. 위젯 선택기 아이콘(`ic_widget_cat.png`)도 같은 그림이므로 함께 바꾼다.

출처: [Google Play 지식재산권 정책](https://support.google.com/googleplay/android-developer/answer/9888072)

## 구현 순서 체크리스트

사용자(손으로):

- [ ] 사진 권리 확인(직접 찍은 사진 · 직접 만든 인형인지). 아니면 벡터로 다시 그리기로 결정
- [ ] 가능하면 같은 사진의 큰 원본(가로 1024 px 이상) 준비
- [ ] 그림 도구로 고양이만 오려 투명 PNG 만들기(바닥 그림자 제거)
- [ ] 뒤 층 색(파스텔) 정하기
- [ ] Image Asset Studio 실행(4절 1~8): 앞 = Image, 뒤 = Color, Options 에서 Play 아이콘 생성
- [ ] 미리보기 다섯 모양에서 귀가 안 잘리는지 확인

코드(Claude):

- [ ] 고양이 머리 실루엣 벡터 `drawable/ic_launcher_monochrome.xml` 작성(108dp, 66dp 안)
- [ ] `mipmap-anydpi/ic_launcher.xml` · `ic_launcher_round.xml` 의 `<monochrome>` 을 실루엣으로 바꾸기
- [ ] `mipmap-anydpi` 와 `-v26` 이 둘 다 생겼으면 하나만 남기기
- [ ] 안 쓰는 예전 `drawable/ic_launcher_foreground.xml` · `ic_launcher_background.xml` 정리
- [ ] `ic_launcher-playstore.png` 위치 정하기(9단계 스토어 자료와 함께, 512×512 · 1MB 이하 확인)
- [ ] `./gradlew test assembleDebug` 통과 → 커밋

휴대폰(S26):

- [ ] `installDebug` 후 홈 화면 · 앱 서랍 · 최근 앱 · 설정 > 앱 목록에서 확인
- [ ] One UI 테마 아이콘을 켜고 단색 층이 어떻게 보이는지 확인
- [ ] 다크 모드에서 아이콘 확인

## 열린 질문

1. 사진은 사용자가 직접 찍었고, 인형도 직접 만들었거나 권리가 있나? (아니면 출시용으로 쓸 수 없음)
2. 털실 사진 느낌을 지킬까(큰 원본 + 오려내기), 아니면 벡터로 다시 그릴까?
3. 뒤 층 색은 무엇으로? (크림 · 살구 · 민트 등 파스텔 / 흰색)
4. 알림 작은 아이콘을 달력 기호(`ic_today`) 그대로 둘까, 고양이 실루엣으로 바꿀까?
5. 위젯 선택기 아이콘(`ic_widget_cat.png`)을 새 런처 아이콘(`@mipmap/ic_launcher`)으로 바꿀까?
6. 앱 이름(지금 임시 「Routine Calendar」)을 아이콘과 함께 정할까? (PLAN 「아직 정하지 않은 것」)
