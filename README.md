# helpGPT

open AI의 Chat-GPT / Whisper등 공개 API를 활용한 안드로이드 앱

## 관련 기술
  - `Android`, `MVVM`, `Kotlin`, `hilt`, `AAC`

## 오픈소스 라이브러리
 - `ktor`, `Retrofit2`, `ok-http3`, `open-ai-kotlin`, `Retrofit2`, `android-vad:webrtc` 등

## 기능

### 동영상 자동 번역

Android Audio Playback Capture API를 활용해, 사용자의 화면에서 실시간으로 오디오를 캡쳐하여
open-ai Whisper 모델을 이용해 음성을 문자로 만듭니다. 이를 한국어로 번역해 화면위에 표시해줍니다.

### 유튜브 영상 요약

open-ai chatGPT API를 활용해, 유튜브 영상의 내용을 요약합니다.
