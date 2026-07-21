import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

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
    
    // Method 1: Adaptive Threshold
    val blurred1 = Mat()
    Imgproc.medianBlur(gray, blurred1, 5)
    val edged1 = Mat()
    Imgproc.adaptiveThreshold(blurred1, edged1, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)
    Imgcodecs.imwrite("C:/Users/MITS/Desktop/OScan/test-images/output/test4_debug_adaptive.jpg", edged1)
    
    // Method 2: Canny
    val blurred2 = Mat()
    Imgproc.GaussianBlur(gray, blurred2, Size(5.0, 5.0), 0.0)
    val edged2 = Mat()
    Imgproc.Canny(blurred2, edged2, 75.0, 200.0)
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
    val closed2 = Mat()
    Imgproc.morphologyEx(edged2, closed2, Imgproc.MORPH_CLOSE, kernel)
    Imgcodecs.imwrite("C:/Users/MITS/Desktop/OScan/test-images/output/test4_debug_canny.jpg", closed2)
    
    // Method 3: Canny with lower thresholds
    val edged3 = Mat()
    Imgproc.Canny(blurred2, edged3, 30.0, 100.0)
    val closed3 = Mat()
    Imgproc.morphologyEx(edged3, closed3, Imgproc.MORPH_CLOSE, kernel)
    Imgcodecs.imwrite("C:/Users/MITS/Desktop/OScan/test-images/output/test4_debug_canny_low.jpg", closed3)
}
