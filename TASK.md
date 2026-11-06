# Zadanie Lab 3 — Gadżetownia

## 1. Cel

Aplikacja „Gadżetownia” to katalog sklepu z gadżetami działający **offline-first**: dane z API DummyJSON trafiają do Room,
a ekran zawsze czyta z Room. Twoim zadaniem jest dokończyć warstwę sieci i synchronizacji: zapytanie Retrofit,
interceptor i timeouty OkHttp, zapis API → Room z obsługą błędów, Workera z ponawianiem, planowanie pracy w WorkManagerze
oraz cache HTTP. Każda luka jest oznaczona w kodzie komentarzem `TODO(UCZESTNIK) N` i ma swój test-sprawdzarkę.

**Co działa od razu po uruchomieniu:** lista (pusta, dopóki nie zrobisz TODO 3), chipy kategorii (pobierane gotowym
kodem z `CategoryRepository`), nawigacja lista → szczegóły, ekran szczegółów, menu debug (źródło danych i chaos),
przycisk „Synchronizuj” (uruchamia Workera, który na razie nic nie pobiera).

**Czas:** ok. 45 min.
- **Część główna (zajęcia): TODO 1–5**, ok. 33 min.
- **Część druga (zajęcia lub dom): TODO 6–7**, ok. 12 min.

Luki są niezależne: kolejność 1 → 7 jest zalecana, ale nie wymagana. Po każdej luce aplikacja się kompiluje i działa.

## 2. Testy-sprawdzarki

