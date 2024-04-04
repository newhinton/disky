package de.felixnuesse.disky.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.disky.R
import de.felixnuesse.disky.databinding.ItemAppEntryBinding
import de.felixnuesse.disky.databinding.ItemFileEntryBinding
import de.felixnuesse.disky.databinding.ItemFolderEntryBinding
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.model.AppStorageElementEntry
import de.felixnuesse.disky.model.AppdataStorageElementEntry
import de.felixnuesse.disky.model.OSStorageElementEntry
import de.felixnuesse.disky.model.StorageElementEntry
import de.felixnuesse.disky.model.StorageElementType


class RecyclerViewAdapter(private var mContext: Context, private val folders: List<StorageElementEntry>, var callback: ChangeFolderCallback?):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            StorageElementType.FILE.ordinal -> FileView(ItemFileEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            StorageElementType.APP.ordinal -> AppView(ItemAppEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            StorageElementType.APPDATA.ordinal -> AppView(ItemAppEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            StorageElementType.SPECIAL_SYSTEM.ordinal -> AppView(ItemAppEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> FolderView(ItemFolderEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when(holder.itemViewType) {
            StorageElementType.FILE.ordinal -> {
                with(holder as FileView) {
                    with(folders[position]){
                        binding.title.text = name
                        binding.size.text = readableFileSize(getCalculatedSize())
                    }
                }
            }
            StorageElementType.APP.ordinal -> {
                with(holder as AppView) {
                    with(folders[position] as AppStorageElementEntry){
                        binding.title.text = name
                        binding.size.text = readableFileSize(getCalculatedSize())

                        if(overrideIcon != null) {
                            binding.imageView2.setImageDrawable(overrideIcon)
                        }

                        binding.linearLayout.setOnClickListener {
                            callback?.changeFolder(this)
                        }
                    }
                }
            }
            StorageElementType.APPDATA.ordinal -> {
                with(holder as AppView) {
                    with(folders[position] as AppdataStorageElementEntry){
                        binding.title.text = name
                        binding.size.text = readableFileSize(getCalculatedSize())
                        if(overrideIcon != null) {
                            binding.imageView2.setImageDrawable(overrideIcon)
                        }
                    }
                }
            }
            StorageElementType.SPECIAL_SYSTEM.ordinal -> {
                with(holder as AppView) {
                    with(folders[position] as OSStorageElementEntry){
                        binding.title.text = mContext.getText(R.string.system)
                        binding.size.text = readableFileSize(getCalculatedSize())
                        binding.imageView2.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.icon_android))
                    }
                }
            }
            else -> {
                with(holder as FolderView) {
                    with(folders[position]){
                        binding.title.text = name
                        binding.size.text = readableFileSize(getCalculatedSize())
                        binding.progressBar.progress = percent

                        if(storageType == StorageElementType.SPECIAL_APPFOLDER) {
                            binding.imageView.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.icon_apps))
                        }

                        binding.linearLayout.setOnClickListener {
                            callback?.changeFolder(this)
                        }
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

    inner class FolderView(var binding: ItemFolderEntryBinding): RecyclerView.ViewHolder(binding.root)
    inner class FileView(var binding: ItemFileEntryBinding): RecyclerView.ViewHolder(binding.root)
    inner class AppView(var binding: ItemAppEntryBinding): RecyclerView.ViewHolder(binding.root)

}
