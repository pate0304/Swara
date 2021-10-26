package dev.irenicj.swara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import butterknife.Bind
import butterknife.ButterKnife
import butterknife.OnClick
import com.voxeet.VoxeetSDK
import com.voxeet.promise.solve.*
import com.voxeet.sdk.json.ParticipantInfo
import com.voxeet.sdk.json.internal.ParamsHolder
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.services.builders.ConferenceCreateOptions
import com.voxeet.sdk.services.builders.ConferenceJoinOptions
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {


    val views: List<View> = ArrayList()
    val buttonsNotLoggedIn: List<View> = ArrayList()
    val buttonsInConference: List<View> = ArrayList()
    val buttonsNotInConference: List<View> = ArrayList()
    val buttonsInOwnVideo: List<View> = ArrayList()
    val buttonsNotInOwnVideo: List<View> = ArrayList()
    val buttonsInOwnScreenShare: List<View> = ArrayList()
    val buttonsNotInOwnScreenShare: List<View> = ArrayList()

    @Bind(R.id.user_name)
    var user_name: EditText? = null

    @Bind(R.id.conference_name)
    var conference_name: EditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this);


        //we now initialize the sdk
        VoxeetSDK.initialize("RG9HU6JS_JYTp9gK23LfzA==", "lMl-ArdJVJ3Bt7Bbkl_-OOIqrxQb0UKs0yht0Id0JTU=");

        //adding the user_name, login and logout views related to the open/close and conference flow
        add(views as MutableList<View>, R.id.login)
        add(views, R.id.logout)

        add(buttonsNotLoggedIn as MutableList<View>, R.id.login)
        add(buttonsNotLoggedIn, R.id.user_name)

        add(buttonsInConference as MutableList<View>, R.id.logout)

        add(buttonsNotInConference as MutableList<View>, R.id.logout)


        // Add the leave button and enable it only while in a conference
        add(views, R.id.leave);
        add(buttonsInConference, R.id.leave);

        // Set a random user name
        val avengersNames = arrayOf(
                "Thor",
                "Cap",
                "Tony Stark",
                "Black Panther",
                "Black Widow",
                "Hulk",
                "Spider-Man")
        val r = Random()
        user_name?.setText(avengersNames[r.nextInt(avengersNames.size)])

        // Add the join button and enable it only when not in a conference
        add(views, R.id.join);
        add(buttonsNotInConference, R.id.join);

        // Set a default conference name
        conference_name?.setText("Avengers meeting");
    }

    override fun onResume() {
        super.onResume()
        //here will be put the permission check

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 0x20)
        }
        //register the current activity in the SDK
        VoxeetSDK.instance().register(this);


        //we update the various views to enable or disable the ones we want to
        updateViews()

    }


    private fun updateViews() {
        //this method will be updated step by step
        //disable every view
        setEnabled(views, false);

        //if the user is not connected, we will only enabled the not logged in buttons
        if (!VoxeetSDK.session().isSocketOpen()) {
            setEnabled(buttonsNotLoggedIn, true);
            return;
        }


        val current = VoxeetSDK.conference().currentConference
        //we can now add the logic to manage our basic state
        //we can now add the logic to manage our basic state
        if (null != current && VoxeetSDK.conference().isLive) {
            setEnabled(buttonsInConference, true)
        } else {
            setEnabled(buttonsNotInConference, true)
        }
    }



    private fun setEnabled(views: List<View>, enabled: Boolean) {
        for (view in views) view.isEnabled = enabled
    }

    private fun add(list: MutableList<View>, id: Int): MainActivity? {
        list.add(findViewById(id))
        return this
    }


    @OnClick(R.id.login)
    fun onLogin() {
        VoxeetSDK.session().open(ParticipantInfo(user_name!!.text.toString(), "", ""))
                .then { result: Boolean?, solver: Solver<Any?>? ->
                    Toast.makeText(this@MainActivity, "log in successful", Toast.LENGTH_SHORT).show()
                    updateViews()
                }
                .error(error())
    }


    @OnClick(R.id.logout)
    fun onLogout() {
        VoxeetSDK.session().close()
                .then { result: Boolean?, solver: Solver<Any?>? ->
                    Toast.makeText(this@MainActivity, "logout done", Toast.LENGTH_SHORT).show()
                    updateViews()
                }.error(error())
    }

    @OnClick(R.id.join)
    fun onJoin() {
        val paramsHolder = ParamsHolder()
        paramsHolder.setDolbyVoice(true)

        val conferenceCreateOptions = ConferenceCreateOptions.Builder()
                .setConferenceAlias(conference_name!!.text.toString())
                .setParamsHolder(paramsHolder)
                .build()

        VoxeetSDK.conference().create(conferenceCreateOptions)
                .then<ThenPromise<Conference?, Conference>>(ThenPromise<Conference?, Conference> { conference: Conference? ->
                    val conferenceJoinOptions: ConferenceJoinOptions = ConferenceJoinOptions.Builder(conference!!)
                            .build()
                    VoxeetSDK.conference().join(conferenceJoinOptions)
                } as ThenPromise<Conference?, Conference>)
                .then<Any>(ThenVoid<ThenPromise<Conference?, Conference>> { conference: ThenPromise<Conference?, Conference>? ->
                    Toast.makeText(this@MainActivity, "started...", Toast.LENGTH_SHORT).show()
                    updateViews()
                })
                .error { error_in: Throwable? -> Toast.makeText(this@MainActivity, "Could not create conference", Toast.LENGTH_SHORT).show() }
    }


    @OnClick(R.id.leave)
    fun onLeave() {
        VoxeetSDK.conference().leave()
                .then { result: Boolean?, solver: Solver<Any?>? ->
                    updateViews()
                    Toast.makeText(this@MainActivity, "left...", Toast.LENGTH_SHORT).show()
                }.error(error())
    }
    override fun onPause() {
        //register the current activity in the SDK
        VoxeetSDK.instance().unregister(this)
        super.onPause()
    }


    private fun error(): ErrorPromise? {
        return ErrorPromise { error ->
            Toast.makeText(this@MainActivity, "ERROR...", Toast.LENGTH_SHORT).show()
            error.printStackTrace()
            updateViews()
        }
    }
}