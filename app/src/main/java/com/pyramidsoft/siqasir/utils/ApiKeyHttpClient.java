package com.pyramidsoft.siqasir.utils;

import com.pyramidsoft.siqasir.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * AndroidApiKeyInterceptor
 *
 * OkHttp Interceptor yang secara otomatis menambahkan header X-Android-Key
 * ke semua request yang dikirim ke server siqasir.
 *
 * CARA PENGGUNAAN:
 * Ganti semua instansiasi OkHttpClient biasa:
 *
 *   // LAMA (tidak aman):
 *   private OkHttpClient client = new OkHttpClient();
 *
 *   // BARU (dengan API key):
 *   private OkHttpClient client = ApiKeyHttpClient.get();
 *
 * Atau cukup ganti di setiap Activity/Fragment yang menggunakan OkHttpClient.
 *
 * CARA SET API KEY:
 * Tambahkan di app/build.gradle:
 *
 *   android {
 *       defaultConfig {
 *           buildConfigField "String", "ANDROID_API_KEY", '"isi_key_dari_env_di_sini"'
 *       }
 *   }
 *
 * Ganti "isi_key_dari_env_di_sini" dengan nilai ANDROID_API_KEY dari .env Laravel.
 * PENTING: Jangan commit file build.gradle dengan key yang sudah diisi ke repository publik.
 * Gunakan local.properties atau CI/CD secrets untuk menyimpan nilai key.
 */
public class ApiKeyHttpClient {

    private static OkHttpClient instance;

    private ApiKeyHttpClient() {}

    /**
     * Mengembalikan singleton OkHttpClient yang sudah dikonfigurasi dengan
     * interceptor untuk menambahkan header X-Android-Key secara otomatis.
     */
    public static synchronized OkHttpClient get() {
        android.util.Log.d("API_KEY_CHECK", "Key: " + BuildConfig.ANDROID_API_KEY);
        if (instance == null) {
            instance = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .addInterceptor(new ApiKeyInterceptor())
                    .build();
        }
        return instance;
    }

    /**
     * Interceptor internal yang menambahkan header X-Android-Key ke setiap request.
     */
    private static class ApiKeyInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            String url = originalRequest.url().toString();
            String apiKey = BuildConfig.ANDROID_API_KEY;

            android.util.Log.d("API_INTERCEPTOR", "Request ke: " + url);
            android.util.Log.d("API_INTERCEPTOR", "Mengirim API Key: " + (apiKey != null ? "ADA (panjang: " + apiKey.length() + ")" : "KOSONG"));
    
            Request requestWithKey = originalRequest.newBuilder()
                    .header("X-Android-Key", apiKey)
                    .header("X-Api-Key", apiKey)
                    .header("X-App-Version", String.valueOf(BuildConfig.VERSION_CODE))
                    .header("Accept", "application/json")
                    .build();

            Response response = chain.proceed(requestWithKey);

            if (!response.isSuccessful()) {
                android.util.Log.e("API_INTERCEPTOR", "Response Gagal (" + response.code() + ") untuk: " + url);
            }

            return response;
        }
    }
}