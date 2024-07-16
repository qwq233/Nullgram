# Nullgram

[![Telegram](https://img.shields.io/static/v1?label=Telegram&message=@NullgramClient&color=0088cc)](https://t.me/NullgramClient)  [![CI build](https://github.com/qwq233/Nullgram/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/qwq233/Nullgram/actions/workflows/ci.yml/)  [![Crowdin](https://badges.crowdin.net/nullgram/localized.svg)](https://crowdin.com/project/nullgram)

[English](README.md)  [中文](README_CN.md)

Nullgramは、[Telegram App for Android](https://play.google.com/store/apps/details?id=org.telegram.messenger)の公式ソースコードをベースとした **無料でオープンソース** なサードパーティー製のTelegramクライアントです。

## 何故Nullgramなのか
NekoXとNekogramのコミュニティが分断[^1]され、NekoXとNekogramの主要となる開発者が深刻な問題を抱えている状態のため、これらの問題を回避するために両者の機能を統合したNullgramを開発することにしました。

コンピュータープログラミングにおいて、初期化されていないまたは定義されていない、空の状態、あるいは意味がない値という意味で使用されるNull。

Nullgramの名前には、悪意のある物は存在しないことを表す意味があります。Nullgramは、あなたのデバイスにFCM通知 ｢nmsl｣などの通知[^2]やチャンネルに広告を送信[^4]、悪意を持った競争や競合他社に関する悪意ある噂を公表[^5]等はしません。

## 貢献の方法

### 新しい機能を追加したい
素晴らしいですね!

まずは[開発ドキュメント](docs/CONTRIBUTING.md)を完全に理解していることを確認してください。
もしも未読の場合はドキュメントをお読みください。**必ず読んでください。**

その後に新規のPull Requestを作成してください。

### バグに遭遇しました!
まず、最新のバージョンがインストールされていることを確認してください(チャンネル内を確認してください、Playストア版は最新版の公開までに遅延があります)。

その問題が公式のTelegramクライアントにも表示される場合は、それを公式に提出してください(説明文やスクリーンショットにNullgramを含めないように注意してください、公式の開発者は私たちを嫌っています!)。

その後に発生した問題を詳細に説明してください(英語のみ)。Issueを作成するか、私たちのグループ内に#bugを付けて投稿してください。

Issueテンプレートを使用して詳細なバージョン番号を入力してください。**私は最新のバージョンを使用していますが、あなたが使用しているバージョンはわかりません。**

クラッシュが発生した場合は、logcatを使用してログを取得することができます(タグ:`Nullgram`)。

### コンパイルガイド

Android NDK rev.21とAndroid SDK 14が必要です。

1. Telegramのソースコードを https://github.com/qwq233/Nullgram からダウンロードしてください。
2. [こちら](https://ccache.dev/)からccacheをダウンロードして`PATH`にあることを確認してください。
3. TMessagesProj/configのrelease.keystoreを自分のものに置換してください。
4. gradle.properties内のRELEASE_KEY_PASSWORD、RELEASE_KEY_ALIAS、RELEASE_STORE_PASSWORDでrelease.keystoreにアクセスします。
5. https://console.firebase.google.com/ にアクセスしてアプリIDを`top.qwq2333.nullgram`でAndroidアプリを1つ作成してfirebase messagingをONにし、google-services.jsonをダウンロードしてTMessagesProjと同じフォルダにコピーしてください。
6. ターミナルを開いて`./gradlew assembleRelease`を実行してAPKをビルドしてください。

## スポンサー

IDEに無料のオープンソースライセンスを割り当ててくれたJetbrainsとOSSのスポンサーになって頂いたCloudFlareに感謝をします。

[<img src="docs/jetbrains-variant-3.png" width="200"/>](https://jb.gg/OpenSource)
[<img src="docs/CF_logomark.svg" width="200"/>](https://www.cloudflare.com/)


[^1]: https://telegra.ph/%E6%9C%89%E5%85%B3-Nekogram-Lite-%E7%9A%84%E6%95%85%E4%BA%8B-04-09

[^2]:https://sm.ms/image/FAKi3mx6XwqlvRj

[^3]:https://t.me/NekogramX/418

[^4]:https://t.me/zuragram/392

[^5]:https://t.me/sayingArchive/15428
