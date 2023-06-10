import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.webrtc.*
import org.json.JSONObject

var remoteSdp = ""

@Composable
fun HomePage() {
    val context = LocalContext.current

    val rootEglBase = EglBase.create()

    val LOCAL_TRACK_ID = "local_track"

    val LOCAL_STREAM_ID = "local_track"

    val options = PeerConnectionFactory.InitializationOptions.builder(context)
        .setEnableInternalTracer(true)
        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
        .createInitializationOptions()
    PeerConnectionFactory.initialize(options)

    val peerConnectionFactory = PeerConnectionFactory
        .builder()
        .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
        .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
        .setOptions(PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = true
        })
        .createPeerConnectionFactory()

    val videoCapturer = Camera2Enumerator(context).run {
        deviceNames.find {
            isFrontFacing(it)
        }?.let {
            createCapturer(it, null)
        } ?: throw IllegalStateException()
    }

    val localVideoSource = peerConnectionFactory.createVideoSource(false)

//    val rtcConfig = PeerConnection.RTCConfiguration(listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()))

    val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:numb.viagenie.ca")
            .setUsername("sultan1640@gmail.com")
            .setPassword("98376683")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:numb.viagenie.ca")
            .setUsername("sultan1640@gmail.com")
            .setPassword("98376683")
            .createIceServer()
    )

    val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

    val localVideoOutput = remember { SurfaceViewRenderer(context) }

    val remoteVideoOutput = remember { SurfaceViewRenderer(context) }

    val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)

    val localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)

    // Implement the PeerConnection.Observer
    class MyPeerConnectionObserver : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            // Called when an ICE candidate is gathered
            println("ICE candidate received")

            println(candidate)
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            // Called when ICE candidates have been removed
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {

        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            // Called when the ICE connection state changes
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
            // Called when the PeerConnection state changes
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            // Called when the ICE connection receiving state changes
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
//            if(p0 === PeerConnection.IceGatheringState.COMPLETE){
//                println("Ice gathering complete")
//            }
        }

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
            // Called when the selected ICE candidate pair changes
        }

        override fun onAddStream(stream: MediaStream) {
            // Called when a new MediaStream is added
//            println("Streams are passing by")
//
//            println(stream.videoTracks)

            stream.videoTracks?.get(0)?.addSink(remoteVideoOutput)
        }

        override fun onRemoveStream(stream: MediaStream) {
            // Called when a MediaStream is removed
        }

        override fun onDataChannel(channel: DataChannel) {
            // Called when a new DataChannel is created
        }

        override fun onRenegotiationNeeded() {
            // Called when renegotiation is needed (for example, when adding or removing tracks)
        }

        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            // Called when a new track is added
        }
    }

    val observer = MyPeerConnectionObserver()

    val peerConnection = peerConnectionFactory.createPeerConnection(
        rtcConfig,
        observer
    )

    var sdpValue by remember { mutableStateOf("None") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Local Stream",
            modifier= Modifier.padding(20.dp)
        )

        AndroidView(
            factory = { localVideoOutput },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            // start capturing the video frames
            (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, it.context, localVideoSource.capturerObserver)
            videoCapturer.startCapture(320, 240, 60)

            // create a local media stream and add the video track to it
            val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)

            localStream.addTrack(localVideoTrack)

            // add the video track as a sink for the SurfaceViewRenderer
            localVideoTrack?.addSink(it)

            // add the local media stream to the peer connection
            peerConnection?.addStream(localStream)

            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            }

            peerConnection?.createOffer(object: CustomSdpObserver(){
//                override fun onSetSuccess() {
//                    super.onSetSuccess()
//                }

                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    println("Create offer success ${sessionDescription}")

                    val json = JSONObject()

                    json.put("type", "offer")

                    json.put("sdp", sessionDescription?.description)

                    sdpValue = json.toString()

                    peerConnection.setLocalDescription(object : CustomSdpObserver() {
                        override fun onSetSuccess() {
                            println("Set local description success")
                        }

                        override fun onSetFailure(error: String?) {
                            println("Set local description failed: $error")
                        }
                    }, sessionDescription)
                }
                override fun onCreateFailure(error: String?) {
                    println("Create offer failed: $error")
                }

//                override fun onSetFailure(p0: String?) {
//                    super.onSetFailure(p0)
//                }
            }, constraints)
        }

        Text(
            text = "Remote Stream",
            modifier= Modifier.padding(20.dp)
        )

        AndroidView(
            factory = { remoteVideoOutput },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Text(
            "Session Description",
            modifier= Modifier.padding(20.dp)
        )

        SelectionContainer{
            Text(
                text= sdpValue,
                modifier= Modifier.padding(20.dp)
            )
        }

        Text(
            "Remote Description",
            modifier= Modifier.padding(20.dp)
        )

        RemoteDescriptionField()

        TextButton(onClick = {
            val jsonString = remoteSdp

            val json = JSONObject(jsonString)

            val type = json.getString("type")

            val sdp = json.getString("description")

            val sessionDescription = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)

            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {

                }

                override fun onSetSuccess() {
                    println("remote description has been set")
                }

                override fun onCreateFailure(p0: String?) {

                }

                override fun onSetFailure(p0: String?) {

                }

            }, sessionDescription)
        }) {
           Text("Set Remote Description")
        }

        DisposableEffect(Unit) {
            remoteVideoOutput.run {
                setEnableHardwareScaler(true)
                setMirror(true)
                init(rootEglBase.eglBaseContext, null)
            }

            localVideoOutput.setMirror(true)
            localVideoOutput.setEnableHardwareScaler(true)
            localVideoOutput.init(rootEglBase.eglBaseContext, null)

            onDispose{
                localVideoTrack?.removeSink(localVideoOutput)
                videoCapturer.stopCapture()
                surfaceTextureHelper.dispose()
                localVideoOutput.release()
                remoteVideoOutput.release()
            }
        }
    }
}

@Composable
fun RemoteDescriptionField(){
    var remoteDescriptionValue by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ){
        TextField(
            value= remoteDescriptionValue,
            onValueChange = {
                remoteDescriptionValue = it
                remoteSdp = it
            }
        )
    }
}

abstract class CustomSdpObserver : SdpObserver {
    override fun onSetFailure(p0: String?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onCreateSuccess(p0: SessionDescription?) {}
}
