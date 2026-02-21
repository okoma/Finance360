package com.i2medier.financialpro.planner.integration

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.ui.CalculatorRegistry

object PlannerCalculatorBridge {
    fun openPlannerWithResult(
        context: Context,
        amount: Double,
        type: TransactionType,
        title: String,
        note: String
    ) {
        if (amount <= 0.0) {
            Toast.makeText(context, "No valid amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        val request = PlannerAddRequest(
            amount = amount,
            suggestedType = type,
            note = note,
            title = title,
            calculatorCategory = CalculatorRegistry.primaryCategoryForActivity(context.javaClass)
        )

        val intent = PlannerIntegrationContract.createOpenPlannerIntent(context, request)
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
