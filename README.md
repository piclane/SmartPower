# SmartPower

スマートメータからWi-SUN Bルートで電力量を取得し、現在の電力消費量を表示します。

![スクリーンショット](doc/images/screenshot.png)

## 機能

- **リアルタイム電力監視**: スマートメータから瞬時電力値と電流値を取得
- **GraphQL API**: データを柔軟に取得できるAPIエンドポイント
- **Prometheus メトリクス**: 監視システムとの連携用メトリクス出力
- **Web UI**: ブラウザでリアルタイムに電力消費量を確認

## API エンドポイント

### GraphQL API
`http://localhost:8080/graphql`

**利用可能なクエリ:**
- `instantaneous`: 瞬時計測値（電力、電流）を取得
- `powerSource`: 電源設定（定格電流、線式）を取得

**利用可能なサブスクリプション:**
- `instantaneous`: 瞬時計測値の変更をリアルタイムで購読

**クエリ例:**
```graphql
query {
  instantaneous {
    power
    current {
      rPhase
      tPhase
      sum
    }
  }
  powerSource {
    ratedCurrentA
    wireCount
  }
}
```

**サブスクリプション例:**
```graphql
subscription {
  instantaneous {
    power
    current {
      rPhase
      tPhase
    }
  }
}
```

### Prometheus メトリクス
`http://localhost:8080/actuator/prometheus`

**利用可能なメトリクス:**
- `instantaneous_power`: 瞬時電力計測値 (W)
- `instantaneous_current_phase_r`: R相電流計測値 (A)
- `instantaneous_current_phase_t`: T相電流計測値 (A)
- `cumulative_forward_energy_total`: 正方向積算電力量 (kWh)

## 環境変数

### Wi-SUN デバイス設定
- **DEVICE_PATH**
  - Wi-SUN デバイスへのパスを指定します
  - 例: `/dev/ttyUSB0`（Linux）、`/dev/cu.usbserial-XXXXXXXX`（macOS）
  - 必須項目

- **DEVICE_PASSWORD**
  - 電力メーター情報発信サービス（Ｂルートサービス）のパスワード
  - 電力会社からメールで送付される認証情報
  - 形式: 英数字32文字
  - 必須項目

- **DEVICE_RBID**
  - 電力メーター情報発信サービス（Ｂルートサービス）の認証ID
  - 電力会社から郵便で送付される認証情報
  - 形式: 英数字32文字
  - 必須項目

### 電源設定
- **POWER_SOURCE_RATED_COUNT_A**
  - 契約アンペア数（定格電流）
  - 範囲: 10〜60（A）
  - 一般的な値: 30A, 40A, 50A, 60A
  - 必須項目

- **POWER_SOURCE_WIRE_COUNT**
  - 電源の線式
  - 値: `2`（単相2線式）または `3`（単相3線式）
  - 一般家庭では通常 `3`
  - 必須項目

### ログ設定
- **LOG_LEVEL_SK**
  - Wi-SUN通信のログレベル
  - 値: `debug`, `info`, `warn`, `error`
  - 初期設定時は `debug` を推奨、安定稼働後は `info`
  - オプション項目

### その他
- **TZ**
  - タイムゾーン設定
  - 例: `Asia/Tokyo`
  - 日本では `Asia/Tokyo` を指定

## 電力メーター情報発信サービス（Bルートサービス）について

スマートメータからデータを取得するには、各電力会社の「電力メーター情報発信サービス」への申し込みが必要です。

1. **申し込み**: 各電力会社のWebサイトから申し込み
2. **認証ID受領**: 郵便で認証ID（RBID）が送付される
3. **パスワード受領**: メールでパスワードが送付される
4. **Wi-SUN USBドングル**: 対応デバイスを別途購入・接続

## 起動方法

1. `docker-compose.yml` の environment セクションを上記環境変数の説明に従って設定
2. 以下のコマンドで起動:

```bash
docker compose up
```

3. 起動後、以下のURLにアクセス:
  - **Web UI**: http://localhost:8080/
  - **GraphQL API**: http://localhost:8080/graphql
  - **Prometheus メトリクス**: http://localhost:8080/actuator/prometheus

## トラブルシューティング

- **デバイス接続エラー**: `DEVICE_PATH` の値を確認し、Wi-SUN USBドングルが正しく接続されているか確認
- **認証エラー**: `DEVICE_PASSWORD` と `DEVICE_RBID` の値を確認
- **スマートメータが見つからない**: `LOG_LEVEL_SK=debug` に設定して詳細ログを確認
- **データが取得できない**: スマートメータとの距離を確認し、電波状況を改善
