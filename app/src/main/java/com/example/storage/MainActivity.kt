package com.example.storage

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*


class MainActivity : AppCompatActivity() {

    var sp: SharedPreferences? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp = getSharedPreferences("DirPermission", Context.MODE_PRIVATE)

        btnCreate.setOnClickListener {
            createNewFile()
//            val checkPermission = ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            )
//            if (checkPermission == PackageManager.PERMISSION_GRANTED) {
//                createNewFile()
//            } else {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE
//                    ),
//                    100
//                )
//            }
        }

        btnCopy.setOnClickListener {
            insertNoMediaFolder()

            copyFileToDownloads()
        }

        btnTrans.setOnClickListener {
            transFile()
        }

        btnGetAllowed.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 101)
        }

        btnWrite.setOnClickListener {
            val uriStr = sp?.getString("rootUriTree", null)
            try {
                val uriTree = Uri.parse(uriStr)
                saveUriTreePermission(uriTree, intent.flags)
                createFileToExternalDir(uriTree)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "未申請權限", Toast.LENGTH_SHORT).show()
            }
        }

        btnRead.setOnClickListener {
            val uriStr = sp?.getString("rootUriTree", null)
            try {
                val uriTree = Uri.parse(uriStr)
                saveUriTreePermission(uriTree, intent.flags)
                readExternalFile(uriTree)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "未申請權限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun transFile() {
        val cursor =
            contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.MIME_TYPE
                ),
                "${MediaStore.Downloads.DISPLAY_NAME} like '%temp copy%'",
                null, null
            )
        if (cursor != null && cursor.moveToFirst()) {
            //媒体数据库中查询到的文件id
//            val columnId = cursor.getColumnIndex(MediaStore.Downloads._ID)
            do {
                //通过mediaId获取它的uri
//                val mediaId = cursor.getInt(columnId)
//                val uri =
//                    Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "$mediaId")

//                val displayName =
//                    cursor.getString(cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME))
//                val mime_type =
//                    cursor.getString(cursor.getColumnIndex(MediaStore.Downloads.MIME_TYPE))

                val uri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    cursor.getLong(0)
                )
                val displayName = cursor.getString(1)
                val mime_type = cursor.getString(2)
                println("======> $displayName    $mime_type     $uri")

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null
                try {
                    inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        outputStream =
                            FileOutputStream("${getExternalFilesDir("retemp")}/$displayName")
                        val buffer = ByteArray(4096)
                        var byteCount = 0
                        while (inputStream.read(buffer).also { byteCount = it } != -1) {
                            outputStream.write(buffer, 0, byteCount)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } while (cursor.moveToNext())
        }
        cursor?.close()
    }

    private fun createNewFile() {
        val path = "${getExternalFilesDir("Temp")}/temp.txt"
        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
        }
        try {
            val fileWriter = FileWriter(path)
            fileWriter.write("卡莎刷卡看到奧斯卡多久啊")
            fileWriter.close()
            Toast.makeText(this, "文件創建成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 插入.nomedia文件夾屏蔽系統掃描 useless
     *
     * java.lang.IllegalArgumentException:
     * Primary directory (invalid) not allowed for
     * content://media/external/downloads; allowed directories are [Download]
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertNoMediaFolder() {
        //1、检测是否已存在
        val cursor = contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            null,
            "${MediaStore.Downloads.DISPLAY_NAME} = '%nomdeia%'",
            null, null
        )
        if (cursor == null || !cursor.moveToFirst()) {
            try {
                val values = ContentValues()
                values.put(MediaStore.Downloads.RELATIVE_PATH, "/Temp/.nomedia")
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        cursor?.close()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyFileToDownloads() {
        val values = ContentValues()
        values.put(MediaStore.Downloads.DISPLAY_NAME, "temp copy1.txt")
        values.put(MediaStore.Downloads.TITLE, "temp copy1.txt")
        values.put(MediaStore.Downloads.MIME_TYPE, "*/*")
        values.put(
            MediaStore.Downloads.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/Temp"
        )

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var copyResult = false
        try {
            inputStream = FileInputStream(File("${getExternalFilesDir("Temp")}/temp.txt"))
            if (uri != null) {
                outputStream = contentResolver.openOutputStream(uri)
            }
            if (outputStream != null) {
                val buffer = ByteArray(4096)
                var byteCount = 0
                while (inputStream.read(buffer).also { byteCount = it } != -1) {
                    outputStream.write(buffer, 0, byteCount)
                }
            }
            copyResult = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            outputStream?.close()
            Toast.makeText(this, "拷貝${if (copyResult) "成功" else "失败"}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 向外部文件夹写入文件
     */
    private fun createFileToExternalDir(uriTree: Uri) {
        val rootDocument =
            DocumentFile.fromTreeUri(this, uriTree)
        var tempDocument: DocumentFile? = null
        rootDocument?.listFiles()?.forEach {
            if (it.name.equals("Temp"))
                tempDocument = it
        }
        if (tempDocument == null) {
            tempDocument = rootDocument?.createDirectory("Temp")
        }
        val tempFile =
            tempDocument?.createFile("*/*", "temp")
        tempFile?.let { it ->
            contentResolver.openFileDescriptor(it.uri, "rw")?.let { pfd ->
                val fileWriter = FileWriter(pfd.fileDescriptor)
                fileWriter.write("我就試試行不行")
                fileWriter.close()
                Toast.makeText(this, "写入成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 读取外部文件内容
     */
    private fun readExternalFile(uriTree: Uri) {
        val rootDocument =
            DocumentFile.fromTreeUri(this, uriTree)
        var tempDocument: DocumentFile? = null
        rootDocument?.listFiles()?.forEach {
            if (it.name.equals("Temp"))
                tempDocument = it
        }
        if (tempDocument == null) {
            Toast.makeText(this, "未找到相关文件夹", Toast.LENGTH_SHORT).show()
            return
        }
        tempDocument?.findFile("temp")?.let {
            val inputStream = contentResolver.openInputStream(it.uri)?.let { input ->
                var bytes = ByteArray(0)
                bytes = ByteArray(input.available())
                input.read(bytes)
                Toast.makeText(this, String(bytes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 保存申请的权限
     */
    private fun saveUriTreePermission(uri: Uri, flags: Int) {
        //获取永久访问权限(可手动取消)
        val takeFlags = (flags
                and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        //可将uri以sp的形式保存，以后可直接使用
        val editor = sp?.edit();
        editor?.putString("rootUriTree", uri.toString())
        editor?.apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            //此处获取的权限只持续到设备下次重启
            data?.data?.let {
                saveUriTreePermission(it, data.flags)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        if (requestCode == 100) {
//            var grant = true
//            grantResults.forEach {
//                grant = it == PackageManager.PERMISSION_GRANTED && grant
//            }
//            if (grant) {
//                createNewFile()
//            } else {
//                Toast.makeText(this, "權限未獲取", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        }
//    }
}
