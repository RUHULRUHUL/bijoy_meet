package video.meetting.app.activity

import video.core.extensions.*
import video.meetting.app.Bijoy
import video.meetting.app.R
import video.meetting.app.databinding.ActivityMainBinding
import video.meetting.app.model.Meeting
import video.meetting.app.sharedpref.AppPref
import video.meetting.app.utils.MeetingUtils
import video.meetting.app.viewmodel.MainViewModel
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.navigationInfoParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    private val viewModel by viewModel<MainViewModel>() // Lazy inject ViewModel
    private lateinit var binding: ActivityMainBinding

    private val minMeetingCodeLength = 10
    private var currentUser: FirebaseUser? = null
    private lateinit var createMeetingInterstitialAd: InterstitialAd
    private lateinit var joinMeetingInterstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = FirebaseAuth.getInstance().currentUser
        setProfileIcon()

        // Load ads based on configuration
        if (Bijoy.isAdEnabled) {
            initializeCreateMeetingInterstitialAd()
            loadCreateMeetingInterstitialAd()
            initializeJoinMeetingInterstitialAd()
            loadJoinMeetingInterstitialAd()
        }


        ///navigation drawer customization.................

       binding.constraintLayout.drawerLayout.nav_icon_id.setOnClickListener {
           binding.constraintLayout.drawerLayout.openDrawer(binding.navigationView,true)
       }
        binding.constraintLayout.drawerLayout.closeDrawer(binding.navigationView,true)


        binding.constraintLayout.navigationView.profile_id.setOnClickListener {
            val intent = Intent(this, Profile_Actiity::class.java)
            startActivity(intent)
        }



        handleDynamicLink()

        onMeetingToggleChange()
        onCreateMeetingCodeChange()
        onCopyMeetingCodeFromClipboardClick()
        onShareMeetingCodeClick()
        onJoinMeetingClick()
        onCreateMeetingClick()
        onMeetingHistoryClick()
        //onProfileClick()
    }

    private fun setProfileIcon() {
        currentUser?.let {
            if (it.photoUrl != null) {
                binding.drawerLayout.ivProfile.load(currentUser?.photoUrl) {
                    placeholder(R.drawable.ic_baseline_person_24)
                }
            }
        }
    }

    private fun initializeCreateMeetingInterstitialAd() {
        createMeetingInterstitialAd = InterstitialAd(this)
        createMeetingInterstitialAd.adUnitId = getString(R.string.interstitial_ad_id_create_meeting)

        // Reload ad once shown
        createMeetingInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                createMeeting(getCreateMeetingCode())
                loadCreateMeetingInterstitialAd()
            }
        }
    }

    private fun loadCreateMeetingInterstitialAd() {
        createMeetingInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    private fun initializeJoinMeetingInterstitialAd() {
        joinMeetingInterstitialAd = InterstitialAd(this)
        joinMeetingInterstitialAd.adUnitId = getString(R.string.interstitial_ad_id_join_meeting)

        // Reload ad once shown
        joinMeetingInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                joinMeeting(getJoinMeetingCode())
                loadJoinMeetingInterstitialAd()
            }
        }
    }

    private fun loadJoinMeetingInterstitialAd() {
        joinMeetingInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    private fun handleDynamicLink() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener { pendingDynamicLinkData ->
                val deepLink: Uri?
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                    deepLink?.getQueryParameter("meetingCode")?.let { joinMeeting(it) }
                }
            }
            .addOnFailureListener { _ ->
                toast(getString(R.string.main_error_fetch_dynamic_link))
            }
    }

    /**
     * Called when the meeting toggle button check state is changed
     */

    private fun onMeetingToggleChange() {
        binding.constraintLayout.drawerLayout.tgMeeting.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnToggleJoinMeeting   -> {
                        binding.constraintLayout.drawerLayout.groupCreateMeeting.makeGone()
                        binding.constraintLayout.drawerLayout.groupJoinMeeting.makeVisible()
                    }
                    R.id.btnToggleCreateMeeting -> {
                        binding.constraintLayout.drawerLayout.groupJoinMeeting.makeGone()
                        binding.constraintLayout.drawerLayout.groupCreateMeeting.makeVisible()
                        val meetingCode = generateMeetingCode()
                        binding.constraintLayout.drawerLayout.etCodeCreateMeeting.setText(meetingCode)
                    }
                }
            }
        }
    }

    /**
     * Called when the meeting code in the EditText of the CREATE MEETING toggle changes
     */
    private fun onCreateMeetingCodeChange() {
        binding.constraintLayout.drawerLayout.tilCodeCreateMeeting.etCodeCreateMeeting.doOnTextChanged { text, _, _, _ ->
            if (text.toString().trim()
                    .replace(" ", "").length >= minMeetingCodeLength
            ) binding.constraintLayout.drawerLayout.tilCodeCreateMeeting.error = null
        }
    }

    private fun generateMeetingCode(): String {
        val allowedChars = ('a'..'z') + ('0'..'9')
        return (1..10)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Called when the clipboard icon is clicked in the EditText of the JOIN MEETING toggle
     */
    private fun onCopyMeetingCodeFromClipboardClick() {
        binding.constraintLayout.drawerLayout.tilCodeJoinMeeting.setEndIconOnClickListener {
            val clipboardText = getTextFromClipboard()
            if (clipboardText != null) {
                binding.constraintLayout.drawerLayout.etCodeJoinMeeting.setText(clipboardText)
                toast(getString(R.string.main_meeting_code_copied))
            } else {
                toast(getString(R.string.main_empty_clipboard))
            }
        }
    }

    /**
     * Called when the share icon is clicked in the EditText of the CREATE MEETING toggle
     */
    private fun onShareMeetingCodeClick() {
        binding.constraintLayout.drawerLayout.tilCodeCreateMeeting.setEndIconOnClickListener {
            if (isMeetingCodeValid(getCreateMeetingCode())) {
                binding.constraintLayout.drawerLayout.tilCodeCreateMeeting.error = null
                toast(getString(R.string.main_creating_dynamic_link))

                Firebase.dynamicLinks.shortLinkAsync {
                    link = Uri.parse(getString(R.string.app_deep_link_url, getCreateMeetingCode()))
                    domainUriPrefix = getString(R.string.app_dynamic_link_url_prefix)
                    androidParameters {}
                    navigationInfoParameters {
                        forcedRedirectEnabled = true // Directly open the link in the app
                    }
                }.addOnSuccessListener { result ->
                    val shortDynamicLink = result.shortLink.toString()
                    startShareTextIntent(
                        getString(R.string.main_share_meeting_code_title),
                        getString(R.string.main_share_meeting_code_desc, shortDynamicLink)
                    )
                }.addOnFailureListener {
                    toast(getString(R.string.main_error_create_dynamic_link))
                }
            } else {
                binding.constraintLayout.drawerLayout.tilCodeCreateMeeting.error =
                    getString(R.string.main_error_meeting_code_length, minMeetingCodeLength)
            }
        }
    }

    /**
     * Called when the JOIN button is clicked of the JOIN MEETING toggle
     */
    private fun onJoinMeetingClick() {
        binding.constraintLayout.drawerLayout.btnJoinMeeting.setOnClickListener {
            if (isMeetingCodeValid(getJoinMeetingCode())) {
                if (Bijoy.isAdEnabled) {
                    if (joinMeetingInterstitialAd.isLoaded) joinMeetingInterstitialAd.show() else joinMeeting(
                        getJoinMeetingCode()
                    )
                } else {
                    joinMeeting(getJoinMeetingCode())
                }
            }
        }
    }

    private fun joinMeeting(meetingCode: String) {
        MeetingUtils.startMeeting(
            this,
            meetingCode,
            R.string.all_joining_meeting
        ) // Start Meeting

        viewModel.addMeetingToDb(
            Meeting(
                meetingCode,
                System.currentTimeMillis()
            )
        ) // Add meeting to db
    }

    /**
     * Returns the meeting code for joining the meeting
     */
    private fun getJoinMeetingCode() =
        binding.constraintLayout.drawerLayout.etCodeJoinMeeting.text.toString().trim().replace(" ", "")

    /**
     * Called when the CREATE button is clicked of the CREATE MEETING toggle
     */
    private fun onCreateMeetingClick() {
        binding.constraintLayout.drawerLayout.btnCreateMeeting.setOnClickListener {
            if (isMeetingCodeValid(getCreateMeetingCode())) {
                if (Bijoy.isAdEnabled) {
                    if (createMeetingInterstitialAd.isLoaded) createMeetingInterstitialAd.show() else createMeeting(
                        getCreateMeetingCode()
                    )
                } else {
                    createMeeting(getCreateMeetingCode())
                }
            }
        }
    }

    private fun createMeeting(meetingCode: String) {
        MeetingUtils.startMeeting(
            this,
            meetingCode,
            R.string.all_creating_meeting
        ) // Start Meeting

        viewModel.addMeetingToDb(
            Meeting(
                meetingCode,
                System.currentTimeMillis()
            )
        ) // Add meeting to db
    }

    private fun getCreateMeetingCode() =
        binding.constraintLayout.drawerLayout.etCodeCreateMeeting.text.toString().trim().replace(" ", "")

    private fun isMeetingCodeValid(meetingCode: String): Boolean {
        return if (meetingCode.length >= minMeetingCodeLength) {
            true
        } else {
            Snackbar.make(
                binding.constraintLayout.drawerLayout,
                getString(R.string.main_error_meeting_code_length, minMeetingCodeLength),
                Snackbar.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun onMeetingHistoryClick() {
        binding.constraintLayout.drawerLayout.ivMeetingHistory.setOnClickListener {
            MeetingHistoryActivity.startActivity(this)
        }
    }

//    private fun onProfileClick() {
//        binding.drawerLayout.ivProfile.setOnClickListener {
////            val profileDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
////                customView(R.layout.dialog_profile)
//
//            if (currentUser != null) {
//                currentUser!!.photoUrl?.let {
//                    ivUserProfileDialog.load(it) {
//                        placeholder(R.drawable.ic_profile)
//                    }
//                }
//                tvUserName.text = currentUser!!.displayName
//                tvEmail.text = currentUser!!.email
//                btnUserAuthenticationStatus.text = getString(R.string.all_btn_sign_out)
//            } else {
//                tvUserName.makeGone()
//                tvEmail.makeGone()
//                tvUserNotAuthenticated.makeVisible()
//                btnUserAuthenticationStatus.text = getString(R.string.all_btn_sign_in)
//            }
//            }
//
//            profileDialog.apply {
//                if (currentUser != null) {
//                    currentUser!!.photoUrl?.let {
//                        ivUserProfileDialog.load(it) {
//                            placeholder(R.drawable.ic_profile)
//                        }
//                    }
//                    tvUserName.text = currentUser!!.displayName
//                    tvEmail.text = currentUser!!.email
//                    btnUserAuthenticationStatus.text = getString(R.string.all_btn_sign_out)
//                } else {
//                    tvUserName.makeGone()
//                    tvEmail.makeGone()
//                    tvUserNotAuthenticated.makeVisible()
//                    btnUserAuthenticationStatus.text = getString(R.string.all_btn_sign_in)
//                }
//
//                switchDarkMode.isChecked = !AppPref.isLightThemeEnabled
//
//                // UserAuthenticationStatus button onClick
//                btnUserAuthenticationStatus.setOnClickListener {
//                    dismiss()
//
//                    if (currentUser != null) {
//                        // User is currently signed in
//                        AuthUI.getInstance().signOut(this@MainActivity).addOnCompleteListener {
//                            AuthenticationActivity.startActivity(this@MainActivity)
//                            finish()
//                        }
//                    } else {
//                        // User is not signed in
//                        AuthenticationActivity.startActivity(this@MainActivity)
//                        finish()
//                    }
//                }
//
//                // Dark Mode Switch
//                switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
//                    dismiss()
//
//                    // Change theme after dismiss to prevent memory leak
//                    onDismiss {
//                        if (isChecked) setThemeMode(AppCompatDelegate.MODE_NIGHT_YES) else setThemeMode(
//                            AppCompatDelegate.MODE_NIGHT_NO
//                        )
//                    }
//                }
//
//                // Send feedback onClick
//                tvSendFeedback.setOnClickListener {
//                    startEmailIntent(
//                        getString(R.string.app_feedback_contact_email),
//                        getString(R.string.profile_feedback_email_subject)
//                    )
//                }
//
//                // Rate app onClick
//                tvRateApp.setOnClickListener {
//                    openAppInGooglePlay(applicationContext.packageName, R.color.colorSurface)
//                }
//
//                // Share app onClick
//                tvShareApp.setOnClickListener {
//                    startShareTextIntent(
//                        getString(R.string.profile_share_app_title),
//                        getString(R.string.profile_share_app_text, applicationContext.packageName)
//                    )
//                }
//
//                // FAQs onClick
//                tvFaqs.setOnClickListener {
//                    FaqsActivity.startActivity(this@MainActivity)
//                }
//
//                // Open Source Licenses onClick
//                tvOpenSourceLicenses.setOnClickListener {
//                    startActivity(Intent(this@MainActivity, OssLicensesMenuActivity::class.java))
//                }
//
//                // Privacy Policy onClick
//                tvPrivacyPolicy.setOnClickListener {
//                    openUrl(getString(R.string.app_privacy_policy_url), R.color.colorSurface)
//                }
//
//                // Terms of Service onClick
//                tvTermsOfService.setOnClickListener {
//                    openUrl(getString(R.string.app_terms_of_service_url), R.color.colorSurface)
//                }
//            }
//        }
//    }

    private fun setThemeMode(themeMode: Int) {
        AppCompatDelegate.setDefaultNightMode(themeMode)
        AppPref.isLightThemeEnabled = themeMode == AppCompatDelegate.MODE_NIGHT_NO
    }
}
