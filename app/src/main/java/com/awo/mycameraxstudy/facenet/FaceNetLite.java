package com.awo.mycameraxstudy.facenet;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Trace;

import com.awo.mycameraxstudy.BlazeFace.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

public class FaceNetLite  {
    private static final String MODEL_FILE = "facenet.tflite";

    public static final int EMBEDDING_SIZE = 512;

    private static final int INPUT_SIZE_HEIGHT = 160;
    private static final int INPUT_SIZE_WIDTH = 160;

    private static final int BYTE_SIZE_OF_FLOAT = 4;

    // Pre-allocated buffers.
    private int[] intValues;
    private float[] rgbValues;

    private FloatBuffer inputBuffer;
    private FloatBuffer outputBuffer;

    private Bitmap bitmap;

    private Interpreter interpreter;

    /** Memory-map the model file in Assets. */
    private static ByteBuffer loadModelFile(AssetManager assets)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     */
    public static FaceNetLite create(final AssetManager assetManager) {
        final FaceNetLite f = new FaceNetLite();

        try {
            f.interpreter = new Interpreter(loadModelFile(assetManager));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Pre-allocate buffers.
        f.intValues = new int[INPUT_SIZE_HEIGHT * INPUT_SIZE_WIDTH];
        f.rgbValues = new float[INPUT_SIZE_HEIGHT * INPUT_SIZE_WIDTH * 3];
        f.inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE_HEIGHT * INPUT_SIZE_WIDTH * 3 * BYTE_SIZE_OF_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        f.outputBuffer = ByteBuffer.allocateDirect(EMBEDDING_SIZE * BYTE_SIZE_OF_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        f.bitmap = Bitmap.createBitmap(INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT, Bitmap.Config.ARGB_8888);
        return f;
    }

    private FaceNetLite() {}

    public FloatBuffer getEmbeddings(Bitmap originalBitmap, Rect rect) {
        // Log this method so that it can be analyzed with systrace.
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(originalBitmap, rect,
                new Rect(0, 0, INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT), null);

        bitmap.getPixels(intValues, 0, INPUT_SIZE_WIDTH, 0, 0,
                INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT);
//        ImageUtils.saveBitmap(bitmap);

        for (int i = 0; i < intValues.length; ++i) {
            int p = intValues[i];

            rgbValues[i * 3 + 2] = (float) (p & 0xFF);
            rgbValues[i * 3 + 1] = (float) ((p >> 8) & 0xFF);
            rgbValues[i * 3 + 0] = (float) ((p >> 16) & 0xFF);
        }

        ImageUtils.prewhiten(rgbValues, inputBuffer);


        // Run the inference call.

        outputBuffer.rewind();
        interpreter.run(inputBuffer, outputBuffer);
        outputBuffer.flip();

        return outputBuffer;
    }

    public void close() {
        interpreter.close();
    }
}