Testy działają na JVM (bez emulatora) i **bez internetu** (MockWebServer, fixture'y JSON, Robolectric).

- Android Studio: otwórz klasę testu (np. `app/src/test/java/.../ProductApiTest.kt`) i kliknij zielony trójkąt
  przy nazwie klasy. Wszystkie testy naraz: prawy klik na katalogu `app/src/test/java` → *Run 'Tests in …'*.
- Terminal: `./gradlew testDebugUnitTest` (Windows: `gradlew.bat testDebugUnitTest`).
  Raport HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

Na starcie testy luk są **czerwone**. Komunikat błędu zaczyna się od numeru luki, np.

```
java.lang.AssertionError: TODO 3: odpowiedź 503 → RefreshResult.HttpError(503) (złap HttpException)
```

To znaczy: popraw lukę `TODO(UCZESTNIK) 3`. Testy części gotowych (mappery, `AssetsInterceptorTest`,
`ChaosInterceptorTest`, `CategoryRepositoryTest`) są zielone od początku i takie mają zostać.

> Pierwsze uruchomienie testów Robolectric (`SyncNowUniqueTest`, `PeriodicSyncTest`) pobiera obraz Androida —
> zrób to raz z internetem, np. na początku zajęć.

## 3. Mapa luk

| # | Plik (szukaj `TODO(UCZESTNIK) N`) | Co jest gotowe obok | Test | Jak sprawdzić w apce |
|---|---|---|---|---|
| 1 | `data/remote/ProductApi.kt` | `getCategories()`, `getProduct(id)`, stała `LIST_FIELDS` | `ProductApiTest` | po synchronizacji 194 produkty zamiast 30; w Logcat krótszy JSON |
| 2 | `data/remote/HeaderInterceptor.kt`, `data/remote/NetworkModule.kt` | kolejność interceptorów w `NetworkModule` | `HeaderInterceptorTest`, `NetworkModuleTest` | Logcat (filtr `okhttp`): nagłówek `X-Client: Gadzetownia/1.0` |
| 3 | `data/ProductRepository.kt` → `refreshProducts()` | `CategoryRepository.refreshCategories()`, mappery `toEntity()` | `ProductRepositoryTest` | lista się wypełnia; chaos 503 i „brak sieci” dają różne komunikaty |
| 4 | `sync/SyncWorker.kt` → `toWorkerResult()` | `errorData()`, `describeError()`, stałe `KEY_*` | `SyncWorkerResultTest` | chaos 503: „Błąd, ponowię próbę”, a po 3 próbach „nie powiodła się” |
| 5 | `sync/SyncScheduler.kt` (`buildSyncRequest()`, `syncNow()`), `sync/SyncStatusMapper.kt` | `networkConstraints`, `BACKOFF_SECONDS`, `core/SyncStatus.kt` | `SyncRequestTest`, `SyncNowUniqueTest`, `SyncStatusMapperTest` | pasek statusu ożywa; w trybie samolotowym „Czekam na połączenie z siecią…” |
| 6 | `sync/SyncScheduler.kt` → `schedulePeriodic()` | `networkConstraints`, `PERIODIC_HOURS`, `UNIQUE_PERIODIC` | `PeriodicSyncTest` | App Inspection → Background Task Inspector: jedna praca `product_sync_periodic` |
| 7 | `data/remote/CacheControlInterceptor.kt`, `data/remote/NetworkModule.kt` | `CACHE_SIZE_BYTES`, `MAX_AGE_SECONDS` | `HttpCacheTest` | drugie wejście w te same szczegóły nie wysyła żądania; szczegóły działają offline przez 5 min |

## 4. Zadania

### TODO 1 — zapytanie o listę produktów (Retrofit, `@Query`) · ~4 min

Bez parametrów DummyJSON zwraca tylko **30** produktów i wszystkie pola (opisy, recenzje…). Dodaj do `getProducts()`
dwa parametry zapytania z wartościami domyślnymi: `limit` = `0` (wszystkie produkty) i `select` = `LIST_FIELDS`
(tylko pola, które trzymamy w Room). Repozytorium woła `api.getProducts()` bez argumentów — tak ma zostać.

<details><summary>Podpowiedź 1</summary>

Parametr metody z adnotacją `@Query("nazwa")` dopisuje do adresu `?nazwa=wartość`.
</details>
<details><summary>Podpowiedź 2</summary>

Kotlin pozwala na wartości domyślne parametrów także w interfejsie Retrofit: `limit: Int = 0`.
</details>
<details><summary>Podpowiedź 3</summary>

```kotlin
@GET("products")
suspend fun getProducts(
    @Query("limit") limit: Int = 0,
    @Query("select") select: String = LIST_FIELDS,
): ProductsResponseDto
```
</details>

**W apce:** po rozwiązaniu TODO 3 lista ma 194 produkty (z samym TODO 1 nic jeszcze nie widać, bo repozytorium nie pobiera danych).

### TODO 2 — interceptor nagłówków i timeouty (OkHttp) · ~5 min

1. `HeaderInterceptor.intercept`: dodaj do każdego żądania `X-Client: Gadzetownia/<versionName>` i `Accept: application/json`.
2. `NetworkModule.createOkHttpClient(cacheDir, …)`: ustaw timeouty `connect` 10 s, `read` 15 s, `write` 15 s.

<details><summary>Podpowiedź 1</summary>

Obiekt `Request` jest niezmienny — trzeba zbudować jego kopię: `chain.request().newBuilder()`.
</details>
<details><summary>Podpowiedź 2</summary>

`.header(nazwa, wartość)` ustawia nagłówek, `.build()` tworzy nowe żądanie, `chain.proceed(nowe)` wysyła je dalej.
Timeouty: `builder.connectTimeout(10, TimeUnit.SECONDS)` i analogicznie `readTimeout`, `writeTimeout`.
</details>
<details><summary>Podpowiedź 3</summary>

```kotlin
val request = chain.request().newBuilder()
    .header(HEADER_CLIENT, "$CLIENT_NAME/$clientVersion")
    .header("Accept", "application/json")
    .build()
return chain.proceed(request)
```
</details>

**W apce:** Logcat, filtr `okhttp` → przy żądaniu widać `X-Client: Gadzetownia/1.0`.

### TODO 3 — API → Room i obsługa błędów · ~8 min

`ProductRepository.refreshProducts()` ma pobrać produkty, zapisać je w Room i zwrócić **co się stało**:
`Success(liczba)`, `HttpError(kod)` (serwer odpowiedział błędem) albo `NetworkError(wyjątek)` (nie udało się połączyć).
Przy błędzie baza ma zostać nietknięta — dzięki temu lista dalej pokazuje stare dane.

<details><summary>Podpowiedź 1</summary>

Zobacz gotowe `CategoryRepository.refreshCategories()` — ten sam schemat: API → `map { it.toEntity(...) }` → `dao.upsertAll(...)`.
</details>
<details><summary>Podpowiedź 2</summary>

Retrofit rzuca `retrofit2.HttpException` przy kodzie 4xx/5xx (kod: `e.code()`), a `java.io.IOException` przy problemach
z siecią (brak połączenia, timeout). Złap je w dwóch osobnych `catch`. **Nie** łap ogólnego `Exception`.
</details>
<details><summary>Podpowiedź 3</summary>

```kotlin
return try {
    val entities = api.getProducts().products.map { it.toEntity(updatedAt = clock()) }
    dao.upsertAll(entities)
    RefreshResult.Success(entities.size)
} catch (e: HttpException) {
    RefreshResult.HttpError(e.code())
} catch (e: IOException) {
    RefreshResult.NetworkError(e)
}
```
</details>

**W apce:** „Synchronizuj” → lista się wypełnia. Menu ⋮ → Chaos → `HTTP_503` albo `NO_NETWORK` → lista zostaje z danymi.

### TODO 4 — wynik Workera: success / retry / failure · ~7 min

`toWorkerResult(runAttemptCount)` decyduje, co WorkManager ma zrobić z wynikiem synchronizacji:

| Wynik | Decyzja |
|---|---|
| `Success` | `Result.success(workDataOf(KEY_COUNT to count, KEY_FINISHED_AT to finishedAt))` |
| `NetworkError` | `Result.retry()` |
| `HttpError` 5xx | `Result.retry()` |
| `HttpError` 4xx | `Result.failure(errorData())` — ponawianie nic nie da |
| błąd na ostatniej próbie (`runAttemptCount >= MAX_ATTEMPTS - 1`) | `Result.failure(errorData())` |

<details><summary>Podpowiedź 1</summary>

`runAttemptCount` liczy się od 0. Przy `MAX_ATTEMPTS = 3` ostatnia próba ma numer 2.
</details>
<details><summary>Podpowiedź 2</summary>

Użyj `when (this) { is RefreshResult.Success -> …; is RefreshResult.NetworkError -> …; is RefreshResult.HttpError -> … }`.
</details>
<details><summary>Podpowiedź 3</summary>

```kotlin
val isLastAttempt = runAttemptCount >= SyncScheduler.MAX_ATTEMPTS - 1
return when (this) {
    is RefreshResult.Success -> ListenableWorker.Result.success(
        workDataOf(SyncWorker.KEY_COUNT to count, SyncWorker.KEY_FINISHED_AT to finishedAt),
    )
    is RefreshResult.NetworkError ->
        if (isLastAttempt) ListenableWorker.Result.failure(errorData()) else ListenableWorker.Result.retry()
    is RefreshResult.HttpError ->
        if (code >= 500 && !isLastAttempt) ListenableWorker.Result.retry() else ListenableWorker.Result.failure(errorData())
}
```
</details>

**W apce (razem z TODO 5):** Chaos `HTTP_503` → „Błąd, ponowię próbę (próba 2)” → po trzech próbach
„Synchronizacja nie powiodła się: …” i Snackbar „Ponów”.

### TODO 5 — żądanie pracy, unikalność i status w UI · ~9 min

Trzy małe części:
1. `SyncScheduler.buildSyncRequest()`: wymóg sieci (`setConstraints(networkConstraints)`) i wykładniczy backoff
   (`setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)`).
2. `SyncScheduler.syncNow()`: kolejne kliknięcia nie mogą dublować ani przerywać pracy, która już czeka lub trwa.
3. `SyncStatusMapper.kt`: zamień `WorkInfo` na `SyncStatus` (tabela w komentarzu przy luce).

<details><summary>Podpowiedź 1</summary>

`ExistingWorkPolicy.REPLACE` anuluje istniejącą pracę i wstawia nową; `KEEP` zostawia istniejącą niezakończoną pracę.
</details>
<details><summary>Podpowiedź 2</summary>

Stan pracy to `this.state` (`WorkInfo.State.ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `BLOCKED`).
Dane wyjściowe Workera: `outputData.getInt(SyncWorker.KEY_COUNT, 0)`, `outputData.getString(SyncWorker.KEY_ERROR)`.
</details>
<details><summary>Podpowiedź 3</summary>

```kotlin
if (this == null) return SyncStatus.Idle(lastSync)
return when (state) {
    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
        if (runAttemptCount == 0) SyncStatus.WaitingForNetwork else SyncStatus.Retrying(runAttemptCount + 1)
    WorkInfo.State.RUNNING -> SyncStatus.Running
    WorkInfo.State.SUCCEEDED -> SyncStatus.Succeeded(
        outputData.getInt(SyncWorker.KEY_COUNT, lastSync?.count ?: 0),
        outputData.getLong(SyncWorker.KEY_FINISHED_AT, lastSync?.at ?: 0L),
    )
    WorkInfo.State.FAILED -> SyncStatus.Failed(outputData.getString(SyncWorker.KEY_ERROR) ?: "nieznany błąd")
    WorkInfo.State.CANCELLED -> SyncStatus.Idle(lastSync)
}
```
</details>

**W apce:** pasek pod toolbarem pokazuje „Synchronizuję…” i „Zsynchronizowano 194 produkty o …”.
Tryb samolotowy → „Synchronizuj” → „Czekam na połączenie z siecią…”; po włączeniu sieci praca rusza sama.
Kilka szybkich kliknięć → jedna praca.

### TODO 6 — synchronizacja okresowa · ~6 min (część druga)

`schedulePeriodic()` (wołane w `CatalogApp.onCreate`) ma zaplanować **jedną** pracę okresową `UNIQUE_PERIODIC`
co 12 godzin, tylko przy dostępnej sieci. Restart aplikacji nie może tworzyć kolejnej pracy.

<details><summary>Podpowiedź 1</summary>

`PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)` + `setConstraints(networkConstraints)` + `build()`.
</details>
<details><summary>Podpowiedź 2</summary>

Do pracy okresowej służy `workManager.enqueueUniquePeriodicWork(nazwa, polityka, żądanie)`.
Polityka `ExistingPeriodicWorkPolicy.KEEP` zostawia już zaplanowaną pracę.
</details>

**W apce:** App Inspection → Background Task Inspector → jedna praca `product_sync_periodic` (także po kilku restartach).

### TODO 7 — cache HTTP · ~6 min (część druga)

DummyJSON nie wysyła nagłówków cache, więc OkHttp niczego nie zapamiętuje. Włącz cache dla szczegółów produktu:
1. `CacheControlInterceptor`: dla `GET products/{id}` zwróć kopię odpowiedzi z nagłówkiem
   `Cache-Control: public, max-age=300` (listy nie buforujemy).
2. `NetworkModule`: dodaj `CacheControlInterceptor` jako **network interceptor** i ustaw `Cache(cacheDir, CACHE_SIZE_BYTES)`.

<details><summary>Podpowiedź 1</summary>

Odpowiedź też jest niezmienna: `response.newBuilder().header("Cache-Control", "...").build()`.
Ścieżka: `chain.request().url.pathSegments` → np. `["products", "1"]`.
</details>
<details><summary>Podpowiedź 2</summary>

`addInterceptor` działa przed cache (widzi odpowiedź już z cache), `addNetworkInterceptor` — między cache a siecią,
więc tylko on może „dopisać” nagłówek, który cache zobaczy przy zapisie.
</details>
<details><summary>Podpowiedź 3</summary>

```kotlin
builder.addNetworkInterceptor(CacheControlInterceptor())
if (cacheDir != null) builder.cache(Cache(cacheDir, CACHE_SIZE_BYTES))
```
</details>

**W apce:** wejdź w szczegóły, wróć i wejdź ponownie → w Logcat drugie wejście nie wysyła `--> GET`.
Włącz tryb samolotowy → szczegóły tego produktu dalej mają opis (przez 5 minut).

## 5. Do przemyślenia

- Dlaczego UI czyta z Room, a nie bezpośrednio z API? Co widzi użytkownik, gdy synchronizacja się nie uda?
- Dlaczego błędu 4xx nie ponawiamy, a 5xx i błędy sieci tak?
- `ExistingWorkPolicy.KEEP` vs `REPLACE`: kiedy które ma sens? Co by się stało przy `REPLACE` i 5 szybkich kliknięciach?
- Po co wymóg sieci w Workerze, skoro i tak łapiemy `IOException`?
- Dlaczego kategorie odświeżamy bez WorkManagera, a produkty przez WorkManagera?
- Dlaczego `Cache-Control` ustawiamy tylko dla szczegółów, a nie dla listy?

## 6. Najczęstsze błędy

- `catch (e: Exception)` zamiast konkretnych typów: łapie też `CancellationException` i psuje anulowanie korutyn
  (np. przy wyjściu z ekranu), a do tego nie odróżnisz błędu serwera od braku sieci.
- `addInterceptor` zamiast `addNetworkInterceptor` przy `CacheControlInterceptor` — cache nie zobaczy nagłówka.
- Zapomniany constraint sieci — Worker startuje bez internetu i od razu zużywa próby.
- Zwykłe `enqueue(...)` zamiast `enqueueUniqueWork(...)` — każde kliknięcie tworzy nową pracę, a pasek statusu
  (obserwujący unikalną nazwę) przestaje działać.
- `Result.failure()` dla błędu sieci — WorkManager nie ponowi pracy, choć problem był chwilowy.
- Modyfikowanie `chain.request()` bez `newBuilder()` — `Request` jest niezmienny.

## 7. Gdy DummyJSON nie działa: tryb `OFFLINE_ASSETS`

Zadanie i testy **nie zależą od internetu**. W aplikacji wybierz menu ⋮ → *Źródło danych* → *Pliki z assets
(OFFLINE_ASSETS)*. `AssetsInterceptor` odpowie plikami z `app/src/main/assets/api/` (te same dane co w API, z obsługą
`limit` i `select`, więc efekt TODO 1 też widać), a żądania nie wyjdą do sieci. Uwaga: przy wymogu sieci (TODO 5)
Worker i tak czeka na połączenie — w emulatorze sieć zwykle jest, więc tryb assets wystarczy.
Miniatury produktów (Coil) pobierają się z sieci, więc offline zobaczysz placeholdery.
