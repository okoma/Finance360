package com.i2medier.financialpro.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.home.CalculatorListAdapter
import com.i2medier.financialpro.ui.search.RecentCalculatorStore

class CalculatorsFragment : Fragment() {

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        fun newInstance(categoryId: String): CalculatorsFragment {
            return CalculatorsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_ID, categoryId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calculators, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryId = arguments?.getString(ARG_CATEGORY_ID) ?: CalculatorRegistry.CATEGORY_ALL
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvCalculatorList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CalculatorListAdapter(
            CalculatorRegistry.byCategory(requireContext(), categoryId)
        ) { item ->
            RecentCalculatorStore.record(requireContext(), item.activityClass.name)
            startActivity(Intent(requireContext(), item.activityClass))
        }
    }
}
