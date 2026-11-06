# Gadżetownia — Lab 3: komunikacja sieciowa i synchronizacja danych

Katalog sklepu z elektroniką i gadżetami działający w trybie **offline-first**.
Dane pochodzą z publicznego API [DummyJSON](https://dummyjson.com/) (bez klucza), są zapisywane w **Room**,
a **UI zawsze czyta z Room**. Synchronizację wykonuje **WorkManager**, sieć obsługują **Retrofit + OkHttp**.

```
API (Retrofit/OkHttp) → ProductRepository → Room → ViewModel (StateFlow) → UI (fragmenty)
                             ↑
                 SyncWorker (WorkManager)
```

## Gałęzie

| Gałąź | Zawartość |
|---|---|
| `main` | wersja startowa dla studentów: 7 zaślepek `TODO(UCZESTNIK) N`, testy-sprawdzarki, [`TASK.md`](TASK.md) |
| `solution` | wersja pełna (demo + rozwiązanie wszystkich TODO), skrypt `tools/make_starter.py` |

## Uruchomienie

1. Otwórz katalog repozytorium w Android Studio (Gradle JDK: 17–21, np. JBR 21).
2. Uruchom konfigurację `app` na emulatorze lub telefonie (minSdk 24).
3. Kliknij ikonę **Synchronizuj** (albo przeciągnij listę w dół).

Testy (JVM, bez emulatora i bez internetu):

```bash
./gradlew testDebugUnitTest
```

Na Windowsie: `gradlew.bat testDebugUnitTest`. Testy WorkManagera używają Robolectric (przy pierwszym
uruchomieniu Gradle pobiera obraz Androida dla Robolectric, potem działa offline).

## Ekrany

- **Lista produktów**: pasek statusu synchronizacji, chipy kategorii (filtrowanie lokalne w Room),
  pull-to-refresh, miniatury (Coil), cena, rabat, ocena. Po nieudanej synchronizacji lista zostaje z danymi z Room,
  a Snackbar proponuje „Ponów”.
- **Szczegóły produktu**: od razu dane z Room, równolegle `GET products/{id}` (opis, marka, stan magazynu,
  galeria). Odpowiedź jest buforowana w cache HTTP przez 5 minut.
- **Menu debug** (tylko build debug, menu ⋮ na liście):
  - *Źródło danych*: `REMOTE` (DummyJSON) / `OFFLINE_ASSETS` (pliki z `app/src/main/assets/api/`, bez internetu),
  - *Chaos*: `NONE` / `HTTP_503` / `TIMEOUT` / `NO_NETWORK` — lokalna symulacja błędów w `ChaosInterceptor`.
  Wybór działa od razu, bez restartu aplikacji.

## Mapa mechanizmów Lab 3 → pliki

| Mechanizm | Plik |
|---|---|
| Interfejs Retrofit, `suspend fun`, `@Query`, `@Path` | `data/remote/ProductApi.kt` |
| Budowa OkHttp i Retrofit, kolejność interceptorów, timeouty, cache | `data/remote/NetworkModule.kt` |
| Interceptor nagłówków (`X-Client`, `Accept`) | `data/remote/HeaderInterceptor.kt` |
| Network interceptor `Cache-Control` dla `products/{id}` | `data/remote/CacheControlInterceptor.kt` |
| Symulacja błędów (503, timeout, brak sieci) | `data/remote/ChaosInterceptor.kt` |
| Tryb offline z plików `assets` | `data/remote/AssetsInterceptor.kt` |
| kotlinx.serialization, DTO z wartościami domyślnymi | `data/remote/dto/` |
| API → Room, `HttpException` vs `IOException`, `RefreshResult` | `data/ProductRepository.kt`, `data/RefreshResult.kt` |
| Wzorzec „gotowy obok” (kategorie, bez WorkManagera) | `data/CategoryRepository.kt` |
| `CoroutineWorker`, success / retry / failure | `sync/SyncWorker.kt` |
| `OneTimeWorkRequest`, constraint sieci, exponential backoff, unikalna praca, `PeriodicWorkRequest` | `sync/SyncScheduler.kt` |
| `WorkInfo` → status w UI | `sync/SyncStatusMapper.kt`, `core/SyncStatus.kt` |
| Room (encje, DAO z `Flow`, `@Upsert`) | `data/local/` |
| MVVM, `StateFlow`, `flatMapLatest`, `repeatOnLifecycle` | `ui/list/`, `ui/details/` |
| Ręczne DI | `AppContainer.kt`, `CatalogApp.kt` |

## Scenariusz demo

1. Pierwsze uruchomienie: pusta lista, chipy kategorii już są (odświeżone bez Workera).
2. „Synchronizuj” → „Synchronizuję…” → „Zsynchronizowano 194 produkty o …”. W Logcat (filtr `okhttp`)
   widać żądanie z nagłówkiem `X-Client` i odpowiedź.
3. Szczegóły produktu → powrót → ponowne wejście: drugie żądanie nie trafia do sieci (cache HTTP,
   w logach `<-- 200 ... (… ms, cached)` nie ma ponownego `--> GET`). W App Inspection → Network widać jedno żądanie.
4. Tryb samolotowy: lista nadal działa (Room). „Synchronizuj” → „Czekam na połączenie z siecią…”;
   po wyłączeniu trybu samolotowego praca rusza sama.
5. Chaos `HTTP_503` → „Błąd, ponowię próbę (próba 2)” → … → „Synchronizacja nie powiodła się: serwer zwrócił błąd HTTP 503”
   + Snackbar „Ponów”. Dane w Room nietknięte.
6. Chaos `NO_NETWORK` / `TIMEOUT` → inny powód błędu („problem z siecią …”): `IOException` zamiast `HttpException`.
7. Kilkukrotne szybkie kliknięcie „Synchronizuj” → jedna praca (`ExistingWorkPolicy.KEEP`).
8. App Inspection → Background Task Inspector: jedna praca okresowa `product_sync_periodic`, także po restartach.
9. Tryb `OFFLINE_ASSETS` → synchronizacja działa bez internetu.

## Wersje narzędzi i bibliotek

| Element | Wersja |
|---|---|
| Android Gradle Plugin / Gradle | 8.13.2 / 8.14.3 |
| Kotlin / KSP | 2.3.21 / 2.3.7 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| JDK (Gradle) / jvmTarget | 21 / 17 |
| core-ktx / appcompat / activity / fragment | 1.18.0 / 1.7.1 / 1.13.0 / 1.8.9 |
| Material Components | 1.13.0 |
| lifecycle (viewmodel-ktx, runtime-ktx) | 2.10.0 |
| kotlinx-coroutines | 1.10.2 |
| Room (runtime, ktx, compiler przez KSP) | 2.8.4 |
| Navigation (fragment-ktx, ui-ktx) | 2.9.7 |
| Retrofit + converter-kotlinx-serialization | 3.0.0 |
| OkHttp + logging-interceptor + mockwebserver | 4.12.0 |
| kotlinx-serialization-json | 1.9.0 |
| WorkManager (work-runtime-ktx, work-testing) | 2.11.2 |
| Coil (View) | 2.7.0 |
| swiperefreshlayout / recyclerview / constraintlayout | 1.1.0 / 1.4.0 / 2.2.1 |
| JUnit 4 / Robolectric / androidx.test core | 4.13.2 / 4.16.1 / 1.7.0 |

Wszystkie wersje są w `gradle/libs.versions.toml`.

## Dla prowadzących: generowanie wersji startowej

Wersję startową generujemy z gałęzi `solution` — nie poprawiamy `main` ręcznie (poza `TASK.md`).
Każde rozwiązanie w kodzie jest otoczone markerami `// region SOLUTION TODO N` … `// endregion`,
a tuż za nim stoi zakomentowana zaślepka `// region STUB TODO N` … `// endregion`.

```bash
git checkout solution
python tools/make_starter.py --out ../gadzetownia-starter        # pełny starter (TODO 1–7)
python tools/make_starter.py --out ../check --solved 3           # starter z rozwiązaną tylko luką 3
```

Publikacja na `main` (katalog roboczy z gałęzią `main` obok repozytorium):

```bash
git worktree add ../gadzetownia-main main
python tools/make_starter.py --out ../gadzetownia-main
cd ../gadzetownia-main && git add -A && git commit -m "Starter wygenerowany z solution" && git push
```

Oczekiwany wynik testów: na `solution` wszystkie zielone; na `main` czerwone są tylko testy TODO 1–7
(`ProductApiTest`, `HeaderInterceptorTest`, `NetworkModuleTest`, `ProductRepositoryTest`, `SyncWorkerResultTest`,
`SyncRequestTest`, `SyncStatusMapperTest`, `SyncNowUniqueTest`, `PeriodicSyncTest`, `HttpCacheTest`),
a pozostałe (mappery, `AssetsInterceptor`, `ChaosInterceptor`, `CategoryRepository`) zielone.
