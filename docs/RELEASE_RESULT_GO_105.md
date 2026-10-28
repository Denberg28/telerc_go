# TeleRC Go 1.0.5 — published result

Published **29 October 2026 (Philippine time)** as
[go-v1.0.5 prerelease](https://github.com/Denberg28/telerc_go/releases/tag/go-v1.0.5).

[Download the signed production APK](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.5/TeleRC-Go-v1.0.5.apk).
Use **Settings → Background → Choose image**. Imported wallpaper persists across
restarts; **Restore default** returns to the bundled ship image.

## Identity and installation

- Production package: `io.github.denberg28.telerc.go`, version **1.0.5/code 1001**.
- Exact APK/source commit and release tag target:
  `cfce723beb37b7445dd96725bffdafb31c25ba02`.
- Production certificate retained from 1.0.4/code 1000:
  `5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec`.
- Signed APK: 1,089,468 bytes; SHA-256:
  `c8dff06fa7529fcfe6447e7f2ec7bc14ca6c0bc551a82bde8468ad93378e0b13`.
- Source archive SHA-256:
  `ae974d36289f33e14811cdd508f64679bb233272dd5181b66f790eca86be5eeb`.

Install the production APK over the existing production Go installation; the
same package/certificate and increased code support an in-place upgrade. Do not
uninstall the working app merely to install this update. Actual phone installation
and settings retention still need device acceptance. The separate debug-signed
Candidate preview has independent data and is not the production upgrade.

This app-only update needs **no firmware reflash**. Retain the matched firmware,
wiring, receiver and motor configuration. The existing stable in-app feed skips
prereleases; download and install this GitHub prerelease manually.

## Final gates

| Gate | Status | Evidence and limit |
| --- | --- | --- |
| A — source/build | PASS — build verified | Android debug/release build and lint; full-app regression; exact clean source archive and release tag. |
| B — behavior | PASS — software tested | 23 Go JVM tests, zero failures/errors/skips; 18 source/archive packaging checks. |
| C — regression | PASS — cloud verified | Shared-source checks, normal bridge regression and default ASan/UBSan/LeakSanitizer. Existing released stop/resume behavior retained. |
| D — integration | PASS — native simulator tested | API 35 photo import, cancel, corrupt/oversize rollback, rebuild/restart persistence, restore-default and fitted normal/large-text landscape UI. |
| E — phone/HIL | BLOCKED — not hardware tested | Actual gallery providers, phone upgrade, retained settings and restrained-wheel interruption checks remain pending. |
| F — delivery | PASS — signed prerelease | Retained signer, production identity/code, alignment/ZIP, sanitized source, checksums and all 11 hosted assets verified. Stable/field qualification is not claimed. |

Producers:

- [Full software/native checks](https://github.com/Denberg28/telerc_go/actions/runs/40780005483), source `cfce723beb37b7445dd96725bffdafb31c25ba02`.
- [Retained-signer build](https://github.com/Denberg28/TeleRC/actions/runs/40780129259), signing workflow commit `95017c90ef4db8d3cc178a9073df964f24d39250`.
- [Successful publication verification](https://github.com/Denberg28/telerc_go/actions/runs/40781569624), publication commit `ba4e3fb18f2f86cc4cc614f9cd705bc9282c6dab`.

The initial uploader staged and uploaded all verified assets but stopped at a
tag lookup for an unpublished draft. The finalizer used the draft's release ID,
downloaded all 11 original assets and verified each byte/hash before publication.
No APK, source or release asset was replaced. Public release metadata, canonical
download URLs, sizes and all SHA-256 digests were then rechecked against the
immutable publication artifact.

The release includes APK, exact source, test evidence, installation/acceptance
guide, build/verification/provenance records and
[SHA256SUMS.txt](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.5/SHA256SUMS.txt).
No signing key or private configuration was exported. Earlier Candidate-only and
production-signing-pending notes in the review history are superseded by this result.

Next: perform the phone/raised-wheel acceptance in the released
[WALLPAPER_GUIDE.md](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.5/WALLPAPER_GUIDE.md).
Restore default rolls back the wallpaper immediately. Retain the previous signed
1.0.4 APK for an app rollback; Android downgrade may require uninstall/data loss.
