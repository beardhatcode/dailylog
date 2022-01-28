package com.app.dailylog.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.TypedValue
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.dailylog.R
import com.app.dailylog.repository.Constants
import com.app.dailylog.repository.Shortcut
import com.app.dailylog.utils.DetermineBuild
import kotlinx.android.synthetic.main.settings_view.view.*
import java.util.Objects.isNull

class SettingsFragment(private val viewModel: SettingsViewModel) : Fragment(),
    AddShortcutDialogFragment.AddShortcutDialogListener,
    BulkAddShortcutsDialogFragment.BulkAddListener,
    EditShortcutDialogFragment.EditShortcutDialogListener,
    ShortcutDialogListener {

    var shortcutsLiveData: LiveData<List<Shortcut>> = viewModel.getAllShortcuts()
    private lateinit var adapter: ShortcutListAdapter

    companion object {
        fun newInstance(viewModel: SettingsViewModel) = SettingsFragment(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val value = TypedValue()
        context?.theme?.resolveAttribute(R.attr.colorAccent, value, true)
        adapter = ShortcutListAdapter(
            removeCallback = { label -> viewModel.removeCallback(label) },
            updateShortcutPositions = { shortcuts ->
                viewModel.updateShortcutPositions(
                    shortcuts
                )
            },
            editCallback = { shortcut ->
                onEdit(shortcut)
            },
            cursorColor = if (value.type == TypedValue.TYPE_INT_COLOR_RGB8 || value.type == TypedValue.TYPE_INT_COLOR_RGB4 || value.type == TypedValue.TYPE_INT_COLOR_ARGB4 || value.type == TypedValue.TYPE_INT_COLOR_ARGB8) value.data else -0x10000
        )
        shortcutsLiveData.observe(viewLifecycleOwner, Observer { shortcuts ->
            // Update the cached copy of the words in the adapter.
            shortcuts.let {
                adapter.updateItems(it)
                renderShortcutInstructions()
            }
        })
        renderFileNameRow()
        renderShortcutList()
        view?.addShortcutButton?.setOnClickListener {
            val addDialog: AddShortcutDialogFragment =
                AddShortcutDialogFragment.newInstance(viewModel.createShortcutDialogViewModel())
            addDialog.setTargetFragment(this, 300)
            addDialog.show(parentFragmentManager, "fragment_add_shortcut")
        }
        view?.addShortcutButton?.setOnLongClickListener {
            bulkAddShortcuts()
            return@setOnLongClickListener true
        }
        view?.shortcutMenuButton?.setOnClickListener { v: View ->
            showMenu(v, R.menu.shortcut_options_menu)
        }
    }

    private fun bulkAddShortcuts() {
        val addBulkDialog: BulkAddShortcutsDialogFragment =
            BulkAddShortcutsDialogFragment.newInstance(viewModel.createShortcutDialogViewModel())
        addBulkDialog.setTargetFragment(this, 300)
        addBulkDialog.show(parentFragmentManager, "fragment_bulk_add")
    }

    private fun selectImportFile() {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
        startActivityForResult(
            Intent.createChooser(intent, "Select file"),
            Constants.SELECT_SHORTCUT_FILE_CODE
        )
    }

    private fun selectExportFile() {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "shortcuts.csv")
            }
        if (DetermineBuild.isOreoOrGreater()) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/Documents"));
        }
        startActivityForResult(
            Intent.createChooser(intent, "Create file"),
            Constants.CREATE_FILE_CODE
        )
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(context!!, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.bulkAdd -> bulkAddShortcuts()
                R.id.exportShortcuts -> selectExportFile()
                R.id.importShortcuts -> selectImportFile()
            }
            return@setOnMenuItemClickListener true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun onEdit(shortcut: Shortcut) {
        val editDialog: EditShortcutDialogFragment =
            EditShortcutDialogFragment.newInstance(shortcut, viewModel.createShortcutDialogViewModel())
        editDialog.setTargetFragment(this, 300)
        editDialog.show(parentFragmentManager, "fragment_edit")
    }

    private fun renderFileNameRow() {
        view?.fileName?.text = viewModel.getFilename()
        view?.selectFileButton?.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/*"
                }
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
        }
    }

    private fun renderShortcutList() {
        val recyclerView = view!!.recycler_view
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val callback: ItemTouchHelper.Callback = ShortcutTouchHelperCallback(adapter)
        val shortcutTouchHelper = ItemTouchHelper(callback)
        shortcutTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.SELECT_FILE_CODE && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val selectedFileUri = data.data;
            if (selectedFileUri != null) {
                viewModel.saveFilename(selectedFileUri.toString())
                val contentResolver = context!!.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(selectedFileUri, takeFlags)
                view?.fileName?.text = viewModel.getFilename()
//                TODO("if we didn't get the permissions we needed, ask for permission or have the user select a different file")
            }
        }
        if ((requestCode == Constants.CREATE_FILE_CODE) && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val selectedFileUri = data.data;
            if (selectedFileUri != null) {
                viewModel.exportFileUri = selectedFileUri
                val contentResolver = context!!.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(selectedFileUri, takeFlags)
//                TODO("if we didn't get the permissions we needed, ask for permission or have the user select a different file")
                val error = viewModel.exportShortcuts()
                if (error != null) Toast.makeText(context!!, error.message, Toast.LENGTH_LONG)
            }
        }
        if (requestCode == Constants.SELECT_SHORTCUT_FILE_CODE && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val selectedFileUri = data.data;
            if (selectedFileUri != null) {
                viewModel.importShortcuts(selectedFileUri)
                val contentResolver = context!!.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(selectedFileUri, takeFlags)
//                TODO("if we didn't get the permissions we needed, ask for permission or have the user select a different file")
            }
        }
    }

    private fun renderShortcutInstructions() {
        if (adapter.items.isEmpty()) {
            view!!.noShortcutsMessage.visibility = View.VISIBLE
        } else {
            view!!.noShortcutsMessage.visibility = View.GONE
        }
    }

    override fun onFinishAddShortcutDialog(label: String, text: String, cursor: Int, type: String) {
        viewModel.addShortcut(
            label,
            text,
            cursor,
            type
        )
    }

    override fun onBulkAddShortcuts(info: List<Array<String>>) {
        viewModel.bulkAddShortcuts(info)
    }

    override fun onFinishEditShortcutDialog(
        label: String,
        text: String,
        cursor: Int,
        position: Int,
        type: String
    ) {
        viewModel.updateShortcut(
            label,
            text,
            cursor,
            position,
            type
        )
    }

    override fun labelExists(label: String): LiveData<Boolean> {
        return viewModel.labelExists(label)
    }

}