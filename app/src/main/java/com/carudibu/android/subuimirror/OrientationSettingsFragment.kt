package com.carudibu.android.subuimirror

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment

class OrientationSettingsFragment: DialogFragment(R.layout.fragment_orientation_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

        val portrait: Spinner? = view.findViewById(R.id.portrait)
        portrait?.setSelection(sharedPref.getInt("portrait", 0))

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.orientation_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            portrait?.adapter = adapter
        }

        val landscape: Spinner? = view.findViewById(R.id.landscape)
        portrait?.setSelection(sharedPref.getInt("landscape", 0))

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.orientation_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            landscape?.adapter = adapter
        }

        view.findViewById<Button>(R.id.ok).setOnClickListener {
            with (sharedPref.edit()) {
                portrait?.selectedItemPosition?.let { it1 -> putInt("portrait", it1) }
                landscape?.selectedItemPosition?.let { it1 -> putInt("landscape", it1) }
                apply()
            }
        }

    }
}