package com.android.dictionary_app

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.*
import org.json.JSONArray
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var dictionary: PatriciaTrie
    private var loadingComplete = false


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editTextInput = findViewById<EditText>(R.id.editTextInput)
        val textViewResult = findViewById<TextView>(R.id.textViewResult)
        val loadButton = findViewById<Button>(R.id.load)
        dictionary = PatriciaTrie()
        loadButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                 val suggestions = loadSuggestionsFromJson(this@MainActivity)
                suggestions.forEach { (word, frequency) -> dictionary.insert(word, frequency) }
                loadingComplete = true

            }

            editTextInput.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(
                    charSequence: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val query = charSequence.toString().trim()
                    if (query.isNotEmpty() && loadingComplete) {
                        CoroutineScope(Dispatchers.Main).launch {

                            val results = withContext(Dispatchers.Default) {
                                dictionary.search(query)
                            }

                            textViewResult.text = if (results.isNotEmpty()) {
                                "Results:\n" + results.joinToString("\n")
                            } else {
                                "No suggestions found"
                            }
                        }
                    } else {
                        textViewResult.text = ""
                    }
                }
            })

        }
    }


private fun loadSuggestionsFromJson(context: Context): List<Pair<String, Int>> {
    val inputStream = context.resources.openRawResource(R.raw.data_without_noise_without_redundant)
    val jsonString = inputStream.bufferedReader().use { it.readText() }
    val jsonArray = JSONArray(jsonString)
    val suggestions = mutableListOf<Pair<String, Int>>()

    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.optJSONObject(i) ?: continue
        val suggestion = jsonObject.optString("Clicked suggestion", null) ?: continue
        val frequencyString = jsonObject.optString("frequency", null) ?: "0"
        val frequency = frequencyString.toIntOrNull() ?: 0
        suggestions.add(suggestion to frequency)
    }

    return suggestions
}


}

abstract class SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable?) {}
}
