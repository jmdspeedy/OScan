import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

fun main() {
    nu.pattern.OpenCV.loadLocally()
    val path = "C:/Users/MITS/Desktop/OScan/test-images/test4.jpg"
    val mat = Imgcodecs.imread(path)
    val maxDimension = 800.0
    val ratio = Math.min(maxDimension / mat.width(), maxDimension / mat.height())
    val resized = Mat()
    Imgproc.resize(mat, resized, Size(), ratio, ratio, Imgproc.INTER_LINEAR)
    
    val gray = Mat()
    Imgproc.cvtColor(resized, gray, Imgproc.COLOR_BGR2GRAY)
    
    val blurred = Mat()
    Imgproc.medianBlur(gray, blurred, 5)
    val edged = Mat()
    Imgproc.adaptiveThreshold(blurred, edged, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)
    
    // Test MORPH_OPEN
    val kernel3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    val opened3 = Mat()
    Imgproc.morphologyEx(edged, opened3, Imgproc.MORPH_OPEN, kernel3)
    Imgcodecs.imwrite("C:/Users/MITS/Desktop/OScan/test-images/output/test4_debug_open3.jpg", opened3)
    
    val kernel5 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
    val opened5 = Mat()
    Imgproc.morphologyEx(edged, opened5, Imgproc.MORPH_OPEN, kernel5)
    Imgcodecs.imwrite("C:/Users/MITS/Desktop/OScan/test-images/output/test4_debug_open5.jpg", opened5)
    
    // Also try opening then closing
    val opened_closed = Mat()
    Imgproc.morphologyEx(opened3, opened_closed, Imgproc.MORPH_CLOSE, kernel5)
    Imgcodecs.imwrite("C:/Users/MITS/Desktop/OScan/test-images/output/test4_debug_open3_close5.jpg", opened_closed)
}
