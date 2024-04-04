package de.felixnuesse.disky.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.disky.R
import de.felixnuesse.disky.databinding.ItemFolderEntryBinding
import de.felixnuesse.disky.databinding.ItemLeafEntryBinding
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.extensions.tag
import de.felixnuesse.disky.model.StoragePrototype
import de.felixnuesse.disky.model.StorageType
import de.felixnuesse.disky.model.StorageLeaf


class RecyclerViewAdapter(private var mContext: Context, private val folders: List<StoragePrototype>, var callback: ChangeFolderCallback?):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is FolderView) {
            with(holder) {
                with(folders[position]){
                    binding.title.text = name
                    binding.size.text = readableFileSize(getCalculatedSize())
                    binding.progressBar.progress = percent

                    if(storageType == StorageType.APP_COLLECTION) {
                        setImage(R.drawable.icon_apps)
                    }

                    setChangeFolderCallbackTarget(this)
                }
            }
        } else {
            with(holder as LeafView) {
                with(folders[position] as StorageLeaf){
                    binding.title.text = name
                    binding.size.text = readableFileSize(getCalculatedSize())

                    when(StorageType.fromInt(holder.itemViewType)) {
                        StorageType.OS,
                        StorageType.APP_APK,
                        StorageType.APP -> {
                            setImage( R.drawable.icon_android)
                        }
                        StorageType.APP_CACHE_EXTERNAL -> {
                            setImage( R.drawable.icon_sd)
                        }
                        StorageType.APP_CACHE -> {
                            setImage( R.drawable.icon_cache)
                        }
                        StorageType.APP_DATA -> {
                            setImage( R.drawable.icon_account)
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

    inner class FolderView(var binding: ItemFolderEntryBinding): RecyclerView.ViewHolder(binding.root) {
        fun setImage(resource: Int) {
            binding.imageView.setImageDrawable(AppCompatResources.getDrawable(mContext, resource))
        }

        fun setChangeFolderCallbackTarget(folder: StoragePrototype) {
            binding.linearLayout.setOnClickListener {
                callback?.changeFolder(folder)
            }
        }
    }
    inner class LeafView(var binding: ItemLeafEntryBinding): RecyclerView.ViewHolder(binding.root) {
        fun setImage(resource: Int) {
            binding.leafImage.setImageDrawable(AppCompatResources.getDrawable(mContext, resource))
        }

        fun setChangeFolderCallbackTarget(folder: StoragePrototype) {
            binding.linearLayout.setOnClickListener {
                callback?.changeFolder(folder)
            }
        }
    }

}
