package org.ergoplatform.android.transactions

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.ergoplatform.android.PromptSigningResult

/**
 * Builds a cold signing request according to EIP19
 */
fun buildColdSigningRequest(data: PromptSigningResult): String? {
    if (data.success) {
        val gson = Gson()
        val root = JsonObject()
        root.addProperty("reducedTx", Base64.encodeToString(data.serializedTx!!, Base64.NO_WRAP))
        root.addProperty("sender", data.address)
        val inputsarray = JsonArray()
        data.serializedInputs?.map { Base64.encodeToString(it, Base64.NO_WRAP) }
            ?.forEach { inputsarray.add(it) }
        root.add("inputs", inputsarray)
        return gson.toJson(root)

    } else {
        return null
    }
}