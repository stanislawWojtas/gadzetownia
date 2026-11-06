package com.example.catalog.data.remote

import com.example.catalog.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Dodaje do każdego żądania nagłówki aplikacji:
 * `X-Client: Gadzetownia/<versionName>` oraz `Accept: application/json`.
 */
class HeaderInterceptor(
    private val clientVersion: String = BuildConfig.VERSION_NAME,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // region SOLUTION TODO 2
        val request = chain.request().newBuilder()
            .header(HEADER_CLIENT, "$CLIENT_NAME/$clientVersion")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
        // endregion
        // region STUB TODO 2
        // // TODO(UCZESTNIK) 2: Dodaj nagłówki X-Client ("$CLIENT_NAME/$clientVersion") i Accept ("application/json").
        // // Żądania są niezmienne: zbuduj kopię przez chain.request().newBuilder().header(...).build()
        // // i przekaż ją dalej przez chain.proceed(...).
        // return chain.proceed(chain.request())
        // endregion
    }

    companion object {
        const val HEADER_CLIENT = "X-Client"
        const val CLIENT_NAME = "Gadzetownia"
    }
}
