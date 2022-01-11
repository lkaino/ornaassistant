package com.rockethat.ornaassistant.ornaviews

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.rockethat.ornaassistant.*
import org.json.JSONObject
import org.json.JSONTokener
import java.util.ArrayList

class OrnaViewItem : OrnaView {
    val TAG = "OrnaViewItem"
    var itemName: String? = null
    var nameLocation: Rect? = null
    var attributes: MutableMap<String, Int> = mutableMapOf()
    var level: Int = 1

    constructor(
        data: ArrayList<ScreenData>,
        wm: WindowManager,
        ctx: Context
    ) : super(OrnaViewType.ITEM, wm, ctx) {

    }

    override fun update(
        data: ArrayList<ScreenData>,
        updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit
    ): Boolean {
        if (itemName == null)
        {
            val cleanedData = data
                .filter { it.name.startsWithUppercaseLetter() }
                .filterNot { it.name.startsWith("Inventory") }
                .filterNot { it.name.startsWith("Knights of Inferno") }
                .filterNot { it.name.startsWith("Earthen Legion") }
                .filterNot { it.name.startsWith("FrozenGuard") }
                .filterNot { it.name.startsWith("Party") }
                .filterNot { it.name.startsWith("Arena") }
                .filterNot { it.name.startsWith("Codex") }
                .filterNot { it.name.startsWith("Runeshop") }
                .filterNot { it.name.startsWith("Options") }
                .filterNot { it.name.startsWith("Gauntlet") }
                .filterNot { it.name.startsWith("Character") }
            getName(cleanedData)
            getAttributes(cleanedData)
            assessItem(updateResults)
        }

        return false
    }

    private fun getName(data: List<ScreenData>) {
        val qualities = listOf(
            "Broken ",
            "Poor ",
            "Superior ",
            "Famed ",
            "Legendary ",
            "Ornate ",
            "Masterforged ",
            "Demonforged ",
            "Godforged "
        )

        // https://discord.com/channels/448527960056791051/448548728861884426/870788096805969971
        /*
        Enchantment Prefixes
            Fire - burning, embered, fiery, flaming, infernal, scalding, warm
            Water - chilling, icy, oceanic, snowy, tidal, winter
            Earthen - balanced, earthly, grounded, natural, organic, rocky, stony
            Lightning - electric, shocking, sparking, stormy, thunderous
            Holy - angelic, bright, divine, moral, pure, purifying, revered, righteous, saintly, sublime
            Dark - corrupted, diabolic, demonic, gloomy, impious, profane, unhallowed, wicked
            Dragon - beastly, bestial, chimeric, dragonic, mighty, wild
            None - colorless, customary, normalized, origin, reformed, renewed, reworked
         */

        var prefixes = listOf(
            "burning",
            "embered",
            "fiery",
            "flaming",
            "infernal",
            "scalding",
            "warm",
            "chilling",
            "icy",
            "oceanic",
            "snowy",
            "tidal",
            "winter",
            "balanced",
            "earthly",
            "grounded",
            "natural",
            "organic",
            "rocky",
            "stony",
            "electric",
            "shocking",
            "sparking",
            "stormy",
            "thunderous",
            "angelic",
            "bright",
            "divine",
            "moral",
            "pure",
            "purifying",
            "revered",
            "righteous",
            "saintly",
            "sublime",
            "corrupted",
            "diabolic",
            "demonic",
            "gloomy",
            "impious",
            "profane",
            "unhallowed",
            "wicked",
            "beastly",
            "bestial",
            "chimeric",
            "dragonic",
            //"mighty",
            "wild",
            "colorless",
            "customary",
            "normalized",
            "origin",
            "reformed",
            "renewed",
            "reworked"
        )
        val nameData = data.firstOrNull()
        var name = nameData?.name
        nameLocation = nameData?.position

        for (quality in qualities) {
            if (name?.startsWith(quality) == true) {
                name = name.replace(quality, "")
            }
        }

        for (prefix in prefixes) {
            if (name?.startsWith(prefix.capitalize()) == true) {
                name = name.replace(prefix.capitalize() + " ", "")
            }
        }

        itemName = name
    }

