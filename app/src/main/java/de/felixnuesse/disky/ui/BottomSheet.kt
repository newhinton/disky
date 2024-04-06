package de.felixnuesse.disky.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.felixnuesse.disky.databinding.BottomsheetBinding


class BottomSheet(): BottomSheetDialogFragment() {

    private lateinit var binding: BottomsheetBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.donateButton.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse("https://felixnuesse.de/donate"))
            startActivity(i)
        }
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}
