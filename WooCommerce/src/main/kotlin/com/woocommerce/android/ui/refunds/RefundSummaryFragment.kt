package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.OrderDetailFragment.Companion.REFUND_REQUEST_CODE
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowSnackbarEvent
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_refund_summary.*
import javax.inject.Inject

class RefundSummaryFragment : DaggerFragment(), BackPressListener {
    companion object {
        const val REFUND_SUCCESS_KEY = "refund-success-key"
    }
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_refund_summary, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        initializeViews(viewModel)
        setupObservers(viewModel)
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.triggerEvent.observe(this, Observer { event ->
            when (event) {
                is ShowSnackbarEvent -> {
                    if (event.undoAction == null) {
                        uiMessageResolver.showSnack(event.message)
                    } else {
                        val snackbar = uiMessageResolver.getUndoSnack(
                                event.message,
                                "",
                                actionListener = View.OnClickListener { event.undoAction.invoke() }
                        )
                        snackbar.addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                viewModel.onProceedWithRefund()
                            }
                        })
                        snackbar.show()
                    }
                }
            }
        })

        viewModel.isSummaryFormEnabled.observe(this, Observer {
            refundSummary_btnRefund.isEnabled = it
            refundSummary_reason.isEnabled = it
        })

        viewModel.formattedRefundAmount.observe(this, Observer {
            refundSummary_refundAmount.text = it
        })

        viewModel.previousRefunds.observe(this, Observer {
            refundSummary_previouslyRefunded.text = it
        })

        viewModel.refundMethod.observe(this, Observer {
            refundSummary_method.text = it
        })

        viewModel.isManualRefundDescriptionVisible.observe(this, Observer { visible ->
            refundSummary_methodDescription.visibility = if (visible) View.VISIBLE else View.GONE
        })

        viewModel.exitAfterRefund.observe(this, Observer {
            val bundle = Bundle()
            bundle.putBoolean(REFUND_SUCCESS_KEY, it)

            requireActivity().navigateBackWithResult(
                    REFUND_REQUEST_CODE,
                    bundle,
                    R.id.nav_host_fragment_main,
                    R.id.orderDetailFragment
            )
        })
    }

    private fun initializeViews(viewModel: IssueRefundViewModel) {
        refundSummary_btnRefund.setOnClickListener {
            viewModel.onRefundConfirmed(refundSummary_reason.text.toString())
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        findNavController().popBackStack(R.id.orderDetailFragment, false)
        return false
    }
}
