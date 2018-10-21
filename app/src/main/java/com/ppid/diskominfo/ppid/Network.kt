package com.ppid.diskominfo.ppid

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Network {
    val BASE_URL = "http://ppid.lombokbaratkab.go.id"
    val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
//            .baseUrl("http://192.168.1.10:8000")
            .baseUrl(BASE_URL)
            .build()
}
