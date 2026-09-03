## iConomyUnlocked (mikan6139 fork)

このリポジトリは [SulkyWhale/iConomyUnlocked](https://github.com/SulkyWhale/iConomyUnlocked) のフォークです。

### ダウンロード
オリジナル版は [Modrinth](https://modrinth.com/plugin/iconomyunlocked) から入手できます。
（このフォーク独自のビルドは配布していません。必要な場合はソースからビルドしてください。）

----
### このフォークについて

本家が止まってそうだったので自分用に直したやつです。

- `/shop`でTPS/MSPTが悪化する不具合を修正（MySQLの接続がプーリングされてなかったのでHikariCPで対応）
- Bedrock(Geyser/Floodgate)プレイヤーが`/money top`に出てこない不具合を修正
- ついでに見つけた細かいバグもいくつか直してます

----
### 概要
iConomyの歴史はBukkitプラグイン黎明期、Nijikokun氏の手によるものまで遡ります。7種ほどの派生バージョンを経て、[iConomy 5](https://github.com/iconomy5legacy/iConomy) はElgarL氏、そして最終的にLlmDl氏が引き継ぎました。

シンプルさと安定性で長年多くのサーバーを支えてきたiConomy 5でしたが、[VaultUnlocked](https://github.com/TheNewEconomy/VaultUnlocked) の登場によって、ついにその役目を終える時が来ました。

VaultUnlockedはVaultAPIにようやく正式なUUIDサポートをもたらしました。これにより、Towny のようにプレイヤー以外のアカウント（村・国など）を扱うプラグインも、Vaultの旧式な名前ベースのメソッドを使わずに経済プラグインと連携できるようになります。

iConomyUnlockedは、iConomy 5を使い続けているサーバーに移行先を提供するために作られました。VaultAPIとVaultAPI 2（VaultUnlocked）の両方に対応しているため、Vault系・Vault2系どちらのプラグインとも連携できます。

----
### 特徴
- Vaultベース・Vault2(VaultUnlocked)ベース双方の経済連携プラグインに対応
- Folia対応
- H2・MySQL両方のデータベース形式に対応
- CommentedConfigurationを使用し、設定を保持したままconfigを自動アップデート
- iConomy 5.26からのアカウントインポートに対応

----
### 必要なもの
- [VaultUnlocked](https://github.com/TheNewEconomy/VaultUnlocked) または Vault

----
### コマンド
> <> は必須、[] は任意
- `/money`: 自分の残高を確認
  - `?`: ヘルプ画面を表示
  - `[playername]`: 他プレイヤーの残高を確認
  - `rank`: 資産ランキングでの自分の順位を確認
  - `rank <playername>`: 他プレイヤーの順位を確認
  - `top [amount]`: 資産ランキング上位を表示
  - `pay <player> <amount>`: 指定プレイヤーに送金
  - `grant <player> <amount> <silent>`: 指定プレイヤーの口座に加算
  - `grant <player> -<amount> <silent>`: 指定プレイヤーの口座から減算
  - `set <player> <amount>`: 指定プレイヤーの残高を設定
  - `hide <player> <true/false>`: ランキング上での表示/非表示を切り替え
  - `create <player>`: デフォルト残高でアカウントを作成
  - `remove <player>`: アカウントを削除
  - `reset <player>`: アカウントをデフォルト残高にリセット
  - `purge`: デフォルト残高のままのアカウントを一括削除
  - `empty`: 全アカウントを削除
  - `stats`: 経済全体の統計を確認
  - `importiconomy`: iConomy 5.26からアカウントをインポート

----
### 権限
```yaml
iConomy.admin:
  default: false
  children:
    iConomy.admin.account.create: true
    iConomy.admin.account.remove: true
    iConomy.admin.reset: true
    iConomy.admin.bank.create: true
    iConomy.admin.empty: true
    iConomy.admin.purge: true
    iConomy.admin.stats: true
    iConomy.admin.grant: true
    iConomy.admin.hide: true
    iConomy.admin.set: true
    iConomy.admin.importiconomy: true
iConomy.access:
  default: true
iConomy.payment:
  default: true
iConomy.rank:
  default: true
iConomy.list:
  default: op
```
