# QR-Reader (안드로이드 QR / 1D / 2D 바코드 리더기)

> **Google ML Kit**와 **Android Jetpack CameraX**를 기반으로 개발된 고성능 올인원 바코드 스캐너 안드로이드 애플리케이션입니다.

---

## 📌 목차
1. [프로젝트 소개](#-프로젝트-소개)
2. [주요 기능](#-주요-기능)
3. [지원 바코드 규격](#-지원-바코드-규격)
4. [기술 스택 및 아키텍처](#-기술-스택-및-아키텍처)
5. [프로젝트 폴더 구조](#-프로젝트-폴더-구조)
6. [핵심 구현 상세](#-핵심-구현-상세)
7. [빌드 및 실행 방법](#-빌드-및-실행-방법)
8. [개발자 가이드 및 커스터마이징](#-개발자-가이드-및-커스터마이징)

---

## 📱 프로젝트 소개
본 프로젝트는 안드로이드 스마트폰 카메라를 통해 **QR 코드**, **2D 매트릭스 바코드**, **1D 선형 바코드**를 실시간으로 초고속 인식하고, 스캔 성공 시 경쾌한 **"삐" 비프음(Beep Sound)** 과 함께 바코드의 **형식(Format)** 및 **내용(Content)** 을 즉각 제공합니다. 또한 원클릭 **클립보드 복사** 기능을 탑재하여 스캔 결과를 메모장이나 타 앱에 손쉽게 활용할 수 있습니다.

---

## ✨ 주요 기능
- **상단 타이틀바 (`QR-Reader`)**: 깔끔하고 직관적인 헤더 UI 및 플래시(손전등) 토글 버튼 제공.
- **초고속 온디바이스 스캐닝**: 인터넷 연결 없이 기기 내부 머신러닝 엔진(ML Kit)으로 딜레이 없는 즉각 스캔.
- **오디오 & 햅틱 피드백**:
  - 바코드 인식 성공 시 명확한 비프음 `"삐"` 소리 출력 (`ToneGenerator` 활용)
  - 부드러운 햅틱 진동 피드백 동시 전달
- **결과 표시 (하단 카드)**:
  - **바코드 형식**: QR 코드, Data Matrix, Code 128, EAN-13 등 읽기 쉬운 한글/영문 포맷 뱃지 표시.
  - **바코드 내용**: 긴 텍스트나 URL도 편안하게 확인할 수 있는 스크롤 가능한 텍스트 뷰.
- **복사하기 기능**: [복사하기] 버튼 탭 한 번으로 스캔된 텍스트가 안드로이드 클립보드로 복사되며 안내 토스트 출력.
- **다시 스캔 모드**: 중복 스캔 방지 로직 적용 및 [다시 스캔] 버튼을 통한 재인식 트리거.
- **한글 및 다국어 완벽 지원**: UTF-8 바이트 인코딩 복원 로직을 내장하여 한글 깨짐 방지.
- **카메라 권한 대응**: Android 6.0+ 런타임 권한(CAMERA) 처리 및 미허용 시 친절한 안내 화면 제공.

---

## 🎯 지원 바코드 규격

| 구분 | 바코드 형식 | 규격 코드명 (ML Kit) | 설명 |
| :--- | :--- | :--- | :--- |
| **QR** | QR Code | `FORMAT_QR_CODE` | 표준 QR 코드, Micro QR |
| **2D** | Data Matrix | `FORMAT_DATA_MATRIX` | 부품, 물류, 의약품용 2차원 매트릭스 |
| **2D** | Aztec | `FORMAT_AZTEC` | 항공권/철도 티켓용 2차원 바코드 |
| **2D** | PDF417 | `FORMAT_PDF417` | 신분증, 운전면허증, 항공 탑승권 |
| **1D** | Code 128 | `FORMAT_CODE_128` | 물류, 배송, 영수증 표준 바코드 (ASCII 전 문자 지원) |
| **1D** | Code 39 | `FORMAT_CODE_39` | 산업용 및 군용 바코드 |
| **1D** | Code 93 | `FORMAT_CODE_93` | 고밀도 영숫자 바코드 |
| **1D** | Codabar | `FORMAT_CODABAR` | 도서관, 혈액원, 사진관 |
| **1D** | EAN-13 | `FORMAT_EAN_13` | 국내 및 국제 표준 유통 상품 바코드 (13자리) |
| **1D** | EAN-8 | `FORMAT_EAN_8` | 소형 유통 상품 바코드 (8자리) |
| **1D** | UPC-A | `FORMAT_UPC_A` | 북미 표준 유통 상품 바코드 (12자리) |
| **1D** | UPC-E | `FORMAT_UPC_E` | 북미 소형 상품 바코드 |
| **1D** | ITF | `FORMAT_ITF` | 물류 유통 골판지 박스 포장 바코드 |

---

## 🛠 기술 스택 및 아키텍처

- **Language**: Kotlin 1.9.22
- **Min SDK**: API 24 (Android 7.0 Nougat 이상 지원)
- **Target / Compile SDK**: API 34 (Android 14)
- **Camera Stack**: **Android Jetpack CameraX 1.3.2**
  - 생명주기(`LifecycleOwner`)와 완전히 결합되어 메모리 누수 및 리소스 낭비 없음.
  - `ImageAnalysis`를 통해 카메라 프리뷰 프레임을 백그라운드 스레드에서 무중단 실시간 분석.
- **Vision Engine**: **Google ML Kit Barcode Scanning 17.2.0**
  - 온디바이스(On-Device) 오프라인 구동 모델
  - 하드웨어 가속 처리로 초당 수십 프레임 분석
- **UI Framework**: Material Design 3 Components, ConstraintLayout, ViewBinding
- **Audio Feedback**: `android.media.ToneGenerator` (시스템 리소스 최소화, 경쾌한 비프음 즉시 발생)
- **Encoding**: UTF-8 (문자열 및 한글 디코딩)

---

## 📂 프로젝트 폴더 구조

```text
gemini38flash/
├── app/
│   ├── build.gradle.kts                      # 앱 모듈 빌드 스크립트 및 의존성
│   ├── proguard-rules.pro                    # 난독화 및 최적화 규칙
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml           # 카메라 권한, 메타데이터, 액티비티 설정
│           ├── java/com/example/qrreader/
│           │   ├── MainActivity.kt           # 메인 컨트롤러 (카메라 바인딩, 비프음, 클립보드 복사)
│           │   ├── BarcodeAnalyzer.kt        # CameraX 프레임 분석 & ML Kit 바코드 인식기
│           │   └── BarcodeFormatMapper.kt    # 포맷 코드 변환 매퍼 (1D/2D/QR 한글 라벨링)
│           └── res/
│               ├── drawable/                 # 아이콘, 버튼, 타겟 프레임, 라운드 카드 배경
│               ├── layout/
│               │   └── activity_main.xml     # 상단바, 프리뷰, 스캔박스, 하단 결과 카드 레이아웃
│               ├── values/
│               │   ├── colors.xml            # 모던 다크 테마 색상 팔레트
│               │   ├── strings.xml           # 한글 리소스 텍스트 (UTF-8)
│               │   └── themes.xml            # Material3 다크 테마
│               └── mipmap-anydpi-v26/        # 앱 런처 아이콘
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties         # Gradle 8.4 래퍼 설정
├── build.gradle.kts                          # 루트 프로젝트 빌드 설정
├── settings.gradle.kts                       # 프로젝트 및 레포지토리 관리
├── gradle.properties                         # JVM 및 AndroidX 옵션
├── gradlew.bat                               # Windows 실행 스크립트
├── .gitignore
└── README.md                                 # 본 개발자 가이드
```

---

## 🔍 핵심 구현 상세

### 1. 바코드 리딩 & UTF-8 한글 처리 (`BarcodeAnalyzer.kt`)
```kotlin
val options = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS) // QR, 1D, 2D 전체 활성화
    .build()

// 한글 UTF-8 디코딩 보정
val decodedText = primaryBarcode.rawBytes?.let { bytes ->
    try {
        String(bytes, StandardCharsets.UTF_8)
    } catch (e: Exception) {
        primaryBarcode.rawValue ?: ""
    }
} ?: (primaryBarcode.rawValue ?: "")
```

### 2. 비프음 "삐" 소리 발생 (`MainActivity.kt`)
`MediaPlayer` 대신 시스템 리소스를 거의 차지하지 않고 지연(Latency)이 없는 `ToneGenerator`를 사용하여 즉각적인 `"삐"` 소리를 출력합니다:
```kotlin
toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

// 스캔 성공 시 호출
toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150) // 150ms 동안 비프음 발생
```

### 3. 클립보드 텍스트 복사 (`MainActivity.kt`)
```kotlin
val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
val clip = ClipData.newPlainText("Barcode Text", currentScannedText)
clipboard.setPrimaryClip(clip)

Toast.makeText(this, "바코드 내용이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
```

### 4. 하단 결과 카드 바인딩
스캔 시 바코드 형식을 1D / 2D / QR 규격명과 함께 사용자 친화적인 뱃지로 출력하고, 내용은 스크롤 박스에 표시하여 긴 문자열도 한눈에 볼 수 있도록 구성되었습니다.

---

## 🚀 빌드 및 실행 방법

### Android Studio에서 열기
1. **Android Studio** (Hedgehog, Iguana, Jellyfish, Koala 이상 권장) 실행
2. **Open** 메뉴 선택 -> 본 프로젝트 폴더 (`gemini38flash`) 선택 후 열기
3. Gradle Sync가 자동으로 진행되며 의존성 라이브러리가 다운로드됩니다.
4. Android 기기를 USB 디버깅으로 연결하거나 가상 기기(Emulator - 카메라 시뮬레이션 지원)를 실행합니다.
5. 상단의 **Run (Shift + F10)** 버튼을 누르면 앱이 빌드되어 실행됩니다.

### 권한 안내
- 앱 첫 실행 시 카메라 권한 허용 대화상자가 표시됩니다. **"앱 사용 중에만 허용"**을 선택하세요.

---

## 💡 개발자 가이드 및 커스터마이징

1. **비프음 톤 및 지속 시간 변경**:
   - `MainActivity.kt`의 `playBeepSound()` 메서드에서 `ToneGenerator.TONE_PROP_BEEP` 대신 `ToneGenerator.TONE_CDMA_PIP` 등으로 음색을 변경하거나 지속 시간(`150` -> `200ms`)을 조절할 수 있습니다.
2. **연속 스캔 모드로 전환하려면**:
   - `BarcodeAnalyzer.kt`에서 `isEnabled = false` 처리 부분을 제거하고, 동일한 바코드의 경우 이전 값과 비교하는 시간 지연(Debounce, 약 1.5초) 로직을 추가하면 마트 계산대와 같은 연속 스캔 모드로 동작시킬 수 있습니다.
3. **특정 바코드만 인식하도록 제한하려면**:
   - `BarcodeAnalyzer.kt`의 `BarcodeScannerOptions.Builder()`에서 `.setBarcodeFormats(Barcode.FORMAT_QR_CODE)`처럼 필요한 규격만 지정하면 인식 속도와 배터리 효율을 더욱 극대화할 수 있습니다.
