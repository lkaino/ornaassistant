package com.rockethat.ornaassistant
import com.rockethat.ornaassistant.AssessedItemsAdapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rockethat.ornaassistant.databinding.AssessedItemsFragmentBinding

class AssessedItemsFragment : Fragment() {

    private var _binding: AssessedItemsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AssessedItemsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Example data for the RecyclerView
        val exampleData = listOf("Item 1", "Item 2", "Item 3") // Replace with your data

        // Set up the RecyclerView
        binding.rvAssess.layoutManager = LinearLayoutManager(context)
        binding.rvAssess.adapter = AssessedItemsAdapter(exampleData)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
