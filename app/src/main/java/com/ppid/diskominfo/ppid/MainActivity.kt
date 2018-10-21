package com.ppid.diskominfo.ppid


import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.support.design.widget.Snackbar
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.widget.ImageView


class MainActivity : AppCompatActivity() {

    private val list = mutableListOf<Dinas>()
    private val listCari = mutableListOf<Dinas>()
    private val listTmp = mutableListOf<Dinas>()
    private var adapter: DinasAdapter? = null

    companion object {
        val MESSAGE_PROGRESS = "message_progress"
    }

    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Membuat Toolbar / Header
        setSupportActionBar(tool_bar)
        tool_bar.setNavigationIcon(R.mipmap.ic_launcher)
        tool_bar.setNavigationOnClickListener {
            img_logo.visibility = View.VISIBLE
            adapter?.removeAllItem()
            adapter?.setFilter(listTmp)
        }
        this.title = "PPID Lombok Barat"

        //Set Image Logo
        Glide.with(this).load(R.drawable.logo).into(img_logo)

        //Recycle View / List Data
        rc_view.layoutManager = LinearLayoutManager(this)
        adapter = DinasAdapter(list) { dinas ->
            startDownload(dinas.dokumen_dip.toString())
//            Toast.makeText(this, dinas.dokumen_dip.toString(), Toast.LENGTH_LONG).show()
        }

        rc_view.adapter = adapter

        val service = Network.retrofit.create(Repository::class.java)
        service.getDinas().enqueue(object : Callback<MutableList<Dinas>> {
            override fun onFailure(call: Call<MutableList<Dinas>>?, t: Throwable?) {
                Log.e("Error", t?.message)
            }

            override fun onResponse(call: Call<MutableList<Dinas>>?, response: Response<MutableList<Dinas>>?) {

                //untuk progress loading
                rc_view.visibility = View.VISIBLE
                progress_bar.visibility = View.GONE


                response?.body()?.let {
                    listTmp.addAll(it)
                    adapter?.addItem(it)
                }
            }

        })

        registerReceiver()

        if (!checkPermission()) {
            requestPermission()
        }

        button_cari.setOnClickListener {
            val key = txt_cari.text.toString()
            cari(key)
        }
    }

    fun downloadFile(url: String) {

        if (checkPermission()) {
            startDownload(url)
        } else {
            requestPermission()
        }
    }

    private fun startDownload(url: String) {

        val intent = Intent(this, DownloadService::class.java)
        intent.putExtra("FILE_URL", url)
        startService(intent)

    }

    private fun registerReceiver() {

        val bManager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter();
        intentFilter.addAction(MESSAGE_PROGRESS);
        bManager.registerReceiver(broadcastReceiver, intentFilter);

    }

    //untuk android 6+
    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return if (result == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            false
        }
    }

    //tidak dipanggil / tidak dipakai
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == MESSAGE_PROGRESS) {

                val (progress, currentFileSize, totalFileSize) = intent.getParcelableExtra<Download>("download")
                Log.e("PROGRESS", progress.toString())
            }
        }
    }

    private fun requestPermission() {

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission Denied, Please allow to proceed !", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun cari(key: String) {
        listCari.addAll(listTmp)

        img_logo.visibility = View.GONE
        val filteredList = mutableListOf<Dinas>()
        listCari.forEach {
            var query = "${it.judul_dip}${it.nama_dinas}"
            if (query.toLowerCase().contains(key)) {
                filteredList.add(it)
            }
        }

        adapter?.removeAllItem()
        adapter?.setFilter(filteredList)
    }
}

class DinasAdapter(private val list: MutableList<Dinas>, private val listener: (Dinas) -> Unit) : RecyclerView.Adapter<DinasAdapter.DinasViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DinasAdapter.DinasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dinas_item, parent, false)
        return DinasViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: DinasAdapter.DinasViewHolder, position: Int) {
        holder.setData(list[position], listener)
    }

    fun addItem(items: MutableList<Dinas>) {
        list.addAll(items)
        notifyDataSetChanged()
    }

    fun removeAllItem() {
        list.clear()
        notifyDataSetChanged()
    }

    fun setFilter(listFilter: MutableList<Dinas>) {
        list.addAll(listFilter)
        notifyDataSetChanged()
    }


    class DinasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtJudulDip = view.findViewById<TextView>(R.id.txt_judul_dip)
        val txtNamaDinas = view.findViewById<TextView>(R.id.txt_nama_dinas)
        val btnDonwload = view.findViewById<ImageView>(R.id.img_download)

        fun setData(dinas: Dinas, listener: (Dinas) -> Unit) {
            txtJudulDip.text = dinas.judul_dip
            txtNamaDinas.text = dinas.nama_dinas

            btnDonwload.setOnClickListener {
                listener(dinas)
            }
        }
    }

}