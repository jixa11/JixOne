# Working on JixOne (native) as an AI agent

JixOne's native Android app is a fork of Metrolist (GPL-3.0). The engine lives
under the `com.metrolist.music` namespace; the upstream attribution in
`LICENSE` and `NOTICE.md` must stay.

## Rules

1. Commit messages: `type(scope): short description`, e.g. `fix(onboarding): …`.
2. New user-facing strings go in `app/src/main/res/values/jixone_strings.xml`
   (English default only). Do not edit the translated `values-*` files.
3. Do not change the Room database schema unless the task requires it.
4. Room DAO methods that are not `suspend` are blocking: call them from
   `Dispatchers.IO`, never from the main thread.
5. Do not bump the app version unless the user asks for a release.
6. Comments only for non-obvious logic.

## Building and testing

```bash
./gradlew :app:assembleGmsDebug
./gradlew :app:testGmsDebugUnitTest
```

CI (`.github/workflows/native-release.yml`) builds every push and pull request
that touches `native/`; tags starting with `n` publish a signed release.
