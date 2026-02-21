package com.i2medier.financialpro.ui

import android.content.Context
import com.i2medier.financialpro.R
import com.i2medier.financialpro.activity.AmortizationActivity
import com.i2medier.financialpro.activity.AutoLoanActivity
import com.i2medier.financialpro.activity.BondYieldCalculatorActivity
import com.i2medier.financialpro.activity.BudgetCalculatorActivity
import com.i2medier.financialpro.activity.CarLeaseActivity
import com.i2medier.financialpro.activity.CompoundInterestActivity
import com.i2medier.financialpro.activity.CreditCardPayoffCalculatorActivity
import com.i2medier.financialpro.activity.DebtPayoffCalculatorActivity
import com.i2medier.financialpro.activity.EmergencyFundCalculatorActivity
import com.i2medier.financialpro.activity.InterestOnlyActivity
import com.i2medier.financialpro.activity.LoanAffordabilityActivity
import com.i2medier.financialpro.activity.LoanComparisonActivity
import com.i2medier.financialpro.activity.MortgageActivity
import com.i2medier.financialpro.activity.NPVCalculatorActivity
import com.i2medier.financialpro.activity.NetWorthCalculatorActivity
import com.i2medier.financialpro.activity.PresentValueCalculatorActivity
import com.i2medier.financialpro.activity.ROIActivity
import com.i2medier.financialpro.activity.RefinanceActivity
import com.i2medier.financialpro.activity.RentalYieldCalculatorActivity
import com.i2medier.financialpro.activity.RetirementCalculatorActivity
import com.i2medier.financialpro.activity.SimpleInterestActivity
import com.i2medier.financialpro.activity.TaxCalculatorActivity
import com.i2medier.financialpro.util.CountryCalculatorRules
import com.i2medier.financialpro.util.CountrySettingsManager

object CalculatorRegistry {

    const val CATEGORY_ALL = "all"
    const val CATEGORY_LOANS = "loans"
    const val CATEGORY_SAVINGS = "savings"
    const val CATEGORY_INVESTING = "investing"
    const val CATEGORY_TAX = "tax"
    const val CATEGORY_BUDGET = "budget"
    const val CATEGORY_BUSINESS = "business"
    const val CATEGORY_RETIREMENT = "retirement"
    const val CATEGORY_INSURANCE = "insurance"

    data class Item(
        val title: String,
        val subtitle: String,
        val iconRes: Int,
        val activityClass: Class<*>
    )

    private data class ItemSpec(
        val titleRes: Int,
        val subtitleRes: Int,
        val iconRes: Int,
        val activityClass: Class<*>,
        val categories: Set<String>
    )

