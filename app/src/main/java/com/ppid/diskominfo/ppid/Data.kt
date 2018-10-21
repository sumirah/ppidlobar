package com.ppid.diskominfo.ppid

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//data class Dinas(
//        val id:Int?=0,
//        val pendek:String?="",
//        val dinas:String?="",
//        val web:String?=""
//)

data class Dinas(
        val id:Int?=0,
        val judul_dip:String?="",
        val nama_dinas:String?="",
        val dokumen_dip:String?=""
)

@Parcelize
data class Download(
        var progress : Int? = 0,
        var currentFileSize : Int? = 0,
        var totalFileSize : Int? = 0
) : Parcelable

