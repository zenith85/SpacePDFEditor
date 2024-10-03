package com.example.pefeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.Toast;

import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.graphics.Canvas;
import android.os.Handler;

import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1; // Define a request code
    private static final int PICK_PDF_REQUEST = 2; // Request code for picking PDF

    private DrawingView drawingView;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;
    private int currentPageIndex = 0;

    // HashMap to hold the drawings for each page
    private final HashMap<Integer, Bitmap> drawingsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the PDF directory when the app starts
        createPdfDirectory();

        drawingView = findViewById(R.id.drawingView);
        Button selectPdfButton = findViewById(R.id.selectPdfButton); // Assuming you have this button
        Button prevButton = findViewById(R.id.prevButton);
        Button nextButton = findViewById(R.id.nextButton);
        Button savePdfButton = findViewById(R.id.savePdfButton);

        // Request storage permissions
        requestStoragePermissions();

        selectPdfButton.setOnClickListener(v -> openFilePicker()); // Open the file picker when the button is clicked

        savePdfButton.setOnClickListener(v -> savePdfWithDrawing());
        prevButton.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                saveCurrentDrawing(); // Save the current drawing
                currentPageIndex--;
                showPage(currentPageIndex);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (currentPageIndex < pdfRenderer.getPageCount() - 1) {
                saveCurrentDrawing(); // Save the current drawing
                currentPageIndex++;
                showPage(currentPageIndex);
            }
        });
    }

    private void requestStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    // Load the selected PDF
                    loadPdf(uri);
                }
            }
        }
    }
    private File createPdfDirectory() {
        // Get the app's external files directory
        //File pdfDirectory = new File(getExternalFilesDir(null), "MySpacePDF"); // Create a "MyPdfs" directory
        File pdfDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MyPdfs");

        // Check if the directory exists, if not, create it
        if (!pdfDirectory.exists()) {
            if (pdfDirectory.mkdirs()) {
                Toast.makeText(this, "Directory created: " + pdfDirectory.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
            }
        }
        return pdfDirectory;
    }
    private void loadPdf(Uri uri) {
        try {
            // Use the content resolver to open the PDF file
            fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (fileDescriptor != null) {
                pdfRenderer = new PdfRenderer(fileDescriptor);
                currentPageIndex = 0; // Reset to first page
                showPage(currentPageIndex); // Show the first page
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPage(int index) {
        if (currentPage != null) {
            currentPage.close();
        }

        currentPage = pdfRenderer.openPage(index);
        Bitmap pdfBitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(),
                Bitmap.Config.ARGB_8888);

        currentPage.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Restore the drawing for the current page if it exists
        Bitmap savedDrawing = drawingsMap.get(index);
        if (savedDrawing != null) {
            drawingView.restoreDrawing(savedDrawing);
        } else {
            drawingView.setBitmap(pdfBitmap); // No previous drawing, just set the PDF bitmap
        }
        drawingView.invalidate(); // Force a redraw to ensure the new content is displayed
    }

    private void saveCurrentDrawing() {
        if (drawingView.getBitmap() != null) {
            drawingsMap.put(currentPageIndex, Bitmap.createBitmap(drawingView.getBitmap()));
        }
    }

//    private void savePdfWithDrawing() {
//        if (currentPage != null) {
//            // Create a new PDF document
//            PdfDocument pdfDocument = new PdfDocument();
//            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(currentPage.getWidth(), currentPage.getHeight(), currentPageIndex + 1).create();
//            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
//
//            // Get the canvas from the page
//            Canvas canvas = page.getCanvas();
//
//            // Render the original PDF page into a Bitmap
//            Bitmap pdfBitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
//            currentPage.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
//
//            // Draw the PDF bitmap on the canvas, ensuring it fits the canvas
//            canvas.drawBitmap(pdfBitmap, 0, 0, null);
//
//            // Draw the user's drawings on top of the PDF bitmap
//            Bitmap drawingBitmap = drawingView.getBitmap();
//            if (drawingBitmap != null) {
//                // Scale the drawing bitmap to match the PDF dimensions if necessary
//                Bitmap scaledDrawingBitmap = Bitmap.createScaledBitmap(drawingBitmap, pdfBitmap.getWidth(), pdfBitmap.getHeight(), true);
//                canvas.drawBitmap(scaledDrawingBitmap, 0, 0, null);
//            }
//
//            // Finish the page
//            pdfDocument.finishPage(page);
//
//            // Create a directory for saving PDFs
//            File pdfDirectory = createPdfDirectory(); // Call the method to create the directory
//            String fileName = "Modified_PDF_" + System.currentTimeMillis() + ".pdf"; // Change the filename as needed
//            File file = new File(pdfDirectory, fileName); // Save the file in the created directory
//
//            try (FileOutputStream outputStream = new FileOutputStream(file)) {
//                pdfDocument.writeTo(outputStream);
//                Toast.makeText(this, "PDF saved as " + fileName, Toast.LENGTH_SHORT).show();
//            } catch (IOException e) {
//                e.printStackTrace();
//                Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//
//            // Close the PDF document
//            pdfDocument.close();
//        }
//    }

    private void savePdfWithDrawing() {
        // Invalidate the drawing view to ensure all drawing operations are finalized
        drawingView.invalidate();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (pdfRenderer != null) {
                    // Create a new PDF document
                    PdfDocument pdfDocument = new PdfDocument();

                    try {
                        // Loop through all pages in the PDF
                        for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                            PdfRenderer.Page currentPage = pdfRenderer.openPage(i);

                            // Create a page in the new PDF document with the same dimensions
                            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(currentPage.getWidth(), currentPage.getHeight(), i + 1).create();
                            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                            // Get the canvas from the page
                            Canvas canvas = page.getCanvas();

                            // Render the original PDF page into a Bitmap
                            Bitmap pdfBitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
                            currentPage.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                            // Draw the original PDF page
                            canvas.drawBitmap(pdfBitmap, 0, 0, null);

                            // Draw the user's drawings on top of the PDF bitmap
                            Bitmap drawingBitmap = drawingsMap.get(i); // Retrieve the drawing for the current page
                            if (drawingBitmap != null) {
                                // Scale the drawing bitmap to match the PDF dimensions
                                Bitmap scaledDrawingBitmap = Bitmap.createScaledBitmap(drawingBitmap, pdfBitmap.getWidth(), pdfBitmap.getHeight(), true);
                                canvas.drawBitmap(scaledDrawingBitmap, 0, 0, null);
                            }

                            // Finish the page
                            pdfDocument.finishPage(page);

                            // Close the current page to avoid memory leaks
                            currentPage.close();
                        }

                        // Create a directory for saving PDFs
                        File pdfDirectory = createPdfDirectory(); // Call the method to create the directory
                        String fileName = "Modified_PDF_" + System.currentTimeMillis() + ".pdf"; // Change the filename as needed
                        File file = new File(pdfDirectory, fileName); // Save the file in the created directory

                        // Writing to file might throw IOException
                        try (FileOutputStream outputStream = new FileOutputStream(file)) {
                            pdfDocument.writeTo(outputStream);
                            Toast.makeText(MainActivity .this, "PDF saved as " + fileName, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity .this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace(); // Catch any other unexpected exceptions
                        Toast.makeText(MainActivity .this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } finally {
                        pdfDocument.close(); // Ensure the PDF document is closed
                    }
                } else {
                    Toast.makeText(MainActivity .this, "PDF renderer is not initialized.", Toast.LENGTH_SHORT).show();
                }
            }
        }, 100); // Add a 100ms delay (you can adjust this)
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
