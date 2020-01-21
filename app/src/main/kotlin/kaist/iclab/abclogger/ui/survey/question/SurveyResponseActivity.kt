package kaist.iclab.abclogger.ui.survey.question

import android.os.Bundle
import android.view.*
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseAppCompatActivity
import kaist.iclab.abclogger.databinding.ActivitySurveyResponseBinding

import kaist.iclab.abclogger.ui.Status
import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.survey.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.survey.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.survey.sharedViewNameForTitle
import kotlinx.android.synthetic.main.activity_survey_response.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SurveyResponseActivity : BaseAppCompatActivity() {
    private val viewModel: SurveyResponseViewModel by viewModel()
    private lateinit var binding: ActivitySurveyResponseBinding
    private var reactionTime: Long = -1L

    private val entityId = intent.getLongExtra(EXTRA_ENTITY_ID, -1)
    private val showFromList = intent.getBooleanExtra(EXTRA_SHOW_FROM_LIST, false)
    private val surveyTitle = intent.getStringExtra(EXTRA_SURVEY_TITLE) ?: ""
    private val surveyMessage = intent.getStringExtra(EXTRA_SURVEY_MESSAGE) ?: ""
    private val surveyDeliveredTime = intent.getLongExtra(EXTRA_SURVEY_DELIVERED_TIME, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        closeOptionsMenu()

        reactionTime = System.currentTimeMillis()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_survey_response)

        setSupportActionBar(tool_bar)
        supportActionBar?.apply {
            title = getString(R.string.title_survey_response)
            setDisplayHomeAsUpEnabled(true)
        }

        val recyclerViewAdapter = SurveyQuestionListAdapter()

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.recyclerView.apply {
            adapter = recyclerViewAdapter
        }

        viewModel.loadStatus.observe(this) { status ->
            when (status.state) {
                Status.STATE_LOADING -> binding.loadProgressBar.show()
                Status.STATE_SUCCESS -> {
                    openOptionsMenu()
                    binding.loadProgressBar.hide()
                }
                Status.STATE_FAILURE -> {
                    binding.loadProgressBar.hide()
                    showToast(status.error, false)
                }
            }
        }

        viewModel.storeStatus.observe(this) { status ->
            when (status.state) {
                Status.STATE_LOADING -> binding.storeProgressBar.show()
                Status.STATE_SUCCESS -> {
                    binding.storeProgressBar.hide()

                    if (showFromList) {
                        supportFinishAfterTransition()
                    } else {
                        finish()
                    }
                }
                Status.STATE_FAILURE -> {
                    binding.loadProgressBar.hide()
                    if (status.error is ABCException) {
                        showSnackBar(binding.root, status.error.stringRes)
                    } else {
                        showSnackBar(binding.root, R.string.error_general)
                    }
                }
            }
        }

        viewModel.surveySetting.observe(this) { surveySetting ->
            recyclerViewAdapter.bindData(
                    questions = surveySetting.survey.questions,
                    isAvailable = surveySetting.isAvailable,
                    showAltText = surveySetting.showEtc
            )
        }

        if (showFromList) {
            ViewCompat.setTransitionName(binding.txtHeader, sharedViewNameForTitle(entityId))
            ViewCompat.setTransitionName(binding.txtMessage, sharedViewNameForMessage(entityId))
            ViewCompat.setTransitionName(binding.txtDeliveredTime, sharedViewNameForDeliveredTime(entityId))
            window.allowReturnTransitionOverlap = true
            window.sharedElementEnterTransition.doOnEnd { load() }
        } else {
            load()
        }
    }

    private fun load() = viewModel.load(
            title = surveyTitle,
            message = surveyMessage,
            deliveredTime = surveyDeliveredTime,
            entityId = entityId
    )

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_survey_question, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            true
        }
        R.id.menu_activity_survey_question_save -> {
            YesNoDialogFragment.showDialog(
                    supportFragmentManager,
                    getString(R.string.dialog_title_save_immutable),
                    getString(R.string.dialog_message_save_immutable)) { isYes ->
                if (isYes) viewModel.store(entityId, reactionTime, System.currentTimeMillis())
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_ENTITY_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_ENTITY_ID"
        const val EXTRA_SHOW_FROM_LIST = "${BuildConfig.APPLICATION_ID}.EXTRA_SHOW_FROM_LIST"
        const val EXTRA_SURVEY_TITLE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_TITLE"
        const val EXTRA_SURVEY_MESSAGE = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_MESSAGE"
        const val EXTRA_SURVEY_DELIVERED_TIME = "${BuildConfig.APPLICATION_ID}.EXTRA_SURVEY_DELIVERED_TIME"
    }
}