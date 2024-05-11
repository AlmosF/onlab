/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsung.android.scan3d.fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.android.camera.utils.OrientationLiveData
import com.samsung.android.scan3d.CameraActivity
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.databinding.FragmentCameraBinding
import com.samsung.android.scan3d.serv.CamEngine
import kotlinx.parcelize.Parcelize
import java.io.IOException
import java.util.Date


import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class CameraFragment : Fragment() {

    /** Android ViewBinding */
    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private var isFirstRun = true

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    var Sresponse : String? = null

    /** AndroidX navigation arguments */
    //  private val args: CameraFragmentArgs by navArgs()

    //var resW = 1280
    //var resH = 720
    var resW = 2560
    var resH = 1440

    var viewState =
        ViewState(true, stream = false, cameraId = "0", quality = 80, resolutionIndex = null)

    lateinit var Cac: CameraActivity

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        // Get the local ip address
        //val localIp = IpUtil.getLocalIpAddress()
        /*_fragmentCameraBinding!!.textView6.text = "$localIp:8080/cam.mjpeg"
        _fragmentCameraBinding!!.textView6.setOnClickListener {
            // Copy the ip address to the clipboard
            ClipboardUtil.copyToClipboard(context, "ip", _fragmentCameraBinding!!.textView6.text.toString())
            // Toast to notify the user
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }*/

        Cac = (activity as CameraActivity?)!!
        return fragmentCameraBinding.root
    }


    fun sendPost(address: String, port: String){
        val requestBody  = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("data","${Date()}")
            .addFormDataPart("usernamee", fragmentCameraBinding.username.text.toString())
            .addFormDataPart("passwordd", fragmentCameraBinding.password.text.toString())
            .build()

        if (address == "" || port == "") {
            Toast.makeText(context, "Please enter an IP address and port", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://$address:3001/"
        val url3 = "http://$address:$port/"
        val url2 = "http://vm.fured.cloud.bme.hu:1444/"

        val request = Request.Builder()
            //.url("http://13.50.233.145:3001/")
            .url(url3)
            .post(requestBody)
            .build()


        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {


                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val serverResponse = response.body?.string()
                //Log.d("Response", "onResponse: ${response.body?.string()}")


                Sresponse = serverResponse
                if (Sresponse != null) {
                    if (Sresponse != "Invalid Credentials"){
                        activity?.runOnUiThread(Runnable {
                            // Stuff that updates the UI
                            fragmentCameraBinding.button2.isEnabled = true

                            fragmentCameraBinding.button2.setBackgroundColor(Color.GREEN)
                            //fragmentCameraBinding.ftFeedback.text = "Connected to $address:$port"
                            fragmentCameraBinding.ftFeedback.text = "Connected to $address:$port\nCode: $Sresponse"
                            Log.d("Response", "onResponse: $Sresponse")
                            fragmentCameraBinding.ftFeedback.setTextColor(Color.GREEN)
                        })
                    }else{
                        activity?.runOnUiThread(Runnable {
                            // Stuff that updates the UI
                            fragmentCameraBinding.button2.isEnabled = false
                            fragmentCameraBinding.ftFeedback.text = "Invalid Credentials"
                            fragmentCameraBinding.ftFeedback.setTextColor(Color.RED)
                        })
                    }
                }

            }
        })
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {


            /*intent.extras?.getParcelable<CamEngine.Companion.DataQuick>("dataQuick")?.apply {
                activity?.runOnUiThread(Runnable {
                    // Stuff that updates the UI
                    fragmentCameraBinding.qualFeedback?.text =
                        " " + this.rateKbs + "kB/sec"
                    fragmentCameraBinding.ftFeedback?.text =
                        " " + this.ms + "ms"
                })

            }*/



            val data = intent.extras?.getParcelable<CamEngine.Companion.Data>("data") ?: return


            val re = data.resolutions[data.resolutionSelected]
            resW = re.width
            resH = re.height

            activity?.runOnUiThread(Runnable {

                fragmentCameraBinding.viewFinder.setAspectRatio(resW, resH)
                Log.d("Resolution", "onReceive: $resW x $resH")
            })


            fragmentCameraBinding.button.setOnClickListener {
                val ip = fragmentCameraBinding.editTextIP.text.toString()
                val port = fragmentCameraBinding.editTextPort.text.toString()
                sendPost(ip, port)
                //sendPost(fragmentCameraBinding.editTextIP.text.toString())
            }

            val blinkAnimator = ObjectAnimator.ofFloat(fragmentCameraBinding.streaming, "alpha", 0f, 1f).apply {
                duration = 1000 // duration of the animation in milliseconds
                repeatCount = ObjectAnimator.INFINITE // repeat the animation indefinitely
                repeatMode = ObjectAnimator.REVERSE // reverse the animation at the end
                interpolator = LinearInterpolator() // change the animation speed linearly
            }

            fragmentCameraBinding.button2.setOnClickListener {
                if (viewState.stream) {
                    viewState.stream = false
                    fragmentCameraBinding.button2.text = "Start Stream"
                    fragmentCameraBinding.button2.setBackgroundColor(Color.GREEN)

                    fragmentCameraBinding.streaming.visibility = View.INVISIBLE
                    blinkAnimator.cancel()


                    sendViewState()
                } else {
                    viewState.stream = true
                    fragmentCameraBinding.button2.text = "Stop Stream"
                    fragmentCameraBinding.button2.setBackgroundColor(Color.RED)

                    blinkAnimator.start()
                    fragmentCameraBinding.streaming.visibility = View.VISIBLE
                    sendViewState()

                }
            }

            fragmentCameraBinding.switchCameraFab.setOnClickListener {
                if (viewState.cameraId == "0") {
                    viewState.cameraId = "1"
                } else {
                    viewState.cameraId = "0"
                }
                sendViewState()
            }

            /*run {
                val spinner = fragmentCameraBinding.spinnerCam
                val spinnerDataList = ArrayList<Any>()
                data.sensors.forEach { spinnerDataList.add(it.title) }
                val spinnerAdapter =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        spinnerDataList
                    )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner!!.adapter = spinnerAdapter
                spinner.setSelection(data.sensors.indexOf(data.sensorSelected))
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

                        if (viewState.cameraId != data.sensors[p2].cameraId) {
                            viewState.resolutionIndex = null
                        }

                        viewState.cameraId = data.sensors[p2].cameraId


                        sendViewState()
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }
                }
            }*/

            /*run {
                val spinner = fragmentCameraBinding.spinnerQua
                val spinnerDataList = ArrayList<Any>()
                val quals = arrayOf(1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
                quals.forEach { spinnerDataList.add(it.toString()) }
                // Initialize the ArrayAdapter
                val spinnerAdapter =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        spinnerDataList
                    )
                // Set the dropdown layout style
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Set the adapter for the Spinner
                spinner!!.adapter = spinnerAdapter
                spinner.setSelection(quals.indexOfFirst { it == viewState.quality })
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        viewState.quality = quals[p2]
                        sendViewState()
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }
                }
            }*/

            /*run {

                val outputFormats = data.resolutions

                val spinner = fragmentCameraBinding.spinnerRes
                val spinnerDataList = ArrayList<Any>()
                outputFormats.forEach { spinnerDataList.add(it.toString()) }
                // Initialize the ArrayAdapter
                val spinnerAdapter =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        spinnerDataList
                    )
                // Set the dropdown layout style
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Set the adapter for the Spinner
                spinner!!.adapter = spinnerAdapter


                if (viewState.resolutionIndex == null) {
                    Log.i("DEUIBGGGGGG", "NO PRIOR R, " + data.resolutionSelected)
                    viewState.resolutionIndex = data.resolutionSelected
                }

                spinner.setSelection(viewState.resolutionIndex!!)
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        resW = outputFormats[p2].width
                        resH = outputFormats[p2].height
                        activity?.runOnUiThread(Runnable {

                            fragmentCameraBinding.viewFinder.setAspectRatio(resW, resH)
                        })
                        if (p2 != viewState.resolutionIndex) {
                            viewState.resolutionIndex = p2
                            sendViewState()
                        }

                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }
                }
            }*/

        }
    }


    fun sendViewState() {
        Cac.sendCam {
            it.action = "new_view_state"

            it.putExtra("data", viewState)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i("onPause", "onPause")
        activity?.unregisterReceiver(receiver)
        viewState.stream = false
        fragmentCameraBinding.button2.text = "Start Stream"
        fragmentCameraBinding.button2.setBackgroundColor(Color.GREEN)
        //sendViewState()
    }

    override fun onResume() {
        super.onResume()
        Log.i("onResume", "onResume")
        activity?.registerReceiver(receiver, IntentFilter("UpdateFromCameraEngine"))
        //viewState.stream = true
        fragmentCameraBinding.button2.text = "Start Stream"
        fragmentCameraBinding.button2.setBackgroundColor(Color.GREEN)
    }


    /*@SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        Log.i("onResume", "onResume")
        activity?.registerReceiver(receiver, IntentFilter("UpdateFromCameraEngine"))
    }*/


    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("onViewCreated", "onViewCreated")


        Cac.sendCam {
            it.action = "start_camera_engine"
        }
        // engine.start(requireContext())

        Log.i("CAMMM", "fragmentCameraBinding.buttonKill " + fragmentCameraBinding.buttonKill)
        fragmentCameraBinding.buttonKill.setOnClickListener {
            Log.i("CameraFrag", "KILL")
            val intent = Intent("KILL") //FILTER is a string to identify this intent
            context?.sendBroadcast(intent)
        }

        fragmentCameraBinding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                fragmentCameraBinding.viewFinder.setAspectRatio(
                    resW, resH
                )
                Cac.sendCam {
                    it.action = "new_preview_surface"
                    it.putExtra("surface", fragmentCameraBinding.viewFinder.holder.surface)
                }
            }
        })
        activity?.registerReceiver(receiver, IntentFilter("UpdateFromCameraEngine"))
    }

    /*override fun onStop() {
        super.onStop()
        try {

        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }*/

    /*override fun onDestroy() {
        super.onDestroy()

    }*/

    /*override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
    }*/

    companion object {
        private val TAG = CameraFragment::class.java.simpleName

        @Parcelize
        data class ViewState(
            var preview: Boolean,
            var stream: Boolean,
            var cameraId: String,
            var resolutionIndex: Int?,
            var quality: Int
        ) : Parcelable

    }
}
