package video.meetting.app.activity

import android.content.Intent
import video.meetting.app.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import video.core.extensions.makeGone
import video.core.extensions.makeVisible
import coil.api.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_profile__actiity.*

class Profile_Actiity : AppCompatActivity() {
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile__actiity)

        currentUser = FirebaseAuth.getInstance().currentUser



        val username: TextView = findViewById(R.id.username_id) as TextView
        val useremail: TextView = findViewById(R.id.useremail_id) as TextView
        val userphoto: CircleImageView = findViewById(R.id.profilepic_id) as CircleImageView
        val Signinbt: Button = findViewById(R.id.signin_id) as Button

        val backarrow:ImageView=findViewById(R.id.backarro_id) as ImageView
        backarrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }



            if (currentUser != null) {
//                currentUser!!.photoUrl?.let {
//                    ivUserProfileDialog.load(it) {
//                        placeholder(R.drawable.ic_profile)
//                    }
//                }

                username.text = currentUser!!.displayName
                useremail.text = currentUser!!.email
                currentUser!!.photoUrl?.let {
                    profilepic_id.load(it) {
                        placeholder(R.drawable.ic_profile)
                    }
                    //ConstraintLayout.profilepic_id.text = currentUser!!.displayName
                    //tvEmail.text = currentUser!!.email


                }
                signin_id.makeGone()
            }
            else {
                signin_id.makeVisible()

            }

        signin_id.setOnClickListener {
            val intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
        }
//                tvUserName.makeGone()
//                tvEmail.makeGone()
//                tvUserNotAuthenticated.makeVisible()
//                btnUserAuthenticationStatus.text = getString(R.string.all_btn_sign_in)
            }



}