package com.example.catalog.data

import java.io.IOException

/** Wynik odświeżenia produktów: rozróżniamy błąd serwera (kod HTTP) od błędu sieci. */
sealed interface RefreshResult {
    data class Success(val count: Int) : RefreshResult

    /** Serwer odpowiedział, ale kodem błędu (Retrofit: HttpException). */
    data class HttpError(val code: Int) : RefreshResult

    /** Nie udało się porozmawiać z serwerem: brak sieci, timeout, zerwane połączenie (IOException). */
    data class NetworkError(val cause: IOException) : RefreshResult
}