    private fun getAttributes(data: List<ScreenData>) {
        var bAdornments = false
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward")

        for (item in data) {
            if (item.name.contains("ADORNMENTS")) {
                bAdornments = true
            } else if (item.name.contains("Level")) {
                level = item.name.replace("Level ", "").toInt()
            } else {
                var text = item.name
                    .replace("−", "-")
                    .replace(" ", "")
                    .replace(",", "")
                    .replace(".", "")
                val match = Regex("([A-Za-z\\s]+):\\s(-?[0-9]+)").findAll(text)
                match.forEach {
                    if (it.groups.size == 3) {
                        val attName = it.groups[1]?.value.toString()
                        val attVal = it.groups[2]?.value?.toInt()
                        if (acceptedAttributes.contains(attName) && attVal != null) {
                            if (!bAdornments) {
                                if (attName == "Level") {
                                    level = attVal
                                } else {
                                    attributes[attName] = attVal
                                }
                            } else {

                                var newValue = attributes[attName]
                                if (newValue != null) {
                                    newValue -= attVal
                                    attributes[attName] = newValue
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun assessItem(updateResults: (MutableMap<OrnaViewUpdateType, Any?>) -> Unit) {
        val url = "https://orna.guide/api/v1/assess"
        val start = System.currentTimeMillis()

        val params = HashMap<String, Any>()
        attributes!!.forEach { (attName, attValue) ->
            when (attName) {
                "HP" -> params["hp"] = attValue
                "Mana" -> params["mana"] = attValue
                "Mag" -> params["magic"] = attValue
                "Att" -> params["attack"] = attValue
                "Def" -> params["defense"] = attValue
                "Res" -> params["resistance"] = attValue
                "Dex" -> params["dexterity"] = attValue
                "Ward" -> params["ward"] = attValue
                "Crit" -> {}
                else -> {
                    Log.d(TAG, "Invalid attribute $attName")
                    return
                }
            }
        }

        params["name"] = itemName!!
        params["level"] = level
        val jsonObject = JSONObject(params as Map<*, *>)
        Log.d(TAG, "Assessing item ${jsonObject}")

        // Volley post request with parameters
        Log.v(TAG, "POSTING request!")
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                // Process the json

                try {
                    Log.d(TAG, "Response in ${System.currentTimeMillis() - start} ms: $response")

                    val jsonObject = JSONTokener(response.toString()).nextValue() as JSONObject
                    updateResults(mutableMapOf(OrnaViewUpdateType.ITEM_ASSESS_RESULTS to jsonObject))
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: $e")
                }

            }, {
                // Error in request
                Log.e(TAG, "Volley error: $it")
            })


        // Volley request policy, only one time request to avoid duplicate transaction
        request.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            // 0 means no retry
            1, // DefaultRetryPolicy.DEFAULT_MAX_RETRIES = 2
            1f // DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Add the volley post request to the request queue
        VolleySingleton.getInstance(ctx).addToRequestQueue(request)
    }

    private fun createLayout(x_: Int, y_: Int, width_: Int, height_: Int, text: String) {
        if (mLayout != null) {
            return
        }

        Log.d(TAG, "CREATING LAYOUT")

        mLayout = LinearLayout(ctx)
        val layout = mLayout as LinearLayout
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)

        //mLayout!!.setBackgroundColor(Color.GREEN and 0x55FFFFFF)
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.apply {
            y = x_
            x = y_
            width = width_
            height = height_
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            gravity = Gravity.TOP or Gravity.LEFT
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        var textView = TextView(ctx)
        textView.text = text
        textView.setTextColor(Color.WHITE)

        layout.addView(textView)

        layout.isVisible = true

        try {
            wm.addView(layout, layoutParams)
        } catch (ex: Exception) {
            Log.i(TAG, "adding view failed", ex)
        }
    }
}