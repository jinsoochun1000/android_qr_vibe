package kr.co.tkinfo.qrgemini38f

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kr.co.tkinfo.qrgemini38f.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    private var camera: Camera? = null
    private var isTorchOn: Boolean = false
    private var barcodeAnalyzer: BarcodeAnalyzer? = null

    // 비프음 재생용 ToneGenerator (100% 음량)
    private var toneGenerator: ToneGenerator? = null

    // 스캔된 텍스트 보관용
    private var currentScannedText: String = ""

    // 카메라 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.layoutPermission.visibility = android.view.View.GONE
            startCamera()
        } else {
            binding.layoutPermission.visibility = android.view.View.VISIBLE
            Toast.makeText(
                this,
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상단 타이틀바 설정 (요구사항: "QR-Reader")
        binding.tvToolbarTitle.text = getString(R.string.title_bar)

        // 비프음 생성기 초기화
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        checkCameraPermissionAndStart()
    }

    private fun setupUI() {
        // 복사하기 버튼 리스너
        binding.btnCopy.setOnClickListener {
            copyToClipboard()
        }

        // 다시 스캔 버튼 리스너
        binding.btnRescan.setOnClickListener {
            resetScanner()
        }

        // 플래시(토치) 토글 버튼
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        // 권한 요청 버튼 (권한 거부 화면 시)
        binding.btnGrantPermission.setOnClickListener {
            checkCameraPermissionAndStart()
        }
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                binding.layoutPermission.visibility = android.view.View.GONE
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 미리보기 (Preview) 설정
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // 바코드 분석기 (ImageAnalysis) 설정
            barcodeAnalyzer = BarcodeAnalyzer { barcode, text ->
                runOnUiThread {
                    onBarcodeScanned(barcode, text)
                }
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, barcodeAnalyzer!!)
                }

            // 후면 카메라 기본 선택
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 이전 바인딩 해제 후 새로 바인딩
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                updateFlashIcon()
            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 바코드 인식 성공 시 호출
     */
    private fun onBarcodeScanned(barcode: Barcode, decodedText: String) {
        currentScannedText = decodedText

        // 1. 비프음 "삐" 소리 재생 (요구사항)
        playBeepSound()

        // 2. 가벼운 햅틱 진동 피드백
        triggerVibration()

        // 3. 바코드 "형식" 정보 변환
        val formatInfo = BarcodeFormatMapper.getFormatInfo(barcode.format)
        val formatDisplayText = "${formatInfo.displayName} [${formatInfo.formatName}]"

        // 4. 하단 표시 항목 업데이트
        binding.tvBarcodeFormat.text = formatDisplayText
        binding.tvBarcodeContent.text = decodedText

        // 시각적 강조 피드백
        binding.viewFinder.animate()
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(120)
            .withEndAction {
                binding.viewFinder.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            }.start()
    }

    /**
     * 비프음 "삐" 소리 출력
     */
    private fun playBeepSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 진동 피드백
     */
    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 클립보드 복사하기 기능 (요구사항)
     */
    private fun copyToClipboard() {
        if (currentScannedText.isBlank()) {
            Toast.makeText(this, getString(R.string.toast_no_content), Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Barcode Text", currentScannedText)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }

    /**
     * 다시 스캔 모드로 전환
     */
    private fun resetScanner() {
        barcodeAnalyzer?.isEnabled = true
        binding.tvBarcodeFormat.text = getString(R.string.placeholder_format)
        binding.tvBarcodeContent.text = getString(R.string.placeholder_content)
        currentScannedText = ""
        Toast.makeText(this, "스캔이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 플래시 토치 토글
     */
    private fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isTorchOn = !isTorchOn
                cam.cameraControl.enableTorch(isTorchOn)
                updateFlashIcon()
            } else {
                Toast.makeText(this, "기기에 플래시가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFlashIcon() {
        val iconRes = if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        binding.btnFlash.setIconResource(iconRes)
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
        cameraExecutor.shutdown()
    }
}
