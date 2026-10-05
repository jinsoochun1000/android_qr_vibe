package kr.co.tkinfo.qr26

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kr.co.tkinfo.qr26.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cameraController: LifecycleCameraController? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var cameraStarted = false
    private var askedForPermission = false
    private var analysisErrorShown = false
    private var lastValue: String? = null
    private var lastFormat: String? = null
    private var toneGenerator: ToneGenerator? = null

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hidePermissionPanel()
            startCamera()
        } else {
            val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            showPermissionPanel(permanentlyDenied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rawValue.movementMethod = ScrollingMovementMethod.getInstance()
        ViewCompat.setOnApplyWindowInsetsListener(binding.resultContent) { view, insets ->
            val navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val baseBottom = (12 * resources.displayMetrics.density).toInt()
            view.updatePadding(bottom = navigationBar.bottom + baseBottom)
            insets
        }
        showWaiting()
        binding.copyButton.setOnClickListener { copyResult() }
        binding.requestPermissionButton.setOnClickListener {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
        binding.openSettingsButton.setOnClickListener { openAppSettings() }
    }

    override fun onStart() {
        super.onStart()
        when {
            hasCameraPermission() -> {
                hidePermissionPanel()
                startCamera()
            }
            !askedForPermission -> {
                askedForPermission = true
                requestPermission.launch(Manifest.permission.CAMERA)
            }
            else -> {
                val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                showPermissionPanel(permanentlyDenied)
            }
        }
    }

    override fun onDestroy() {
        cameraController?.unbind()
        super.onDestroy()
        barcodeScanner?.close()
        barcodeScanner = null
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        if (cameraStarted || !hasCameraPermission()) return

        val scanner = createBarcodeScanner()
        val controller = LifecycleCameraController(this)
        controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            MlKitAnalyzer(
                listOf(scanner),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this),
            ) { result ->
                handleScanResult(scanner, result)
            }
        )

        try {
            controller.bindToLifecycle(this)
        } catch (_: IllegalStateException) {
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show()
            return
        }

        controller.initializationFuture.addListener({
            try {
                controller.initializationFuture.get()
            } catch (_: Exception) {
                Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))

        binding.previewView.controller = controller
        cameraController = controller
        cameraStarted = true
    }

    private fun createBarcodeScanner(): BarcodeScanner {
        barcodeScanner?.let { return it }
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
        return BarcodeScanning.getClient(options).also { barcodeScanner = it }
    }

    private fun handleScanResult(scanner: BarcodeScanner, result: MlKitAnalyzer.Result?) {
        if (result == null) return

        val failure = result.getThrowable(scanner)
        if (failure != null) {
            if (!analysisErrorShown) {
                analysisErrorShown = true
                Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_LONG).show()
            }
            return
        }

        val barcodes = result.getValue(scanner).orEmpty()
        val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() } ?: return
        val value = barcode.rawValue ?: return
        val format = formatName(barcode.format)
        if (value == lastValue && format == lastFormat) return

        lastValue = value
        lastFormat = format
        showResult(format, value)
        playBeep()
    }

    private fun playBeep() {
        try {
            val tone = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 90).also {
                toneGenerator = it
            }
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        } catch (_: RuntimeException) {
            toneGenerator = null
        }
    }

    private fun showWaiting() {
        binding.formatValue.setText(R.string.format_placeholder)
        binding.rawValue.setText(R.string.hint_point_camera)
        binding.copyButton.isEnabled = false
    }

    private fun showResult(format: String, value: String) {
        binding.formatValue.text = format
        binding.rawValue.text = value
        binding.copyButton.isEnabled = true
    }

    private fun copyResult() {
        val value = lastValue ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("barcode", value))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionPanel(permanentlyDenied: Boolean) {
        binding.permissionPanel.visibility = View.VISIBLE
        binding.permissionMessage.setText(
            if (permanentlyDenied) R.string.permission_denied else R.string.permission_message
        )
        binding.requestPermissionButton.visibility = if (permanentlyDenied) View.GONE else View.VISIBLE
    }

    private fun hidePermissionPanel() {
        binding.permissionPanel.visibility = View.GONE
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    private fun formatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            else -> getString(R.string.format_unknown)
        }
    }
}
