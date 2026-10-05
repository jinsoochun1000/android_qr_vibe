package kr.co.tkinfo.qrgemini38f

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.nio.charset.StandardCharsets

/**
 * CameraX ImageAnalysis용 바코드 분석기.
 * QR 코드, 2D 바코드, 1D 바코드를 모두 스캔하고 콜백으로 전달합니다.
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (barcode: Barcode, decodedText: String) -> Unit
) : ImageAnalysis.Analyzer {

    // 모든 바코드 포맷 지원 (QR, 2D: Aztec/DataMatrix/PDF417, 1D: Code 128/EAN/UPC 등)
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @Volatile
    var isEnabled: Boolean = true

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (!isEnabled || mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (isEnabled && barcodes.isNotEmpty()) {
                    val primaryBarcode = barcodes[0]
                    
                    // UTF-8 한글 텍스트 처리: rawBytes 가 있다면 UTF-8로 우선 복원, 없으면 rawValue 사용
                    val decodedText = primaryBarcode.rawBytes?.let { bytes ->
                        try {
                            String(bytes, StandardCharsets.UTF_8)
                        } catch (e: Exception) {
                            primaryBarcode.rawValue ?: ""
                        }
                    } ?: (primaryBarcode.rawValue ?: primaryBarcode.displayValue ?: "")

                    if (decodedText.isNotBlank()) {
                        isEnabled = false // 성공 시 연속 인식 일시 정지 (사용자가 재스캔할 때까지)
                        onBarcodeDetected(primaryBarcode, decodedText)
                    }
                }
            }
            .addOnFailureListener {
                // 프레임 분석 에러 무시
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
