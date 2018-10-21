package com.ppid.diskominfo.ppid

import android.app.IntentService
import android.content.Context.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.support.v4.app.NotificationCompat
import okhttp3.ResponseBody
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import android.support.v4.content.LocalBroadcastManager
import android.R.string.cancel
import android.R.string.cancel
import android.widget.Toast
import android.os.AsyncTask.execute
import java.io.IOException
import android.content.ActivityNotFoundException
import android.app.PendingIntent
import android.net.Uri
import android.webkit.MimeTypeMap


class DownloadService : IntentService("Download Service") {
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var totalFileSize: Int = 0

    override fun onHandleIntent(intent: Intent?) {

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_file_download_black_24dp)
                .setContentTitle("Download")
                .setContentText("Downloading File")
                .setAutoCancel(true)
        notificationManager!!.notify(0, notificationBuilder!!.build())

        val url = intent?.getStringExtra("FILE_URL")
        val name = url?.substringAfter("../fileppid/")
        val validEndpoint = url?.replace("..", "")
        val validUrl = Network.BASE_URL + validEndpoint

        initDownload(validUrl, name.toString())
    }

    private fun initDownload(url: String, fileName: String) {
        val service = Network.retrofit.create(Repository::class.java)
        val request = service.donwloadFile(url)

        try {

            downloadFile(request.execute().body(), fileName)

        } catch (e: IOException) {

            e.printStackTrace()
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()

        }

    }

    private fun downloadFile(body: ResponseBody?, fileName: String) {
        var count: Int = 0
        val data = ByteArray(1024 * 4)
        val fileSize = body?.contentLength()
        val bis = BufferedInputStream(body?.byteStream(), 1024 * 8)
        val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        val output = FileOutputStream(outputFile)
        var total: Long = 0
        val startTime = System.currentTimeMillis()
        var timeCount = 1

        while (true) {
            count = bis.read(data)
            if (count == -1) {
                break
            }
            total += count.toLong()
            totalFileSize = (fileSize!! / Math.pow(1024.0, 2.0)).toInt()
            val current = Math.round(total / Math.pow(1024.0, 2.0)).toDouble()

            val progress = (total * 100 / fileSize).toInt()

            val currentTime = System.currentTimeMillis() - startTime

            val download = Download()
            download.totalFileSize = totalFileSize

            if (currentTime > 1000 * timeCount) {

                download.currentFileSize = current.toInt()
                download.progress = progress
                sendNotification(download)
                timeCount++
            }

            output.write(data, 0, count)
        }
        onDownloadComplete(fileName)
        output.flush()
        output.close()
        bis.close()
    }

    private fun sendNotification(download: Download) {

        sendIntent(download)
        notificationBuilder?.setProgress(100, download.progress!!, false)
        notificationBuilder?.setContentText("Downloading file " + download.currentFileSize + "/" + totalFileSize + " MB")
        notificationManager?.notify(0, notificationBuilder?.build())
    }

    private fun sendIntent(download: Download) {

        val intent = Intent(MainActivity.MESSAGE_PROGRESS)
        intent.putExtra("download", download)
        LocalBroadcastManager.getInstance(this@DownloadService).sendBroadcast(intent)
    }

    private fun onDownloadComplete(fileName: String) {

        val download = Download()
        download.progress = 100
        sendIntent(download)

        notificationManager?.cancel(0)
        notificationBuilder?.setProgress(0, 0, false)
        notificationBuilder?.setContentText("${fileName} Downloaded")
        val notif = notificationBuilder?.build()
        notif?.flags = NotificationCompat.FLAG_AUTO_CANCEL

        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            intent.setDataAndType(Uri.fromFile(file), mimeType)

            val pIntent = PendingIntent.getActivity(applicationContext, 0,
                    intent, 0)

            notif?.contentIntent = pIntent
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(applicationContext, "Tidak ada aplikasi untuk membuat file ini", Toast.LENGTH_LONG).show()
        }

        notificationManager?.notify(0, notif)

    }

    override fun onTaskRemoved(rootIntent: Intent) {
        notificationManager?.cancel(0)
    }
}