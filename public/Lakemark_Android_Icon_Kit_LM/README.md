# Lakemark – LM Mark Logo & Android Icon Kit (flat SVG)

Colours: L + upper wave green #04C353 · M + lower wave lime #7BE842 · background purple #2B0938 (light purple #3D0F4F only on reversed badges) · text #3D0F4F / #5B2A6E / #49125E · cream #FDF6E9
Wordmark: Poppins Bold / Medium, converted to outlines.

## Folders
- `svg/` – master vectors: mark, badge, stacked / tagline / horizontal logos (+ reversed), one-colour marks, app icon artwork, adaptive layers, notification icon, favicon
- `android/app/src/main/res/` – copy into your project's `res/`: adaptive icon (`mipmap-anydpi-v26`, `drawable/ic_launcher_*`), legacy PNG icons, notification icon, Android 12 splash, in-app logo vectors, brand colours
- `google-play/` – `play_store_icon_512.png` (full-bleed) and the feature graphic
- `png-exports/` – high-resolution PNGs
- `web-pwa/` – favicon, PWA and maskable icons, manifest.json

## Manifest
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:theme="@style/Theme.Lakemark.Starting" ...>
```
Notification: `setSmallIcon(R.drawable.ic_stat_lakemark)`.
