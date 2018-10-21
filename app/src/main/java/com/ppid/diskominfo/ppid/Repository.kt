package com.ppid.diskominfo.ppid

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface gooRepository {
    //    @GET("/dinas/list")
    @GET("/folder/?????.php")
    fun getDinas(): Call<MutableList<Dinas>>

    @GET
    @Streaming
    fun donwloadFile(@Url url: String?): Call<ResponseBody>
}