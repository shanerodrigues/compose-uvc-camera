package com.example.camerauvctest

import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camerauvctest.ui.theme.CameraUVCTestTheme
import com.google.accompanist.permissions.*
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.utils.ToastUtils
import androidx.compose.foundation.layout.Arrangement

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraUVCTestTheme {

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    RequestMultiplePermissions(
                        permissions = listOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.ACCESS_NETWORK_STATE,
                            android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.INTERNET,
                            android.Manifest.permission.RECORD_AUDIO,
                        )
                    )

               }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
fun RequestMultiplePermissions(
    permissions: List<String>,
    deniedMessage: String = "This app requires the camera and access to storage. If it doesn't work, then you'll have to do it manually from the settings.",
    rationaleMessage: String = "To use this app's functionalities, you need to give us the permission.",
) {
    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    HandleRequests(
        multiplePermissionsState = multiplePermissionsState,
        deniedContent = { shouldShowRationale ->
            PermissionDeniedContent(
                deniedMessage = deniedMessage,
                rationaleMessage = rationaleMessage,
                shouldShowRationale = shouldShowRationale,
                onRequestPermission = { multiplePermissionsState.launchMultiplePermissionRequest() }
            )
        },
        content = {
            Content(
                text = "PERMISSION GRANTED!",
                showButton = false
            ) {}
        }
    )
}

@ExperimentalPermissionsApi
@Composable
private fun HandleRequests(
    multiplePermissionsState: MultiplePermissionsState,
    deniedContent: @Composable (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var shouldShowRationale by remember { mutableStateOf(false) }
    val result = multiplePermissionsState.permissions.all {
        shouldShowRationale = it.status.shouldShowRationale
        it.status == PermissionStatus.Granted
    }
    if (result) {
        Toast.makeText(LocalContext.current, "Permission granted successfully", Toast.LENGTH_SHORT).show()
//        YourApp()
        MyApp()
    } else {
        deniedContent(shouldShowRationale)
    }
}

@ExperimentalPermissionsApi
@Composable
fun PermissionDeniedContent(
    deniedMessage: String,
    rationaleMessage: String,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    if (shouldShowRationale) AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "Permission Request",
                style = TextStyle(
                    fontSize = MaterialTheme.typography.h6.fontSize,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(rationaleMessage)
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Give Permission")
            }
        }
    ) else Content(text = deniedMessage, onClick = onRequestPermission)

}

@Composable
fun Content(text: String, showButton: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        if (showButton) {
            Button(onClick = onClick) {
                Text(text = "Request")
            }
        }
    }
}

@Composable
fun rememberCameraClient(context: Context): CameraClient = remember {
    CameraClient.newBuilder(context).apply {
        setEnableGLES(true)
        setRawImage(false)
        setCameraStrategy(CameraUvcStrategy(context))
        setCameraRequest(
            CameraRequest.Builder()
                .setFrontCamera(false)
                .setPreviewWidth(640)
                .setPreviewHeight(480)
                .create()
        )
        openDebug(true)
    }.build()
}


@Composable
fun MyApp(){
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth()) {
        UVCCameraPreview(rememberCameraClient(context))
    }
}

@Composable
fun UVCCameraPreview(cameraClient : CameraClient) {
    AndroidView(
        factory = { ctx ->
            AspectRatioSurfaceView(ctx).apply {
                this.holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        cameraClient.openCamera(this@apply)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        cameraClient.setRenderSize(width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        cameraClient.closeCamera()
                    }
                })
            }
        }
    ) {
    }
    var currentContext = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Button(onClick = {captureImage(cameraClient, currentContext)}) {
            Text("Take Picture")
        }
    }

}

fun captureImage(cameraClient: CameraClient, context:Context){

    cameraClient.captureImage(object : ICaptureCallBack {
        override fun onBegin() {
            Toast.makeText(context, "onBegin", Toast.LENGTH_SHORT).show()
            Log.i("CameraClient", "onBegin")

        }

        override fun onError(error: String?) {
            Toast.makeText(context, "onError", Toast.LENGTH_SHORT).show()
            ToastUtils.show(error ?: "未知异常")
            Log.i("CameraClient", "onError")
        }

        override fun onComplete(path: String?) {
            Toast.makeText(context, "onComplete", Toast.LENGTH_SHORT).show()
            ToastUtils.show("OnComplete")
            Log.i("CameraClient", "onComplete")
        }
    })
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CameraUVCTestTheme {
        Greeting("Android")
    }
}