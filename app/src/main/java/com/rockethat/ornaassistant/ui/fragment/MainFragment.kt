package com.rockethat.ornaassistant.ui.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rockethat.ornaassistant.R
import android.widget.Button
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.rockethat.ornaassistant.db.DungeonVisitDatabaseHelper
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Main.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_main, container, false)
        val allowPermission: Button = view.findViewById(R.id.allowPermission)

        allowPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        drawWeeklyChart(view)

        // Inflate the layout for this fragment
        return view
    }

    class WeekAxisFormatter(private val startDay: Int) : ValueFormatter() {
        private val days = arrayOf("Mo", "Tu", "Wed", "Th", "Fr", "Sa", "Su")
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            var index = startDay - 1 + value.toInt()
            if (index > 6)
                index -= 7
            return days.getOrNull(index) ?: value.toString()
        }
    }

    class IntegerFormatter() : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return value.toInt().toString()
        }
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return value.toInt().toString()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun drawWeeklyChart(view: View? = this.view) {
        val chart: BarChart = view?.findViewById(R.id.cWeeklyDungeons) as BarChart

        val eDung = mutableListOf<BarEntry>()
        val eOrns = mutableListOf<BarEntry>()

        val db = DungeonVisitDatabaseHelper(context as Context)

        val startOfToday = LocalDate.now().atStartOfDay()
        val startDay = startOfToday.minusDays(6).dayOfWeek.value
        for (i in 6 downTo 0)
        {
            val entries = db.getEntriesBetween(startOfToday.minusDays(i.toLong()), startOfToday.minusDays((i - 1).toLong()))
            eDung.add(BarEntry(i.toFloat(), entries.size.toFloat()))
            var orns = 0f
            entries.forEach {
                orns += it.orns
            }
            orns /= 1000000
            eOrns.add(BarEntry(i.toFloat(), orns))
        }

        val sDung = BarDataSet(eDung, "Dungeons")
        val sOrns = BarDataSet(eOrns, "Orns gained (mil)")
        sDung.valueFormatter = IntegerFormatter()
        sDung.valueTextSize = 12f
        sOrns.valueTextSize = 12f
        sDung.color = Color.RED
        sOrns.color = Color.GREEN

        val data = BarData(sDung, sOrns)

        val groupSpace = 0.06f
        val barSpace = 0.02f // x2 dataset

        val barWidth = 0.45f
        // (0.02 + 0.45) * 2 + 0.06 = 1.00 -> interval per "group"

        data.barWidth = barWidth // x2 dataset

        chart.data = data
        chart.xAxis.valueFormatter = WeekAxisFormatter(startDay)
        chart.xAxis.textSize = 10f
        chart.xAxis.position = XAxis.XAxisPosition.BOTH_SIDED
        chart.groupBars(0F, groupSpace, barSpace)
        chart.xAxis.axisMaximum = 7f
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.setCenterAxisLabels(true)
        chart.xAxis.setDrawGridLines(false)
        chart.description.isEnabled = false
        chart.invalidate()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Main.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}