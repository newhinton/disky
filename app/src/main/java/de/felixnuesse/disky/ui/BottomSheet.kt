package de.felixnuesse.disky.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.felixnuesse.disky.AboutActivity
import de.felixnuesse.disky.FAQActivity
import de.felixnuesse.disky.MainActivity
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

        binding.faqButton.setOnClickListener {
            startActivity(Intent(this.context, FAQActivity::class.java))
        }

        binding.aboutButton.setOnClickListener {
            startActivity(Intent(this.context, AboutActivity::class.java))
        }


        val sharedPref = binding.root.context.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val selection = sharedPref.getInt(MainActivity.APP_PREFERENCE_SORTORDER, 0)
        binding.sortorderDropdownAutocomplete.setText(binding.sortorderDropdownAutocomplete.adapter.getItem(selection).toString(), false)
        binding.sortorderDropdownAutocomplete.onItemClickListener =
            OnItemClickListener { _, _, pos, _ ->
                editor.putInt(MainActivity.APP_PREFERENCE_SORTORDER, pos)
                editor.apply()
            }
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}
