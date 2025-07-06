package de.felixnuesse.disky.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AutoCompleteTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.felixnuesse.disky.AboutActivity
import de.felixnuesse.disky.FAQActivity
import de.felixnuesse.disky.MainActivity
import de.felixnuesse.disky.MainActivity.Companion.APP_PREFERENCE_LSW_TRESHOLD
import de.felixnuesse.disky.MainActivity.Companion.APP_PREFERENCE_LSW_TYPE
import de.felixnuesse.disky.MainActivity.Companion.APP_PREFERENCE_SORTORDER
import de.felixnuesse.disky.databinding.BottomsheetBinding
import de.felixnuesse.disky.worker.BackgroundWorker


class BottomSheet(): BottomSheetDialogFragment() {

    private lateinit var binding: BottomsheetBinding

    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


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

        sharedPref = binding.root.context.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
        editor = sharedPref.edit()

        prepDropdown(APP_PREFERENCE_SORTORDER, 0, binding.sortorderDropdownAutocomplete)
        prepDropdown(APP_PREFERENCE_LSW_TYPE, 0, binding.lowStorageCheckTimeAutocomplete)
        prepDropdown(APP_PREFERENCE_LSW_TRESHOLD, 0, binding.lowStorageCheckThresholdAutocomplete)

    }

    fun prepDropdown(prefString: String, default: Int, dropdown: AutoCompleteTextView) {
        val selection = sharedPref.getInt(prefString, default)
        dropdown.setText(dropdown.adapter.getItem(selection).toString(), false)
        dropdown.onItemClickListener =
            OnItemClickListener { _, _, pos, _ ->
                editor.putInt(prefString, pos)
                editor.apply()

                if(prefString != APP_PREFERENCE_SORTORDER) {
                    BackgroundWorker.schedule(dropdown.context.applicationContext)
                }
            }
    }


    companion object {
        const val TAG = "ModalBottomSheet"
    }
}
