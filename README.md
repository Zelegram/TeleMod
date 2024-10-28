# Dev Tools for Telegram Web Apps

**TeleMod** is a modified Telegram Android app that brings `Developer Tools` functionality, similar to desktop browsers, to your Android device. With TeleMod, you can run JavaScript via custom userscripts—just like **[Tampermonkey](https://www.tampermonkey.net/)** and **[Violentmonkey](https://violentmonkey.github.io/)**—to automate, customize, and extend Telegram’s functionality.

## 🚀 Features

- **Developer Tools**: Familiar developer tools experience, just like in desktop browsers.
- **UserScript Support**: Install and manage custom scripts to personalize and enhance your Telegram experience.
- **Script Store**: Access a library of ready-to-use scripts for tasks like automating Crypto Airdrop games.
- **Open Source**: Open to contributions from the community to help evolve and improve the project.

## 📲 Getting Started

### Installation

Download the latest TeleMod APK from the [releases page](https://github.com/yourusername/TelegramEnhanced/releases). Three build types are available:

- **Standalone**: Functions like the Telegram version from its [official website](https://telegram.org/android?setln=en). The package name is `org.telegram.messenger.web`. *(Recommended)*
- **Release**: Similar to the [Play Store](https://play.google.com/store/apps/details?id=org.telegram.messenger&hl=en&gl=US) version, with the package name `org.telegram.messenger`.
- **Huawei**: Optimized for Huawei devices.

> **Note**: To avoid conflicts, uninstall any existing Telegram app before installing TeleMod, as only one Telegram instance can run at a time. For example, if you install the Release version, you must uninstall the Play Store version.

### Developer Tools ⚙️

1. Open any Telegram Web App, Mini App, or Crypto Game.
2. Tap the `Terminal` icon in the top bar to launch Developer Tools.
3. Access familiar tools like **Console**, **Elements**, **Network**, **Resources**, **Info**, and **Snippets**—just as you would on a desktop browser.

### Script Manager 🧩

Tap the Extension icon to open the Script Manager, where you can write, manage, and run userscripts. Scripts can be injected at various stages of webpage loading, such as `load-start`, `document-start`, `document-body`, `document-end`, `document-idle`, `context-menu`, and `load-done`.

<details>
  <summary>Injection Timing Options</summary>

  ```js
  // @run-at load-start
  ```
The script is injected as the WebView starts loading a URL, equivalent to [onPageStarted](https://developer.android.com/reference/android/webkit/WebViewClient#onPageStarted%28android.webkit.WebView,%20java.lang.String,%20android.graphics.Bitmap%29).

  ```js
  // @run-at document-start
  ```
The script is injected as early as possible.

  ```js
  // @run-at document-body
  ```
The script is injected once the `<body>` element is present.

  ```js
  // @run-at document-end
  ```
The script is injected after the DOMContentLoaded event fires.

  ```js
  // @run-at document-idle
  ```
The script is injected after the DOMContentLoaded event.

  ```js
  // @run-at context-menu
  ```
The script is injected when the context menu is opened.

  ```js
  // @run-at load-done
  ```
The script is injected when the WebView finishes loading, similar to [onPageFinished](https://developer.android.com/reference/android/webkit/WebViewClient#onPageFinished%28android.webkit.WebView,%20java.lang.String%29).

</details>

### Script Store 🤖

Access pre-built scripts in **TeleMod Scripts 🤖**. Tap the Star icon on the Home page to browse the Script Store.

## 🛠️ Contributing

We welcome developers interested in improving TeleMod! Fork the repository, make your changes, and create a pull request (PR).

Our goal is to build a specialized Telegram app for Crypto airdrop hunters and to aid developers in testing and debugging their Telegram Web Apps. We strictly avoid misuse or altering of Telegram’s core functionality, focusing instead on enhancing Telegram Web Apps via WebView. Contributions in line with these principles are appreciated.

### Prerequisites

- You will require [Android Studio](https://developer.android.com/studio) 3.4, Android NDK rev. 20 and Android SDK 8.1
- Basic knowledge of Android development, Java, and Kotlin

> The official Telegram source code is primarily in Java, but all modifications in TeleMod are implemented in Kotlin. These modifications are located in the `org/telegram/mod` directory within the `TMessagesProj` module.