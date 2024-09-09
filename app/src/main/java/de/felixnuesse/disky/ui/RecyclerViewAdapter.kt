package de.felixnuesse.disky.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.disky.R
import de.felixnuesse.disky.databinding.ItemFolderEntryBinding
import de.felixnuesse.disky.databinding.ItemLeafEntryBinding
import de.felixnuesse.disky.extensions.getAppIcon
import de.felixnuesse.disky.extensions.getAppIconDisabled
import de.felixnuesse.disky.extensions.getAppname
import de.felixnuesse.disky.extensions.isAppEnabled
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.extensions.startApp
import de.felixnuesse.disky.extensions.startAppSettings
import de.felixnuesse.disky.model.StorageBranch
import de.felixnuesse.disky.model.StorageLeaf
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import de.felixnuesse.disky.ui.dialogs.DeleteDialog
import de.felixnuesse.disky.ui.dialogs.HelpDialog
import java.io.File


class RecyclerViewAdapter(private var mContext: Context, private val folders: List<StoragePrototype>, var callback: ChangeFolderCallback?):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(StorageType.fromInt(viewType)) {
            StorageType.FILE,
            StorageType.APP_APK,
            StorageType.APP_DATA,
            StorageType.APP_CACHE_EXTERNAL,
            StorageType.APP_CACHE,
            StorageType.OS ->
                LeafView(ItemLeafEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

            else -> FolderView(ItemFolderEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is FolderView) {
            with(holder) {
                with(folders[position]){
                    val branch = folders[position] as StorageBranch
                    binding.title.text = name
                    // the recyclerview does not reenable the title on scroll.
                    binding.title.isEnabled = true
                    binding.size.text = readableFileSize(getCalculatedSize())
                    binding.progressBar.progress = percent

                    if(storageType == StorageType.APP_COLLECTION) {
                        setImage(R.drawable.icon_apps)
                    }

                    if(StorageType.fromInt(holder.itemViewType) == StorageType.APP) {
                        binding.title.text = getAppname(name, mContext)
                        binding.imageView.imageTintList = null
                        binding.imageView.setImageDrawable(getAppIcon(name, mContext))
                        setMenu(R.menu.context_folder_app_menu, branch)
                        if(!isAppEnabled(name, mContext)) {
                            binding.title.text = getAppname(name, mContext) + " " + mContext.getString(R.string.app_disabled_suffix)
                            binding.title.isEnabled = false
                            binding.imageView.setImageDrawable(getAppIconDisabled(name, mContext))
                        }
                    }

                    if(storageType == StorageType.FOLDER) {
                        holder.leafFolder = branch
                        enableDeletion()
                    }
                    setChangeFolderCallbackTarget(this)
                }
            }
        } else {
            with(holder as LeafView) {
                with(folders[position] as StorageLeaf){
                    binding.title.text = name
                    binding.size.text = readableFileSize(getCalculatedSize())

                    val leaf = this
                    when(StorageType.fromInt(holder.itemViewType)) {
                        StorageType.OS -> {
                            setMenu(R.menu.context_os_menu, leaf)
                            setImage(R.drawable.icon_android)
                        }
                        StorageType.APP_APK,
                        StorageType.APP -> {
                            setImage(R.drawable.icon_android)
                        }
                        StorageType.APP_CACHE_EXTERNAL -> {
                            setImage(R.drawable.icon_sd)
                        }
                        StorageType.APP_CACHE -> {
                            setImage(R.drawable.icon_cache)
                        }
                        StorageType.APP_DATA -> {
                            setImage(R.drawable.icon_account)
                        }
                        StorageType.FILE -> {
                            leafItem = leaf
                            enableDeletion()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return folders[position].storageType.ordinal
    }


    override fun getItemCount(): Int {
        return folders.size
    }

    inner class FolderView(var binding: ItemFolderEntryBinding): RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {

        var leafFolder: StorageBranch? = null
        private var popupMenuEnableDeletion = false

        init {
            setMenu(R.menu.context_folder_menu, null)
        }

        fun setMenu(menu: Int, branch: StorageBranch?) {
            branch.let{leafFolder = it}
            binding.root.setOnLongClickListener {
                if(leafFolder == null) {
                    return@setOnLongClickListener true
                }
                val context = binding.root.context
                val popup = PopupMenu(context, it)
                popup.setOnMenuItemClickListener(this)
                popup.menuInflater.inflate(menu, popup.menu)
                popup.menu.findItem(R.id.action_folder_delete)?.setVisible(popupMenuEnableDeletion)
                popup.setForceShowIcon(true)
                popup.show()
                true
            }
        }

        fun enableDeletion() {
            popupMenuEnableDeletion = true
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_folder_open -> {
                    val uri = Uri.parse(leafFolder!!.getParentPath())
                    val explorerIntent = Intent(Intent.ACTION_VIEW)
                    explorerIntent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    binding.root.context.startActivity(explorerIntent)
                    true
                }
                R.id.action_folder_delete -> {
                    DeleteDialog(mContext, File(leafFolder!!.getParentPath())).askDelete()
                    true
                }
                R.id.action_folder_app_open -> {
                    leafFolder?.let { startApp(it.name, mContext) }
                    true
                }
                R.id.action_folder_app_settings -> {
                    leafFolder?.let { startAppSettings(it.name, mContext) }
                    true
                }
                else -> false
            }
        }


        fun setImage(resource: Int) {
            binding.imageView.setImageDrawable(AppCompatResources.getDrawable(mContext, resource))
        }

        fun setChangeFolderCallbackTarget(folder: StoragePrototype) {
            binding.linearLayout.setOnClickListener {
                callback?.changeFolder(folder)
            }
        }
    }
    inner class LeafView(var binding: ItemLeafEntryBinding): RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {

        var leafItem: StorageLeaf? = null
        private var popupMenuEnableDeletion = false

        init {
            setMenu(R.menu.context_file_menu, null)
        }

        fun setMenu(menu: Int, leaf: StorageLeaf?) {
            leaf.let{leafItem = it}
            binding.root.setOnLongClickListener {
                if(leafItem == null) {
                    return@setOnLongClickListener true
                }
                val context = binding.root.context
                val popup = PopupMenu(context, it)
                popup.setOnMenuItemClickListener(this)
                popup.menuInflater.inflate(menu, popup.menu)
                popup.menu.findItem(R.id.action_file_delete)?.setVisible(popupMenuEnableDeletion)
                popup.setForceShowIcon(true)
                popup.show()
                true
            }
        }

        fun enableDeletion() {
            popupMenuEnableDeletion = true
        }
        fun setImage(resource: Int) {
            binding.leafImage.setImageDrawable(AppCompatResources.getDrawable(mContext, resource))
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            if(leafItem == null) {
                return false
            }

            return when (item.itemId) {
                R.id.action_file_open -> {
                    val uri = Uri.parse(leafItem!!.getParentPath())
                    val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(leafItem!!.name.split('.')[1])?: "*/*"
                    binding.root.context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, type))
                    true
                }
                R.id.action_file_openfolder -> {
                    val uri = Uri.parse(File(leafItem!!.getParentPath()).parent)
                    val explorerIntent = Intent(Intent.ACTION_VIEW)
                    explorerIntent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    binding.root.context.startActivity(explorerIntent)
                    true
                }
                R.id.action_file_delete -> {
                    DeleteDialog(mContext, File(leafItem!!.getParentPath())).askDelete()
                    true
                }
                R.id.action_os_help -> {
                    HelpDialog(mContext).help(R.string.help_android_os, R.string.help_android_os_description)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
}
