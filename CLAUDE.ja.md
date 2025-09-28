# プロジェクト設定

## 言語設定
**AI アシスタントとの対話**: 日本語
**英語版ドキュメント**: [CLAUDE.md](CLAUDE.md) を参照

## プロジェクト概要
- **アプリ名**: 5G（com.teampansaru.fiveg）
- **種類**: Androidウィジェットアプリ（踊るおじさんウィジェット）
- **主要機能**: ネットワーク状態監視、ウィジェット表示

## 開発環境
- **Android Studio プロジェクト**
- **言語**: Kotlin
- **最小SDKバージョン**: 30 (Android 11)
- **ターゲットSDK**: 36 (Android 15)
- **コンパイルSDK**: 36

## ビルド設定
- **Gradle バージョン**: 8.14.3
- **Kotlin バージョン**: 2.2.0

## 主要コンポーネント
- `DancingOldmanWidget`: ウィジェットプロバイダー
- `NetworkService`: フォアグラウンドサービス
- `MainActivity`: メインアクティビティ
- `WalkThroughFragment`: チュートリアル画面
- `CustomAdapter`: カスタムアダプター

## 必要な権限
- ACCESS_NETWORK_STATE（ネットワーク状態の確認）
- READ_PHONE_STATE（電話状態の読み取り）
- FOREGROUND_SERVICE（フォアグラウンドサービス）

## コーディング規約
- Kotlinの標準的な命名規則に従う
- パッケージ名: com.teampansaru.fiveg

## 注意事項
- ウィジェット更新時はAppWidgetManagerを使用
- ネットワーク監視はフォアグラウンドサービスで実行
- Android 11以上が必須（minSdkVersion 30）