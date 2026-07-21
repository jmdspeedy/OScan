import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

fun main() {
    nu.pattern.OpenCV.loadLocally()
    val files = listOf("test1.jpg", "test4.jpg", "test5.jpg")
    
    for (file in files) {
        val path = "C:/Users/MITS/Desktop/OScan/test-images/$file"
        val mat = Imgcodecs.imread(path)
        if (mat.empty()) {
            println("Failed to load $file")
            continue
        }
        
        val maxDimension = 800.0
        val ratio = maxDimension / Math.max(mat.width(), mat.height())
        val newSize = Size(mat.width() * ratio, mat.height() * ratio)
        val resized = Mat()
        Imgproc.resize(mat, resized, newSize)
        
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
        val hierarchy = Mat()
        Imgproc.findContours(closed, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        contours.sortByDescending { Imgproc.contourArea(it) }
        
        val totalArea = resized.width() * resized.height().toDouble()
        val minArea = totalArea * 0.03
        
        var found = false
        for (contour in contours) {
            val contourArea = Imgproc.contourArea(contour)
            if (contourArea < minArea) continue
            if (contourArea > totalArea * 0.95) continue
            
            // Try Convex Hull
            val hull = MatOfInt()
            Imgproc.convexHull(contour, hull)
            
            val contourArray = contour.toArray()
            val hullPoints = Array(hull.rows()) { Point() }
            val hullIntArray = hull.toArray()
            for (i in hullIntArray.indices) {
                hullPoints[i] = contourArray[hullIntArray[i]]
            }
            val hullMat = MatOfPoint2f(*hullPoints)
            
            val peri = Imgproc.arcLength(hullMat, true)
            var approx = MatOfPoint2f()
            
            for (eps in 1..20) {
                val epsilon = (eps / 100.0) * peri
                Imgproc.approxPolyDP(hullMat, approx, epsilon, true)
                if (approx.total() == 4L) {
                    println("$file: Found 4-point hull at eps $eps% (Area: ${Imgproc.contourArea(approx) / totalArea * 100}%)")
                    found = true
                    break
                }
            }
            if (found) break
        }
        if (!found) {
            println("$file: Could not find 4-point shape even with hull.")
        }
    }
}
