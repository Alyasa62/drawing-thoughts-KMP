package org.example.project.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Component
import javax.swing.SwingUtilities

class DesktopImageSaver : PlatformImageSaver {
    override suspend fun saveImage(bitmap: ImageBitmap): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // We need to run UI related JFileChooser on EDT if possible, or usually it blocks handling thread.
                // However, doing it in IO thread is "fine" for simple apps, but ideally use invokeAndWait for the dialog.
                // But we are in a suspend function.
                
                var selectedFile: File? = null
                
                // Invoke JFileChooser on the Event Dispatch Thread to be safe with Swing
                SwingUtilities.invokeAndWait {
                    val fileChooser = JFileChooser()
                    fileChooser.dialogTitle = "Save Drawing"
                    fileChooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
                    fileChooser.selectedFile = File("drawing_${System.currentTimeMillis()}.png")
                    
                    val userSelection = fileChooser.showSaveDialog(null)
                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        selectedFile = fileChooser.selectedFile
                        if (!selectedFile!!.name.endsWith(".png", ignoreCase = true)) {
                            selectedFile = File(selectedFile!!.absolutePath + ".png")
                        }
                    }
                }

                if (selectedFile != null) {
                    val awtImage = bitmap.toAwtImage()
                    ImageIO.write(awtImage, "png", selectedFile)
                    Result.success("Saved to ${selectedFile?.absolutePath}")
                } else {
                    Result.failure(Exception("Save cancelled"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