    private fun allSpecs(): List<ItemSpec> = listOf(
        ItemSpec(R.string.mortgage_calculator, R.string.calc_sub_mortgage, R.drawable.mortgage, MortgageActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.auto_loan_calculator, R.string.calc_sub_auto_loan, R.drawable.auto_loan, AutoLoanActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.amortization_calculator, R.string.calc_sub_amortization, R.drawable.amortization, AmortizationActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.loan_comparison_calculator, R.string.calc_sub_loan_comparison, R.drawable.loan_comparison, LoanComparisonActivity::class.java, setOf(CATEGORY_LOANS, CATEGORY_BUSINESS)),
        ItemSpec(R.string.interest_only_calculator, R.string.calc_sub_interest_only, R.drawable.interest_only, InterestOnlyActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.loan_affordability_calculator, R.string.calc_sub_loan_affordability, R.drawable.loan_affordability, LoanAffordabilityActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.car_lease_calculator, R.string.calc_sub_car_lease, R.drawable.car_lease, CarLeaseActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.refinance_calculator, R.string.calc_sub_refinance, R.drawable.refinance, RefinanceActivity::class.java, setOf(CATEGORY_LOANS)),
        ItemSpec(R.string.simple_interest_calculator, R.string.calc_sub_simple_interest, R.drawable.simple_interest, SimpleInterestActivity::class.java, setOf(CATEGORY_INVESTING)),
        ItemSpec(R.string.compound_interest_calculator, R.string.calc_sub_compound_interest, R.drawable.compound_interest, CompoundInterestActivity::class.java, setOf(CATEGORY_INVESTING)),
        ItemSpec(R.string.retirement_calculator, R.string.calc_sub_retirement, R.drawable.internal_rate_of_return, RetirementCalculatorActivity::class.java, setOf(CATEGORY_RETIREMENT, CATEGORY_SAVINGS)),
        ItemSpec(R.string.emergency_fund_calculator, R.string.calc_sub_emergency_fund, R.drawable.money_recive, EmergencyFundCalculatorActivity::class.java, setOf(CATEGORY_SAVINGS, CATEGORY_BUDGET)),
        ItemSpec(R.string.debt_payoff_calculator, R.string.calc_sub_debt_payoff, R.drawable.trend_down, DebtPayoffCalculatorActivity::class.java, setOf(CATEGORY_SAVINGS, CATEGORY_BUDGET)),
        ItemSpec(R.string.credit_card_payoff_calculator, R.string.calc_sub_credit_card_payoff, R.drawable.calendar, CreditCardPayoffCalculatorActivity::class.java, setOf(CATEGORY_LOANS, CATEGORY_BUDGET)),
        ItemSpec(R.string.budget_calculator, R.string.calc_sub_budget, R.drawable.money_send, BudgetCalculatorActivity::class.java, setOf(CATEGORY_BUDGET, CATEGORY_SAVINGS)),
        ItemSpec(R.string.tax_calculator, R.string.calc_sub_tax, R.drawable.annual_percentage_rate, TaxCalculatorActivity::class.java, setOf(CATEGORY_TAX, CATEGORY_BUSINESS)),
        ItemSpec(R.string.present_value_calculator, R.string.calc_sub_present_value, R.drawable.present_value, PresentValueCalculatorActivity::class.java, setOf(CATEGORY_INVESTING, CATEGORY_BUSINESS)),
        ItemSpec(R.string.npv_calculator, R.string.calc_sub_npv, R.drawable.npv, NPVCalculatorActivity::class.java, setOf(CATEGORY_INVESTING, CATEGORY_BUSINESS)),
        ItemSpec(R.string.roi_calculator, R.string.calc_sub_roi, R.drawable.return_on_investment, ROIActivity::class.java, setOf(CATEGORY_INVESTING, CATEGORY_BUSINESS)),
        ItemSpec(R.string.net_worth_calculator, R.string.calc_sub_net_worth, R.drawable.net_worth, NetWorthCalculatorActivity::class.java, setOf(CATEGORY_SAVINGS, CATEGORY_BUDGET)),
        ItemSpec(R.string.rental_yield_calculator, R.string.calc_sub_rental_yield, R.drawable.rental_yield, RentalYieldCalculatorActivity::class.java, setOf(CATEGORY_INVESTING, CATEGORY_BUSINESS)),
        ItemSpec(R.string.bond_yield_calculator, R.string.calc_sub_bond_yield, R.drawable.bond_yield, BondYieldCalculatorActivity::class.java, setOf(CATEGORY_INVESTING))
    )

    fun all(context: Context): List<Item> {
        return byCategory(context, CATEGORY_ALL)
    }

    fun byCategory(context: Context, categoryId: String): List<Item> {
        val country = CountrySettingsManager.getSelectedCountry(context).countryCode
        return allSpecs()
            .filter { CountryCalculatorRules.isVisible(country, it.activityClass) }
            .filter { categoryId == CATEGORY_ALL || categoryId in it.categories }
            .map { spec ->
                Item(
                    title = context.getString(spec.titleRes),
                    subtitle = context.getString(spec.subtitleRes),
                    iconRes = spec.iconRes,
                    activityClass = spec.activityClass
                )
            }
    }

    fun primaryCategoryForActivity(activityClass: Class<*>): String {
        return allSpecs()
            .firstOrNull { it.activityClass == activityClass }
            ?.categories
            ?.firstOrNull()
            ?: CATEGORY_ALL
    }

    fun featured(context: Context): List<Item> {
        val all = all(context)
        val preferredOrder = listOf(
            LoanComparisonActivity::class.java,
            MortgageActivity::class.java,
            CompoundInterestActivity::class.java,
            ROIActivity::class.java
        )
        return preferredOrder.mapNotNull { cls ->
            all.firstOrNull { it.activityClass == cls }
        }
    }

    fun plannerQuickTools(context: Context): List<Item> {
        val all = all(context)
        val preferredOrder = listOf(
            MortgageActivity::class.java,
            ROIActivity::class.java,
            TaxCalculatorActivity::class.java,
            AmortizationActivity::class.java
        )
        return preferredOrder.mapNotNull { cls ->
            all.firstOrNull { it.activityClass == cls }
        }
    }
}
