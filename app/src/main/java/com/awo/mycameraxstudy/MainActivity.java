package com.awo.mycameraxstudy;

import static android.view.Surface.ROTATION_90;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.awo.mycameraxstudy.BlazeFace.BlazeFace;
import com.awo.mycameraxstudy.facenet.FaceFeature;
import com.awo.mycameraxstudy.facenet.FaceNetLite;
import com.awo.mycameraxstudy.facenet.Facenet;
import com.awo.mycameraxstudy.mtcnn.Box;
import com.awo.mycameraxstudy.mtcnn.MTCNN;
import com.awo.mycameraxstudy.mtcnn.Utils;
import com.revivers.mtcnn.ARCFACE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    enum DetectModule {
        BLAZEFAZELITE,
        MTCNN,
        NCNNMTCNN
    }

    enum RecognizeModule {
        FACENET,
        FACENETLITE,
        ARCFACE
    }


    private int REQUEST_CODE_PERMISSIONS = 10; //arbitrary number, can be changed accordingly
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"};

    ImageView ttv;// 用以显示预览画面的的View组件

    Button takeBtn;
    ImageView ivTake;

    boolean take = false;

    Bitmap takeBitmap = null;
    FaceFeature takeFeature = null;

    private float[] takeArcFeature = null;

    public MTCNN mtcnn;

    public Facenet facenet;
    public FaceNetLite faceNetLite;

    TextView tvFaceCount;
    TextView tvScore;
    TextView tvDetectCost;
    TextView tvFeatureCost;
    TextView tvCompareCost;

    BoxView boxView;

    boolean isDetecting = false;

    BlazeFace blazeFace;

    DetectModule detectModule = DetectModule.NCNNMTCNN;
    RecognizeModule recognizeModule = RecognizeModule.ARCFACE;

    TextView tvBlaceFace;
    TextView tvMTCNN;
    TextView tvNCNNMTCNN;
    TextView tvFaceNet;
    TextView tvFaceNetLite;

    TextView tvArcFace;

    private com.revivers.mtcnn.MTCNN ncnnMtcnn = new com.revivers.mtcnn.MTCNN();

    private ARCFACE arcface = new ARCFACE();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkSelfPermission();
        // 第一步：设置预览组件的OnLayoutChangeListener（当预览组件的尺寸发生改变时的回调）
        ttv = findViewById(R.id.ttv_camera_preview);
        takeBtn = findViewById(R.id.btn_take);
        ivTake = findViewById(R.id.iv_take);
        tvFaceCount = findViewById(R.id.tv_faceCount);
        tvScore = findViewById(R.id.tv_score);
        tvDetectCost = findViewById(R.id.tv_detectCost);
        tvFeatureCost = findViewById(R.id.tv_featureCost);
        tvCompareCost = findViewById(R.id.tv_compareCost);
        boxView = findViewById(R.id.box);
        tvBlaceFace = findViewById(R.id.tv_blacface);
        tvMTCNN = findViewById(R.id.tv_MTCNN);
        tvNCNNMTCNN = findViewById(R.id.tv_NCNNMTCNN);
        tvFaceNet = findViewById(R.id.tv_facenet);
        tvFaceNetLite = findViewById(R.id.tv_facenetLite);
        tvArcFace = findViewById(R.id.tv_arcface);

        facenet=new Facenet(getAssets());
        mtcnn = new MTCNN(getAssets());
        blazeFace = BlazeFace.create(getAssets());
        faceNetLite = FaceNetLite.create(getAssets());

        try {
            copyBigDataToSD("det1.bin");
            copyBigDataToSD("det2.bin");
            copyBigDataToSD("det3.bin");
            copyBigDataToSD("det1.param");
            copyBigDataToSD("det2.param");
            copyBigDataToSD("det3.param");
            copyBigDataToSD("mobilefacenet.bin");
            copyBigDataToSD("mobilefacenet.param");
            Log.i("Temp tag", "Succeeded to load the weights");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("Temp tag", "Failed to load the weights");
        }
        //模型初始化

        File sdDir = getExternalFilesDir(null);//获取跟目录
        String sdPath = sdDir.toString() + "/mtcnn/";
        Log.i("sdPath",sdPath);
        ncnnMtcnn.FaceDetectionModelInit(sdPath);

        // Arcface model initialization
        if (arcface.FeatureExtractionModelInit(sdPath)) {
            Log.i("Temp tag", "ArcFace model successfully initialized");
        }
        else {
            Log.i("Temp tag", "FAILED TO INITIALIZE THE ARCFACE MODEL!!!!!!!!");
        }

        takeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take = true;
            }
        });

        tvBlaceFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectModule = DetectModule.BLAZEFAZELITE;
                tvBlaceFace.setBackgroundColor(0xff0000ff);
                tvMTCNN.setBackgroundColor(0xffffffff);
                tvNCNNMTCNN.setBackgroundColor(0xffffffff);
            }
        });

        tvMTCNN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectModule = DetectModule.MTCNN;
                tvBlaceFace.setBackgroundColor(0xffffffff);
                tvMTCNN.setBackgroundColor(0xff0000ff);
                tvNCNNMTCNN.setBackgroundColor(0xffffffff);
            }
        });

        tvNCNNMTCNN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectModule = DetectModule.NCNNMTCNN;
                tvBlaceFace.setBackgroundColor(0xffffffff);
                tvMTCNN.setBackgroundColor(0xffffffff);
                tvNCNNMTCNN.setBackgroundColor(0xff0000ff);
            }
        });

        tvFaceNet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeModule = RecognizeModule.FACENET;
                tvFaceNet.setBackgroundColor(0xff0000ff);
                tvFaceNetLite.setBackgroundColor(0xffffffff);
                tvArcFace.setBackgroundColor(0xffffffff);
            }
        });

        tvFaceNetLite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeModule = RecognizeModule.FACENETLITE;
                tvFaceNet.setBackgroundColor(0xffffffff);
                tvFaceNetLite.setBackgroundColor(0xff0000ff);
                tvArcFace.setBackgroundColor(0xffffffff);
            }
        });

        tvArcFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeModule = RecognizeModule.ARCFACE;
                tvFaceNet.setBackgroundColor(0xffffffff);
                tvFaceNetLite.setBackgroundColor(0xffffffff);
                tvArcFace.setBackgroundColor(0xff0000ff);
            }
        });

        ttv.post(new Runnable() {
            @Override
            public void run() {
                startCamera();
            }
        });
    }

    private void copyBigDataToSD(String strOutFileName) throws IOException {
        Log.i("main111", "start copy file " + strOutFileName);
        File sdDir = getExternalFilesDir(null);//获取跟目录
        File file = new File(sdDir.toString()+"/mtcnn/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/mtcnn/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i("main111", "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/mtcnn/"+ strOutFileName);
        myInput = this.getAssets().open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i("main111", "end copy file " + strOutFileName);

    }

    public void checkSelfPermission() {
        String temp = ""; //파일 읽기 권한 확인
        Log.i("main111", "checkSelfPermission 1111");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.READ_EXTERNAL_STORAGE + " ";
            Log.i("main111", "checkSelfPermission 2222");
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.WRITE_EXTERNAL_STORAGE + " "; } if (TextUtils.isEmpty(temp) == false) {
            ActivityCompat.requestPermissions(this, temp.trim().split(" "),1);
            Log.i("main111", "checkSelfPermission 3333");
        } else {
            Log.i("main111", "checkSelfPermission 4444");
        }
    }

    private void startCamera() {
        // 清楚所有绑定
        CameraX.unbindAll();

        /**
         * 预览
         */
        // 计算屏幕参数:宽、高 、屏幕高宽比、尺寸
        int aspRatioW = 640; // 预览View的宽
        int aspRatioH = 480; // 预览View的高
        Rational asp = new Rational (aspRatioW, aspRatioH); // 屏幕高、宽比
        Size screen = new Size(aspRatioW, aspRatioH); // 屏幕尺寸

        // 通过PreviewConfig注入预览设置
        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(asp)
                .setTargetResolution(screen)
                .build();

        // 根据预览配置生成预览对象，并设置预览回调（每一帧画面都调用一次该回调函数）
        Preview preview = new Preview(pConfig);
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                // 需要移除父组件后重新添加View组件，固定写法
//                ViewGroup parent = (ViewGroup) ttv.getParent();
//                parent.removeView(ttv);
//                parent.addView(ttv, 0);
//
//                ttv.setSurfaceTexture(output.getSurfaceTexture());
//
//                updateTransform();
                Log.d("main", "onUpdated ");
            }
        });

        /**
         * 分析
         */
        // 创建Handler用以在子线程处理数据
        HandlerThread handlerThread = new HandlerThread("Image_Analyze");
        handlerThread.start();
        // 创建ImageAnalysisConfig 配置
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetAspectRatio(asp)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(new MyAnalyzer());


        // 将当前Activity和preview 绑定生命周期
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    /**
     * 更新相机预览, 用以保证预览方向正确, 固定写法
     */
    private void updateTransform() {
        Matrix matrix = new Matrix();

//        // Compute the center of the view finder
//        float centerX = ttv.getWidth() / 2f;
//        float centerY = ttv.getHeight() / 2f;
//
//        float[] rotations = {0,90,180,270};
//        // Correct preview output to account for display rotation
//        float rotationDegrees = rotations[ttv.getDisplay().getRotation()];
//
////        matrix.postScale(-1, 1);
//        matrix.postRotate(90, centerX, centerY);
//
//
//        // Finally, apply transformations to our TextureView
//        ttv.setTransform(matrix);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //start camera when permissions have been granted otherwise exit app
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * 检查是否所有所请求的权限都获得许可
     * @return
     */
    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    /**
     * 自定义Analyzer类, 实现ImageAnalysis.Analyzer接口
     * anylyze()是每一帧画面的回调函数
     */
    private class MyAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy image, int rotationDegrees) {
            // 在这里对每一帧画面进行处理
            final Image img = image.getImage();
            if (img != null) {
                long startTime = System.currentTimeMillis();
                Bitmap bitmap = toBitmap(img);
//                Log.d("MainActivity", "Image progress caust: " + (System.currentTimeMillis() - startTime));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ttv.setImageBitmap(bitmap);
                    }
                });

                if (isDetecting) {
//                    Log.d("MainActivity", "isDetecting");
                    return;
                }
                isDetecting = true;
                MuliTaskThread.postToMultiTaskThread(new Runnable() {
                    @Override
                    public void run() {
                        isDetecting = true;
                        if (recognizeModule == RecognizeModule.ARCFACE) {
                            recognizeArcFace(bitmap);
                        } else {
                            long startTime = System.currentTimeMillis();
                            List<Rect> rectList = detectFaces(bitmap);
                            if (rectList == null || rectList.size() == 0) {
                                isDetecting = false;
//                            Log.d("MainActivity", "detect 0");
                                return;
                            }

                            if (take) {
                                List<FaceFeature> features = getFeatures(bitmap, rectList);
                                if (features != null);
                                takeFeature = features.get(0);
                            }
                            long detectCost = System.currentTimeMillis() - startTime;
                            if (takeFeature != null && rectList.size() > 0) {
                                startTime = System.currentTimeMillis();
                                List<FaceFeature> features2 = getFeatures(bitmap, rectList);
                                long featureCost = System.currentTimeMillis() - startTime;
                                if (features2 != null) {
                                    startTime = System.currentTimeMillis();
                                    double score = 0;
                                    if (recognizeModule == RecognizeModule.ARCFACE) {
                                        score = takeFeature.compareArc(features2.get(0));
                                    } else {
                                        score = takeFeature.compare(features2.get(0));
                                    }
                                    long compareCost = System.currentTimeMillis() - startTime;
                                    double finalScore = score;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            boxView.setRects(rectList);
                                            tvFaceCount.setText("人脸：" + rectList.size());
                                            tvScore.setText("scroe = " + finalScore);
                                            tvDetectCost.setText("检测人脸耗时：" + detectCost);
                                            tvFeatureCost.setText("获取feature耗时：" + featureCost);
                                            tvCompareCost.setText("比较耗时："+ compareCost);
                                            if (take) {
                                                takeBitmap = bitmap;
                                                ivTake.setImageBitmap(takeBitmap);
                                                take = false;
                                            }
                                            isDetecting = false;
                                        }
                                    });
                                } else {
                                    isDetecting = false;
                                }
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        boxView.setRects(rectList);
                                        tvFaceCount.setText("人脸：" + rectList.size());
                                        tvDetectCost.setText("检测人脸耗时：" + detectCost);
                                        if (take) {
                                            takeBitmap = bitmap;
                                            ivTake.setImageBitmap(takeBitmap);
                                            take = false;
                                        }
                                        isDetecting = false;
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }
    }

    private List<Rect> detectFaces (Bitmap bitmap) {
        if (detectModule == DetectModule.BLAZEFAZELITE) {
            List<Rect> rectList = rectfToRect(blazeFace.detect(bitmap));
            return rectList;
        }
        if (detectModule == DetectModule.MTCNN) {
            Vector<Box> boxes = mtcnn.detectFaces(bitmap,40);
            List<Rect> rectList = boxToRect(boxes);
            return rectList;
        }
        if (detectModule == DetectModule.NCNNMTCNN) {
            List<Rect> rectList = null;
            int faceInfo[] = ncnnMtcnn.FaceDetect(getPixelsRGBA(bitmap), bitmap.getWidth(), bitmap.getHeight(), 4);
            int faceNum = faceInfo[0];
            for(int i=0;i<faceNum;i++) {
                if (rectList == null) {
                    rectList = new ArrayList<>();
                }
                int left, top, right, bottom;
                left = faceInfo[1+14*i];
                top = faceInfo[2+14*i];
                right = faceInfo[3+14*i];
                bottom = faceInfo[4+14*i];
                rectList.add(new Rect(left, top, right, bottom));
            }
            return rectList;
        }
        return null;
    }

    private List<Rect> boxToRect (Vector<Box> boxes) {
        List<Rect> rects = null;
        if (boxes == null) {
            return rects;
        }
        for (Box box : boxes) {
            if (rects == null) {
                rects = new ArrayList<>();
            }
            rects.add(box.transform2Rect());
        }
        return rects;
    }

    private List<Rect> rectfToRect (List<RectF> rectFS) {
        List<Rect> rects = null;
        for (RectF rectF : rectFS) {
            if (rects == null) {
                rects = new ArrayList<>();
            }
            Rect rect = new Rect((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
            rects.add(rect);
        }
        return rects;
    }

    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

//        旋转90度
        byte[] rotatenv21 = rotateYUV420Degree90(nv21, image.getWidth(), image.getHeight());

        YuvImage yuvImage = new YuvImage(rotatenv21, ImageFormat.NV21, image.getHeight(), image.getWidth(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }

    public List<FaceFeature> getFeatures(Bitmap bitmap, List<Rect> rects) {
        List<Rect> rectList = rects;
        if (rectList == null) {
            rectList = detectFaces(bitmap);
        }
        if (rectList == null || rectList.size() == 0) {
            return null;
        }

        List<FaceFeature> faceFeatures = null;

        for (Rect rect : rectList) {
            //MTCNN检测到的人脸框，再上下左右扩展margin个像素点，再放入facenet中。
            if (recognizeModule == RecognizeModule.FACENET) {
                int margin=20; //20这个值是facenet中设置的。自己应该可以调整。
                Utils.rectExtend(bitmap,rect,margin);
                Bitmap face=Utils.crop(bitmap, rect);
                FaceFeature ff = facenet.recognizeImage(face);
                if (faceFeatures == null) {
                    faceFeatures = new ArrayList<>();
                }
                faceFeatures.add(ff);
            } else if (recognizeModule == RecognizeModule.FACENETLITE) {
                FloatBuffer floatBuffer = faceNetLite.getEmbeddings(bitmap, rect);
                float fea[] = new float[512];
                for (int i = 0; i < 512; i ++) {
                    fea[i] = floatBuffer.get(i);
                }
                FaceFeature ff = new FaceFeature();
                ff.fea = fea;
                if (faceFeatures == null) {
                    faceFeatures = new ArrayList<>();
                }
                faceFeatures.add(ff);
            } else if (recognizeModule == RecognizeModule.ARCFACE) {
//                float[] floats = arcface.getFeature(getPixelsRGBA(bitmap), bitmap.getWidth(), bitmap.getHeight(), 4);
//                FaceFeature ff = new FaceFeature();
//                ff.arcfacefea = floats;
//                if (faceFeatures == null) {
//                    faceFeatures = new ArrayList<>();
//                }
//                faceFeatures.add(ff);
            }
        }
        return  faceFeatures;
    }

    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    private void recognizeArcFace (Bitmap bitmap) {
        long startTime = System.currentTimeMillis();
        List<Rect> rectList = null;
        int faceInfo[] = ncnnMtcnn.FaceDetect(getPixelsRGBA(bitmap), bitmap.getWidth(), bitmap.getHeight(), 4);
        int faceNum = faceInfo[0];
        for(int i=0;i<faceNum;i++) {
            if (rectList == null) {
                rectList = new ArrayList<>();
            }
            int left, top, right, bottom;
            left = faceInfo[1+14*i];
            top = faceInfo[2+14*i];
            right = faceInfo[3+14*i];
            bottom = faceInfo[4+14*i];
            rectList.add(new Rect(left, top, right, bottom));
        }
        int[] currentFaceInfo = new int[14];
        if (rectList == null || rectList.size() == 0) {
            isDetecting = false;
            return;
        }
        for(int i = 0; i < 14; i++) {
            currentFaceInfo[i] = faceInfo[i+1];
        }

        if (take) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            byte[] imageData = getPixelsRGBA(bitmap);
            takeArcFeature = arcface.getFeature(imageData, width, height, 4, currentFaceInfo);
        }
        long detectCost = System.currentTimeMillis() - startTime;
        if (takeArcFeature != null && rectList.size() > 0) {
            startTime = System.currentTimeMillis();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            byte[] imageData = getPixelsRGBA(bitmap);
            float[] features2 = arcface.getFeature(imageData, width, height, 4, currentFaceInfo);
            long featureCost = System.currentTimeMillis() - startTime;
            if (features2 != null) {
                startTime = System.currentTimeMillis();
                double score = 0;
                for (int i = 0; i < features2.length; i++)
                    score += features2[i] * takeArcFeature[i];
                long compareCost = System.currentTimeMillis() - startTime;
                double finalScore = score;
                List<Rect> finalRectList = rectList;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boxView.setRects(finalRectList);
                        tvFaceCount.setText("人脸：" + finalRectList.size());
                        tvScore.setText("scroe = " + finalScore);
                        tvDetectCost.setText("检测人脸耗时：" + detectCost);
                        tvFeatureCost.setText("获取feature耗时：" + featureCost);
                        tvCompareCost.setText("比较耗时："+ compareCost);
                        if (take) {
                            takeBitmap = bitmap;
                            ivTake.setImageBitmap(takeBitmap);
                            take = false;
                        }
                        isDetecting = false;
                    }
                });
            } else {
                isDetecting = false;
            }
        } else {
            List<Rect> finalRectList1 = rectList;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boxView.setRects(finalRectList1);
                    tvFaceCount.setText("人脸：" + finalRectList1.size());
                    tvDetectCost.setText("检测人脸耗时：" + detectCost);
                    if (take) {
                        takeBitmap = bitmap;
                        ivTake.setImageBitmap(takeBitmap);
                        take = false;
                    }
                    isDetecting = false;
                }
            });
        }
    }

}