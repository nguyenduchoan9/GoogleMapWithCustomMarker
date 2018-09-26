package solution.com.googlemap

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitUtils {
    companion object {
        private val API_ENDPOINT = "https://codetest18292.mvlchain.io/"

        val retrofit: Retrofit by lazy {
            create()
        }

        private fun create(): Retrofit {
            return Retrofit.Builder()
                    .baseUrl(API_ENDPOINT)
                    .client(client())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }

        private fun client(): OkHttpClient {
            return OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor())
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()
        }

        private fun loggingInterceptor(): Interceptor{
            return  HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
        }
    }
}