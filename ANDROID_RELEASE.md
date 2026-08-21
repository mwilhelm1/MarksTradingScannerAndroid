# Android release preparation

Release publication is intentionally limited to tags matching `android-v*`.
Ordinary pushes and pull requests run tests and build a debug APK only.

## Safety gate before configuration

Do not configure signing secrets or create a release tag until the installed
phone application's signing certificate has been verified as:

`15cb930b519475f423b44376b63e333a3a4ce79c9c36ea3b729d37cfd0c134e8`

The local debug keystore is only a candidate signing identity until that device
verification succeeds. Never commit or regenerate it.

## Required GitHub secrets

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `MOBILE_API_KEY`

## Required GitHub variables

- `MOBILE_API_BASE_URL`
- `ANDROID_SIGNING_CERT_SHA256`

Set `ANDROID_SIGNING_CERT_SHA256` only after device verification. The workflow
compares the built APK signer to this value before publishing.

## Versioning

Every release must increase `versionCode` in `app/build.gradle.kts`. Set
`versionName` to the release tag suffix: version `1.1` uses tag `android-v1.1`.
The workflow rejects a tag whose suffix does not match the APK version name.

## Update checks

Release builds receive `ANDROID_UPDATE_REPOSITORY` from `github.repository`.
Local builds may set the same name in `local.properties`; when it is blank,
update checks are disabled without affecting the scanner dashboard.

The app queries public GitHub releases without credentials, considers only
published non-prerelease `android-v*` tags, and shows an Update button only for
a newer version. Android's normal package installer performs the update.
