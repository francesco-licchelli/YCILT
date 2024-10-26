package com.example.ycilt.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import com.example.ycilt.auth.LoginActivity
import com.example.ycilt.utils.Constants.BASE_URL
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.QueryMap
import retrofit2.http.Url
import java.io.File
import java.net.HttpURLConnection

interface ApiService {
    @GET
    fun getRequest(
        @Url url: String,
        @Header("Authorization") auth: String? = null,
        @QueryMap queryParams: Map<String, String>? = null
    ): Call<ResponseBody>

    @POST
    fun postRequest(
        @Url url: String,
        @Header("Authorization") auth: String? = null,
        @Body requestBody: RequestBody? = "".toRequestBody("application/json".toMediaType()),
    ): Call<ResponseBody>

    @POST
    @Multipart
    fun postRequest(
        @Url url: String,
        @Header("Authorization") auth: String? = null,
        @QueryMap queryParams: Map<String, String>? = mapOf(),
        @Part filePart: MultipartBody.Part
    ): Call<ResponseBody>

    @DELETE
    fun deleteRequest(
        @Url url: String,
        @Header("Authorization") auth: String? = null,
    ): Call<ResponseBody>
}

object NetworkUtils {
    private val client = OkHttpClient()
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    private val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    fun jsonEncoding(map: Map<String, String>): RequestBody {
        val body: RequestBody = map.let {
            val json = JSONObject(it).toString()
            json.toRequestBody("application/json".toMediaType())
        }
        return body
    }

    fun wwwEncoding(map: Map<String, String>): RequestBody {
        val body: RequestBody = map.let {
            val www = map.entries.joinToString(separator = "&") { (key, value) ->
                "$key=$value"
            }
            www.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        }
        return body
    }

    private fun reqToPair(response: retrofit2.Response<ResponseBody>): Pair<Int, String> {
        val responseCode = response.code()
        val responseBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""
        Log.d("NetworkUtils", "Response code: $responseCode")
        Log.d("NetworkUtils", "Response body: $responseBody")
        return Pair(responseCode, responseBody)
    }

    private fun checkToken(authRequest: Context?, response: Pair<Int, String>): Pair<Int, String> {
        if (authRequest != null &&
            response.first == HttpURLConnection.HTTP_UNAUTHORIZED &&
            response.second.contains("Token expired")
        ) {
            val intent = Intent(authRequest, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            authRequest.startActivity(intent)
        }
        return response
    }

    fun getRequest(
        endpoint: String,
        authRequest: Context? = null,
        queryParams: Map<String, String>? = mapOf(),
        isFullEndpoint: Boolean = false
    ): Pair<Int, String> {
        val clientSecret = authRequest?.getSharedPreferences("shared_prefs", MODE_PRIVATE)
            ?.getString("client_secret", null)
        return try {
            checkToken(
                authRequest,
                reqToPair(
                    apiService.getRequest(
                        if (isFullEndpoint) endpoint else "$BASE_URL$endpoint",
                        "Bearer $clientSecret",
                        queryParams
                    ).execute()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                mapOf("detail" to "An error occurred during the request").toString()
            )
        }
    }

    fun postRequest(
        endpoint: String,
        authRequest: Context,
        postData: Map<String, String>? = mapOf(),
        encode: (Map<String, String>) -> RequestBody = { jsonEncoding(it) }
    ): Pair<Int, String> {
        val clientSecret = authRequest.getSharedPreferences("shared_prefs", MODE_PRIVATE)
            ?.getString("client_secret", null)
        return try {
            checkToken(
                authRequest,
                reqToPair(
                    apiService.postRequest(
                        "$BASE_URL$endpoint",
                        "Bearer $clientSecret",
                        postData?.let { encode(it) }
                    ).execute()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                mapOf("detail" to "An error occurred during the request").toString()
            )
        }
    }


    fun postRequest(
        endpoint: String,
        authRequest: Context,
        queryParams: Map<String, String>,
        audio: File //carico solo audio/mpeg, non generalizzo
    ): Pair<Int, String> {
        val clientSecret = authRequest.getSharedPreferences("shared_prefs", MODE_PRIVATE)
            ?.getString("client_secret", null)
        return try {
            val filePart = MultipartBody.Part.createFormData(
                "file", audio.name,
                audio.asRequestBody("audio/mpeg".toMediaTypeOrNull())
            )
            checkToken(
                authRequest,
                reqToPair(
                    apiService.postRequest(
                        "$BASE_URL$endpoint",
                        "Bearer $clientSecret",
                        queryParams,
                        filePart
                    ).execute()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                mapOf("detail" to "An error occurred during the request").toString()
            )
        }
    }

    fun deleteRequest(
        endpoint: String,
        authRequest: Context,
    ): Pair<Int, String> {
        val clientSecret = authRequest.getSharedPreferences("shared_prefs", MODE_PRIVATE)
            ?.getString("client_secret", null)
        return try {
            checkToken(
                authRequest,
                reqToPair(
                    apiService.deleteRequest(
                        "$BASE_URL$endpoint",
                        "Bearer $clientSecret"
                    ).execute()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                mapOf("detail" to "An error occurred during the request").toString()
            )
        }
    }
}