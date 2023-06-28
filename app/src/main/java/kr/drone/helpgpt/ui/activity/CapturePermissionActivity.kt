package kr.drone.helpgpt.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import kr.drone.helpgpt.service.AudioCaptureService

class CapturePermissionActivity: AppCompatActivity() {

    private val translationResumeLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val audioCaptureIntent = Intent(this, AudioCaptureService::class.java).apply {
                    action = AudioCaptureService.ACTION_RESUME_RECORD
                    if(it.data!=null){
                        putExtra(AudioCaptureService.EXTRA_RESULT_DATA, it.data)
                    }
                }
                startForegroundService(audioCaptureIntent)
            } else {
                Toast.makeText(
                    this, "Request to obtain MediaProjection denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkRequestedPermission(listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO)) {
            val mediaProjectionManager =
                applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            translationResumeLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            finish()
        }
    }

    private fun checkRequestedPermission(permissions:List<String>, granted: ()->Unit){
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    granted()
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    Toast.makeText(
                        this@CapturePermissionActivity,
                        "Permission Denied\n$deniedPermissions",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            .setPermissions(*permissions.toTypedArray()).check()
    }
}