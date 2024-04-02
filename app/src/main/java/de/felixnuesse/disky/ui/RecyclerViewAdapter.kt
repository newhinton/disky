package de.felixnuesse.disky.ui

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import de.felixnuesse.disky.databinding.ItemFolderEntryBinding
import de.felixnuesse.disky.extensions.readableFileSize
import de.felixnuesse.disky.model.FolderEntry


class RecyclerViewAdapter(private val folders: List<FolderEntry>, var callback: ChangeFolderCallback?) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemFolderEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        with(holder) {
            with(folders[position]){
                binding.title.text = name
                binding.size.text = readableFileSize(getCalculatedSize())
                binding.progressBar.progress = percent

                binding.linearLayout.setOnClickListener {
                    callback?.changeFolder(this)
                }
            }
        }
    }
    override fun getItemCount(): Int {
        return folders.size
    }

    inner class ViewHolder(val binding: ItemFolderEntryBinding) : RecyclerView.ViewHolder(binding.root)

}
