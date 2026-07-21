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
    Imgproc.resize(mat, resized, Size(), ratio, ratio, Imgproc.INTER_AREA)
    
    val gray = Mat()
    Imgproc.cvtColor(resized, gray, Imgproc.COLOR_BGR2GRAY)
    val blurred = Mat()
    Imgproc.medianBlur(gray, blurred, 5)
    val edged = Mat()
    Imgproc.adaptiveThreshold(blurred, edged, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)
    
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
    val closed = Mat()
    Imgproc.morphologyEx(edged, closed, Imgproc.MORPH_CLOSE, kernel)
    
    val contours = ArrayList<MatOfPoint>()
    Imgproc.findContours(closed, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
    contours.sortByDescending { Imgproc.contourArea(it) }
    
    val totalArea = resized.width() * resized.height().toDouble()
    
    for (i in 0 until Math.min(5, contours.size)) {
        val contour = contours[i]
        val contourArea = Imgproc.contourArea(contour)
        val rect = Imgproc.boundingRect(contour)
        val boundingArea = rect.width * rect.height.toDouble()
        println("Contour $i: contourArea = ${contourArea / totalArea * 100}%, boundingArea = ${boundingArea / totalArea * 100}%")
    }
}
