# Lakemark Kitchen — APK Security Checklist (`https://lakemarkhotel.com/`)

## Before publishing

- [x] Build a release version of the APK, not a debug version (`app/build/outputs/apk/release/lakemark-v3.4.7.apk`).
- [x] Sign the APK with your own private release key (`my-upload-key.jks`, alias `upload`).
- [x] Use the same signing key for every future update (cert SHA-256: `2b:2d:58:5f:f1:94:0c:ba:0a:f6:85:e7:f0:8d:ca:e0:d3:e6:73:8b:be:b7:35:da:f0:5c:7d:3e:1f:e3:2e:ab`).
- [x] Use a unique package name: `com.aistudio.applet.kphrxs`.
- [x] Set a clear app name, version number, and version code (`Lakemark`, `3.4.7`, `versionCode = 21`).
- [x] Add a proper app icon and app label (`Lakemark_Android_Icon_Kit_LM`).
- [x] Remove test accounts, private server addresses, and debug configurations from the APK (`allowBackup="false"`, `usesCleartextTraffic="false"`).
- [x] Check that the APK does not request unnecessary permissions.
- [ ] Test installation and updating on a real Android phone.

## Hosting the APK

- [ ] Host the APK on your own HTTPS website (`https://lakemarkhotel.com/`) or GitHub Releases.
- [ ] Use a stable direct download URL, for example:  
      `https://lakemarkhotel.com/apps/lakemark/v3.4.7/lakemark-v3.4.7.apk`
- [x] Use HTTPS, not HTTP (enforced by `network_security_config.xml` and `ApkUpdateVerifier`).
- [x] Use a clear APK filename: `lakemark-v3.4.7.apk`.
- [ ] Keep the download link working after you publish it.
- [x] Do not upload the APK to Google Drive as a raw view link; Google Drive may block direct APK downloads with an HTML page.
- [ ] Keep old APK files only if users may need them; otherwise remove outdated versions.

## Download page (`https://lakemarkhotel.com/app`)

- [ ] Create a download page at `https://lakemarkhotel.com/app`.
- [ ] Show the app name (`Lakemark Kitchen`), LM logo, version (`3.4.7`), and update date.
- [ ] Add a large and clear **Download APK** button.
- [ ] Explain that Android may show an “Install unknown apps” warning once per device.
- [ ] Give simple installation instructions.
- [ ] Show the APK file size (`20.9 MB`).
- [ ] Provide a support email, Telegram, WhatsApp, or contact link (`https://lakemarkhotel.com/`).
- [x] Do not claim the app is on Google Play if it is not.

## User instructions

Add this text to your download page (`https://lakemarkhotel.com/app`):

```text
1. Tap “Download APK”.

2. When Android asks about installing unknown apps,
   tap “Settings”.

3. Turn on “Allow from this source” for Chrome
   (or Lakemark for in-app updates). On Xiaomi/HyperOS,
   confirm the one-time prompt and tap OK.

4. Go back and tap “Install” (or “Update”).

This warning is normal the first time because the app is
downloaded directly from https://lakemarkhotel.com/, not from Google Play.
Future updates install directly without repeating this prompt.
```

## APK integrity

- [x] Generate a SHA-256 checksum for `lakemark-v3.4.7.apk`:  
      `E148A9FEA4B54DAD431013FE0E4A4D26D509FEF9DF32F8E058B8241D1B89EA9A`
- [ ] Publish the checksum on `https://lakemarkhotel.com/app`.
- [ ] Tell users to compare the checksum after downloading, if they know how.
- [x] Never share the APK through random file-sharing websites.
- [x] Do not send the APK through untrusted links or unknown messaging groups.

## Updates

- [x] Keep the same package name (`com.aistudio.applet.kphrxs`) for updates.
- [x] Increase the version code for every new release (next release `>= 22`).
- [x] Increase the visible version name, for example from `3.4.7` to `3.4.8`.
- [x] Sign every update with the same private key (`my-upload-key.jks`).
- [ ] Test that the new APK installs over the old version.
- [ ] Update `https://lakemarkhotel.com/app` and **Manage → Alerts → Release Update & Delivery** with the new version, date, SHA-256, and HTTPS APK link.
- [ ] Tell users to install updates only from `https://lakemarkhotel.com/` or the in-app **Update Now** prompt.

## Protect your signing key

- [x] Keep your keystore file private (`my-upload-key.jks`).
- [x] Do not upload the keystore to GitHub (`Github-project` excludes `*.jks`), Google Drive, Telegram, or any public place.
- [x] Use a strong keystore password.
- [ ] Save a secure backup of the keystore in a private location.
- [x] Do not share the keystore password with anyone.
- [x] If you lose the signing key, users cannot update the app normally; they may need to uninstall and reinstall it.

## Android security warnings

- [x] Do not ask users to disable Play Protect.
- [x] Do not ask users to turn off security settings.
- [x] Do not ask users to grant broad permissions unless the app truly needs them.
- [x] Explain why each important permission is needed (`REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS`, `CAMERA`).
- [x] Respect user privacy and do not collect unnecessary personal data.

## Final check

- [x] APK is signed (`APK Signature Scheme v2` verified).
- [ ] APK is hosted on HTTPS (`https://lakemarkhotel.com/`).
- [ ] Download page is ready (`https://lakemarkhotel.com/app`).
- [x] Installation instructions are clear.
- [ ] SHA-256 checksum is published (`E148A9FEA4B54DAD431013FE0E4A4D26D509FEF9DF32F8E058B8241D1B89EA9A`).
- [ ] App has been tested on Android.
- [ ] Update process has been tested.
- [ ] Support contact is available on `https://lakemarkhotel.com/`.