# @aun-abbas/expo-pin-shortcut

[![npm](https://img.shields.io/npm/v/@aun-abbas/expo-pin-shortcut.svg)](https://www.npmjs.com/package/@aun-abbas/expo-pin-shortcut)
[![Android](https://img.shields.io/badge/Android-✓-green)](#)
[![iOS](https://img.shields.io/badge/iOS-no--op-lightgrey)](#ios)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Pin a shortcut to the Android home screen from your Expo or React Native app — like Chrome's "Add to Home Screen" for websites, but invokable from your own app for arbitrary deep links.

> **Android-only by design.** iOS does not provide a programmatic API for adding home screen icons. On iOS this module's `pin()` throws `E_UNSUPPORTED` and `isSupported()` returns `false`. See [iOS](#ios) below for the rationale.

## Pinned shortcut vs. App shortcut

This package creates **pinned shortcuts** — actual icons placed on the user's home screen, separately from your app icon, with their own labels and (optionally) custom favicons. Tapping one fires a deep link into your app.

It does **not** create *app shortcuts* (the menu that pops up when long-pressing your app icon). For that, use [`expo-quick-actions`](https://github.com/EvanBacon/expo-quick-actions) or [`@rn-bridge/react-native-shortcuts`](https://www.npmjs.com/package/@rn-bridge/react-native-shortcuts) — both of which are about the long-press menu, not the home screen itself.

## Installation

### Expo

```sh
yarn add @aun-abbas/expo-pin-shortcut
npx expo prebuild --clean
yarn android
```

The module autolinks via Expo's module system. No manual configuration needed.

### Bare React Native (without Expo)

```sh
yarn add @aun-abbas/expo-pin-shortcut
npx install-expo-modules    # only the first time you add an Expo module
cd android && ./gradlew assembleDebug
```

`npx install-expo-modules` installs `expo` + `expo-modules-core` and wires up the autolinking. Once installed, all Expo modules (including this one) work in a bare RN project.

## Usage

```ts
import { appShortcut } from '@aun-abbas/expo-pin-shortcut';

if (appShortcut.isSupported()) {
  try {
    const ok = await appShortcut.pin({
      id: 'unique-shortcut-id',
      label: 'My Shortcut',
      uri: 'myapp://route?param=value',
      iconUrl: 'https://example.com/icon.png',
    });
    if (ok) {
      // Pin request was accepted by the launcher; the user has been prompted.
    }
  } catch (err) {
    // err.code: 'E_UNSUPPORTED' | 'E_INVALID_ARGS' | 'E_INVALID_URI' | 'E_PIN_FAILED'
    console.warn(err);
  }
}
```

For tapping the shortcut to actually open your app, your app must register an intent filter for the URI scheme you used. With Expo Router, this is automatic if `uri` uses your app's `scheme` from `app.json`.

## API

### `appShortcut.isSupported(): boolean`

Synchronous. Returns `true` if the device's default launcher accepts pinned shortcut requests.

- **Android:** wraps `ShortcutManagerCompat.isRequestPinShortcutSupported()`. Most modern launchers (Pixel Launcher, Nova, Samsung One UI) return `true`. Some AOSP emulator launchers return `false`.
- **iOS:** always returns `false`.
- **Web:** always returns `false`.

### `appShortcut.pin(input: PinShortcutInput): Promise<boolean>`

Asks the launcher to pin a shortcut. The launcher typically shows a system dialog asking the user to confirm.

```ts
interface PinShortcutInput {
  /** Stable identifier for the shortcut. Re-using an existing id replaces that pin. */
  id: string;
  /** Label shown under the icon on the home screen. */
  label: string;
  /** Deep link URI opened when the shortcut is tapped, e.g. `myapp://route`. */
  uri: string;
  /** Optional remote favicon URL. Falls back to your app's launcher icon. */
  iconUrl?: string | null;
}
```

**Resolves with `true`** if the launcher accepted the pin request (the user has been shown the confirm dialog). Note: the resolved `true` does **not** guarantee the user actually accepted — Android does not surface that signal back to your app reliably. It only confirms the request was delivered.

**Resolves with `false`** on iOS / web (no-op).

**Throws `CodedException`** on Android with one of:
- `E_UNSUPPORTED` — launcher does not support pinning. Falls back gracefully; show a toast.
- `E_INVALID_ARGS` — `id`, `label`, or `uri` was empty.
- `E_INVALID_URI` — `uri` could not be parsed.
- `E_PIN_FAILED` — anything else went wrong (icon download crash, system error). The exception message includes the underlying class + message.

## Icon handling

If you pass `iconUrl`, the native side downloads the bitmap on a background thread, then renders it onto a 432×432 white square with ~20% padding to survive launcher icon masking (adaptive icon shape). If the download fails or `iconUrl` is null, the launcher icon (`R.mipmap.ic_launcher`) is used instead.

## iOS

iOS does not expose a programmatic API for adding home screen icons. This is an explicit Apple platform decision, not an oversight, and has not changed through iOS 26.

The closest analogues — `UIApplicationShortcutItem` (long-press app menu), Shortcuts.app workflows, Universal Links + Safari "Add to Home Screen" — are all the wrong shape for this feature. Rather than ship a half-implementation that confuses users, this module is explicitly Android-only on iOS.

If your use case can be served by long-press app shortcuts, see [`expo-quick-actions`](https://github.com/EvanBacon/expo-quick-actions).

## Why this exists

I wanted Chrome's "Add to Home Screen" experience for arbitrary deep links inside my own app. None of the existing `react-native-*-shortcuts` packages on npm wrap `requestPinShortcut` — they all do app shortcuts (the long-press menu). So I built this.

## License

[MIT](LICENSE) © Aun Abbas
