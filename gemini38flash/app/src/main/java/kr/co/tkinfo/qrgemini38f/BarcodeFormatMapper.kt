package kr.co.tkinfo.qrgemini38f

import com.google.mlkit.vision.barcode.common.Barcode

/**
 * ML Kit 바코드 포맷을 사용자가 이해하기 쉬운 명칭과 카테고리로 변환하는 매퍼
 */
object BarcodeFormatMapper {

    data class FormatInfo(
        val category: String, // "QR", "2D", "1D"
        val formatName: String,
        val displayName: String
    )

    fun getFormatInfo(format: Int): FormatInfo {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> FormatInfo("QR", "QR_CODE", "QR 코드 (2D)")
            
            // 2D 바코드
            Barcode.FORMAT_DATA_MATRIX -> FormatInfo("2D", "DATA_MATRIX", "데이터 매트릭스 (2D)")
            Barcode.FORMAT_AZTEC -> FormatInfo("2D", "AZTEC", "아즈텍 (2D)")
            Barcode.FORMAT_PDF417 -> FormatInfo("2D", "PDF417", "PDF417 (2D)")
            
            // 1D 선형 바코드
            Barcode.FORMAT_CODE_128 -> FormatInfo("1D", "CODE_128", "Code 128 (1D)")
            Barcode.FORMAT_CODE_39 -> FormatInfo("1D", "CODE_39", "Code 39 (1D)")
            Barcode.FORMAT_CODE_93 -> FormatInfo("1D", "CODE_93", "Code 93 (1D)")
            Barcode.FORMAT_CODABAR -> FormatInfo("1D", "CODABAR", "Codabar (1D)")
            Barcode.FORMAT_EAN_13 -> FormatInfo("1D", "EAN_13", "EAN-13 (1D 상품바코드)")
            Barcode.FORMAT_EAN_8 -> FormatInfo("1D", "EAN_8", "EAN-8 (1D)")
            Barcode.FORMAT_ITF -> FormatInfo("1D", "ITF", "ITF (1D)")
            Barcode.FORMAT_UPC_A -> FormatInfo("1D", "UPC_A", "UPC-A (1D)")
            Barcode.FORMAT_UPC_E -> FormatInfo("1D", "UPC_E", "UPC-E (1D)")
            
            else -> FormatInfo("UNKNOWN", "UNKNOWN", "알 수 없는 형식")
        }
    }
}
