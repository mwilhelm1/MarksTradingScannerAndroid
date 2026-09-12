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


## Prepared Android 1.6

Version code 7 adds native V2 Research navigation and an isolated /v2/research read-only screen. This is distinct from the scanner's mobile web assets. Displays reported authority/status, Eastern date/times, bounded receipts, gates, and explicitly historical account evidence. No trading controls or V1 seed/broker requests in V2. Existing Cockpit and Evidence remain separate. Evidence handles the API's nonblocking 202 building response and marks cached stale evidence; retrying the first pending report does not force a duplicate refresh.

Local debug test/build passes all 18 unit tests, including five new V2 route, date, missing-state, bounded-row, and Eastern-time checks. APK versionCode 7/versionName 1.6 and package identity were checked; its signer matches the documented SHA-256 above. No device install or phone-render test was performed. This package has not been published. Release still uses the existing verified signing workflow and requires working GitHub authentication; local release lookup currently returns 401. Do not represent a local APK or web restart as a published Android update.
