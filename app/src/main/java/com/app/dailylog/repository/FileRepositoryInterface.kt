package com.app.dailylog.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.app.dailylog.R
import com.app.dailylog.ui.permissions.PermissionChecker
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.*
import java.lang.Exception
import java.math.BigInteger
import java.security.MessageDigest



interface FileRepositoryInterface {
    var filename: String
    val context: Context
    val permissionChecker: PermissionChecker
    var lastSavedContentsHash: String

    fun initializeFilename() {
        filename = retrieveFilename()
    }

    fun userHasSelectedFile(): Boolean {
        return filename == Constants.NO_FILE_SELECTED
    }

    fun retrieveFilename(): String {
        val preferences =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )
        return preferences.getString(Constants.FILENAME_PREF_KEY, Constants.NO_FILE_SELECTED)
            ?: Constants.NO_FILE_SELECTED
    }

    fun storeFilename(filename: String) {
        val preferences =
            context.getSharedPreferences(
                context.getString(
                    R.string.preference_file_key
                ), Context.MODE_PRIVATE
            )
        val editor = preferences.edit()
        editor.putString(Constants.FILENAME_PREF_KEY, filename)
        editor.apply()
        this.filename = filename
    }

    fun readFile(firstTime: Boolean): String {
        if (permissionChecker.requestPermissionsBasedOnAppVersion()) {
            try {
                val stringBuilder = StringBuilder()
                val uri = Uri.parse(filename)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            stringBuilder.append(System.lineSeparator())
                            line = reader.readLine()
                        }
                    }
                    inputStream.close()
                }
                val fileData = stringBuilder.toString()
                if (firstTime) {
                    updateLastSavedHash(fileData)
                }
                return fileData
            } catch (ex: Exception) {
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG)
                    .show()
            }
        }
        Toast.makeText(context, "File read permissions not yet granted.", Toast.LENGTH_LONG).show()
        return ""
    }

    private fun md5(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hash = BigInteger(1, md.digest(data.toByteArray())).toString(16)
        return hash
    }

    private fun updateLastSavedHash(data: String) {
        lastSavedContentsHash = md5(data)
    }

    private fun shouldSave(data: String): Boolean {
        return md5(data) != lastSavedContentsHash
    }

    fun saveToFile(data: String, overrideSmartSave: Boolean): Boolean {
        if (!overrideSmartSave && !shouldSave(data)) {
            return false
        }
        if (permissionChecker.requestPermissionsBasedOnAppVersion()) {
            return try {
                val uri = Uri.parse(filename)
                val openFileDescriptor = context.contentResolver.openFileDescriptor(uri, "rwt")
                val fileDescriptor = openFileDescriptor?.fileDescriptor
                val fileStream = FileOutputStream(fileDescriptor)
                fileStream.write((data).toByteArray())
                fileStream.close()
                openFileDescriptor?.close()
                updateLastSavedHash(data)
                true
            } catch (ex: IllegalArgumentException) {
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                false
            } catch (ex: Exception) {
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                false
            }
        }
        Toast.makeText(context, "File write permissions not yet granted.", Toast.LENGTH_LONG).show()
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportShortcuts(uri: Uri, rows: List<List<String>>): Boolean {
        if (permissionChecker.requestPermissionsBasedOnAppVersion()) {
            return try {
                val openFileDescriptor = context.contentResolver.openFileDescriptor(uri, "rwt")
                val fileDescriptor = openFileDescriptor?.fileDescriptor
                val fileStream = FileOutputStream(fileDescriptor)
                val writer = CSVWriter(OutputStreamWriter(fileStream))
                for (row in rows) {
                    writer.writeNext(row.toTypedArray());
                }
                writer.close();
                fileStream.close()
                openFileDescriptor?.close()
                true
            } catch (ex: IllegalArgumentException) {
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                false
            } catch (ex: Exception) {
                print(ex.stackTrace)
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                false
            }
        }
        Toast.makeText(context, "File write permissions not yet granted.", Toast.LENGTH_LONG).show()
        return false
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun importShortcutValuesFromCSV(uri: Uri): List<Array<String>>? {
        var results: List<Array<String>>? = null
        if (permissionChecker.requestPermissionsBasedOnAppVersion()) {
            return try {
                val openFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                val fileDescriptor = openFileDescriptor?.fileDescriptor
                val fileStream = FileInputStream(fileDescriptor)
                val reader = CSVReader(InputStreamReader(fileStream))
                results = reader.readAll()
                reader.close()
                fileStream.close()
                openFileDescriptor?.close()
                results
            } catch (ex: IllegalArgumentException) {
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                null
            } catch (ex: Exception) {
                print(ex.stackTrace)
                Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show()
                null
            }
        }
        Toast.makeText(context, "File write permissions not yet granted.", Toast.LENGTH_LONG).show()
        return results
    }
}