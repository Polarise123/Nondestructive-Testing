package com.kc.myapplication;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kc.myapplication.ml.Hys1;
import com.kc.myapplication.ml.Tx2;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.manager.PictureCacheManager;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.style.PictureParameterStyle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import io.reactivex.Observer;
//import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
   // private static final String TAG = "OpenCV";
    private final int maxSelectNum = 4;//最多到几张还可以选择
    private final List<LocalMedia> selectList = new ArrayList<>();
    private static final ArrayList<Float> imageArray = new ArrayList<>();
    private GridImageAdapter adapter;
    private RecyclerView mRecyclerView;
    private PopupWindow pop;
    private PictureParameterStyle mPictureParameterStyle;
 //   private List<LocalMedia> list = new ArrayList<>();
    private String path;

    private EditText editText;

    private int whichModel;
    private ByteBuffer inputBuffer;
    private ArrayList<Float> inputArray;
    private String temperature;
    private static float imagfloat;
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler);
        Button buttonmodel = findViewById(R.id.ButtonModel);
        textView = findViewById(R.id.Count);
        RadioGroup buttonGroup = findViewById(R.id.RadioGroup);
        RadioButton RB1 = findViewById(R.id.rb_hfs);
        RadioButton RB2 = findViewById(R.id.rb_tx);
        RadioButton RB3 = findViewById(R.id.rb_hys);
        editText = findViewById(R.id.EditText);
        iniLoadOpenCV();
        initWidget();

        buttonGroup.setOnCheckedChangeListener(ChangeRadioGroup);
        buttonmodel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                temperature =editText.getText().toString();
                if (!temperature.equals("") ) {

                        inputArray = imageArray;
                        if(imageArray.size() == 4) {
                            inputArray.add(Float.parseFloat(temperature));
                            Log.i("inputArray", String.valueOf(inputArray));
                            inputBuffer = getBuffer(inputArray);
                            if (whichModel == 1) {
                                runModelTX(inputBuffer);
                            } else if (whichModel == 2) {
                                runModelHYS(inputBuffer);
                            } else if (whichModel == 3) {
                                runModelHFS(inputBuffer);
                            }

                        }else{
                            //ToastUtil.shotMsg(this, "请获取图片数组");
                            Log.i("inputArray", String.valueOf(inputArray));
                        }

                } else{
                    //ToastUtil.shotMsg(this, "请输入环境温度");
                    Log.i("temperature", temperature);
                }

            }
        });

    }
    private void initWidget() {
        FullyGridLayoutManager manager = new FullyGridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        adapter = new GridImageAdapter(this, onAddPicClickListener);//初始化添加监听
        adapter.setList(selectList);//图（参数）
        adapter.setSelectMax(maxSelectNum);//数量（8）
        mRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((position, v) -> {
            if (selectList.size() > 0) {
                LocalMedia media = selectList.get(position);
                path = media.getRealPath();
                Log.i("IMGpath", path);

                String mimeType = media.getMimeType();
                int mediaType = PictureMimeType.getMimeType(mimeType);
                switch (mediaType) {
                    case PictureConfig.TYPE_VIDEO:
                        // 预览视频
                        PictureSelector.create(MainActivity.this)
                                .themeStyle(R.style.picture_default_style)
                                //.setPictureStyle(mPictureParameterStyle)// 动态自定义相册主题
                                .externalPictureVideo(TextUtils.isEmpty(media.getAndroidQToPath()) ? media.getPath() : media.getAndroidQToPath());
                        break;
                    //case PictureConfig.TYPE_AUDIO:
                        // 预览音频
                        //PictureSelector.create(MainActivity.this)
                                //.externalPictureAudio(PictureMimeType.isContent(media.getPath()) ? media.getAndroidQToPath() : media.getPath());
                        //break;
                    default:
                        // 预览图片 可自定长按保存路径
//                        PictureWindowAnimationStyle animationStyle = new PictureWindowAnimationStyle();
//                        animationStyle.activityPreviewEnterAnimation = R.anim.picture_anim_up_in;
//                        animationStyle.activityPreviewExitAnimation = R.anim.picture_anim_down_out;
                        PictureSelector.create(MainActivity.this)
                                .themeStyle(R.style.picture_default_style) // xml设置主题
                                //.setPictureStyle(mPictureParameterStyle)// 动态自定义相册主题
                                //.setPictureWindowAnimationStyle(animationStyle)// 自定义页面启动动画
                                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)// 设置相册Activity方向，不设置默认使用系统
                                .isNotPreviewDownload(true)// 预览图片长按是否可以下载
                                //.bindCustomPlayVideoCallback(new MyVideoSelectedPlayCallback(getContext()))// 自定义播放回调控制，用户可以使用自己的视频播放界面
                                .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
                                .openExternalPreview(position, selectList);

                        break;
                }
                getImgArray(path);
                conArray(imagfloat);
                Log.i("imagfloat1", "数组获取完成");

            }
        });//item监听
    }

    private final GridImageAdapter.onAddPicClickListener onAddPicClickListener = this::showPop;//添加图片监听

    private void showPop() {
        View bottomView = View.inflate(MainActivity.this, R.layout.layout_bottom_dialog, null);
        TextView mAlbum = bottomView.findViewById(R.id.tv_album);
        TextView mCamera = bottomView.findViewById(R.id.tv_camera);
        TextView mCancel = bottomView.findViewById(R.id.tv_cancel);

        pop = new PopupWindow(bottomView, -1, -2);
        pop.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pop.setOutsideTouchable(true);
        pop.setFocusable(true);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.5f;
        getWindow().setAttributes(lp);
        pop.setOnDismissListener(() -> {
            WindowManager.LayoutParams lp1 = getWindow().getAttributes();
            lp1.alpha = 1f;
            getWindow().setAttributes(lp1);
        });
        pop.setAnimationStyle(R.style.main_menu_photo_anim);
        pop.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);

        @SuppressLint("NonConstantResourceId") View.OnClickListener clickListener = view -> {
            switch (view.getId()) {
                case R.id.tv_album:
                    //相册
                    PictureSelector.create(MainActivity.this)
                            .openGallery(PictureMimeType.ofImage())
                            .imageEngine(GlideEngine.createGlideEngine()) // 请参考Demo GlideEngine.java
                            .maxSelectNum(maxSelectNum)
                            .minSelectNum(1)
                            .imageSpanCount(4)
                            .selectionMode(PictureConfig.MULTIPLE)
                            .forResult(PictureConfig.CHOOSE_REQUEST);
                    break;
                case R.id.tv_camera:
                    //拍照
                    PictureSelector.create(MainActivity.this)
                            .openCamera(PictureMimeType.ofImage())
                            .imageEngine(GlideEngine.createGlideEngine())
                            .forResult(PictureConfig.CHOOSE_REQUEST);
                    break;
                case R.id.tv_cancel:
                    //取消
                    //closePopupWindow();
                    break;
            }
            closePopupWindow();
        };

        mAlbum.setOnClickListener(clickListener);
        mCamera.setOnClickListener(clickListener);
        mCancel.setOnClickListener(clickListener);
    }

    public void closePopupWindow() {
        if (pop != null && pop.isShowing()) {
            pop.dismiss();
            pop = null;
        }
    }
    //
    private final RadioGroup.OnCheckedChangeListener ChangeRadioGroup =new RadioGroup.OnCheckedChangeListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onCheckedChanged (RadioGroup group,int checkId){
            switch (checkId) {
                case R.id.rb_tx:
                    whichModel=1;
                    break;
                case R.id.rb_hys:
                    whichModel=2;
                    break;

                default:
                    whichModel=3;
            }
        }
    };//数组监听

    public float getImgArray(String path) {
        //图像处理
        // Uri uri=stringtoUri(this,path);
        //Mat imgMat = Imgcodecs.imread(getFileAbsolutePath(this,uri));
        Mat imgMat = Imgcodecs.imread(path);
        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(imgMat, imgMat, 15);

        ArrayList<Float> lineArrayb = new ArrayList<>();
        //ArrayList<Float> imagArray = new ArrayList<Float>();

        for (int i = 3948; i < 4048; i=i+10) {
            ArrayList<Double> lineArraya = new ArrayList<>();

            for (int j = 3245; j < 3355; j++) {
                lineArraya.add(imgMat.get(i, j)[0]);
            }
            //float b =getAvgD(lineArraya);
            lineArrayb.add(getAvgD(lineArraya));
        }
        Log.i("imagfloat1", String.valueOf(lineArrayb));
        imagfloat =getAvgF(lineArrayb);
        Log.i("imagfloat2", String.valueOf(imagfloat));
        return imagfloat;
    }//返回图片像素值


    public static ArrayList<Float> conArray(float imagfloat) {

        imageArray.add(imagfloat);
        Log.i("imageArray:", String.valueOf(imageArray));
        return imageArray;
    }

    private static float getAvgF(ArrayList<Float> list) {
        float sum =0;
        for(int i=0;i<list.size();i++) {
            sum +=list.get(i);
        }
        float ArrayAvge =(float) ((int) (sum / list.size()));
        return ArrayAvge;
    }


    private static float getAvgD(ArrayList<Double> list) {
        Double sum= Double.valueOf(0);
        for(int i=0;i<list.size();i++) {
            sum+=list.get(i);
        }
        float ArrayAvge = (float) (sum / list.size());
        return ArrayAvge;
    }


    private static  ByteBuffer getBuffer(ArrayList<Float> inputArray) {
        ByteBuffer inputBuffer;
        inputBuffer = ByteBuffer.allocateDirect(4 * 5);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();
        for (Float element: inputArray) {
            inputBuffer.putFloat(element);
        }
        return inputBuffer;
    }


    @SuppressLint("StringFormatMatches")
    private void runModelHYS(ByteBuffer inputBuffer) {
        try {
            Hys1 model = Hys1.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 5}, DataType.FLOAT32);
            inputFeature0.loadBuffer(inputBuffer);

            // Runs model inference and gets result.
            Hys1.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.i("hys", Arrays.toString(outputFeature0.getFloatArray()));
            textView.setText(Arrays.toString(outputFeature0.getFloatArray()));
            //textView.setText(String.format(getString(R.string.sugar_result), outputFeature0.getFloatArray()));

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @SuppressLint("StringFormatMatches")
    private void runModelTX(ByteBuffer inputBuffer) {
        try {
            Tx2 model = Tx2.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 5}, DataType.FLOAT32);
            inputFeature0.loadBuffer(inputBuffer);

            // Runs model inference and gets result.
            Tx2.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.i("tx", String.valueOf(outputFeature0));
            textView.setText(Arrays.toString(outputFeature0.getFloatArray()));
            //textView.setText(String.format(getString(R.string.sugar_result), outputFeature0));
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }


    @SuppressLint("StringFormatMatches")
    private void runModelHFS(ByteBuffer inputBuffer) {
        try {
            Tx2 model = Tx2.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 5}, DataType.FLOAT32);
            inputFeature0.loadBuffer(inputBuffer);

            // Runs model inference and gets result.
            Tx2.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.i("hfs", String.valueOf(outputFeature0));
            textView.setText(Arrays.toString(outputFeature0.getFloatArray()));
            //textView.setText(String.format(getString(R.string.sugar_result), outputFeature0.getFloatArray()));
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //LocalMedia media;

        List<LocalMedia> images;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    // 图片选择结果回调

                    images = PictureSelector.obtainMultipleResult(data);
                    selectList.addAll(images);
                    //String path = images.getPath();
                    //Log.i("IMGpath", path);
                    //Log.i("data", String.valueOf(data));
                    //String a =String.valueOf(selectList);
                    Log.i("selectList", String.valueOf(selectList));
                    //Log.i("images", String.valueOf(images));
//                    selectList = PictureSelector.obtainMultipleResult(data);
                    // LocalMedia 里面返回三种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true
                    // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true
                    // 如果裁剪并压缩了，以取压缩路径为准，因为是先裁剪后压缩的
                    adapter.setList(selectList);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    }


    /**
     * 根据Uri获取文件绝对路径，解决Android4.4以上版本Uri转换 兼容Android 10
     *
     * @param context
     * @param imageUri
     */
    public static String getFileAbsolutePath(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }

//        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
//            return getRealFilePath(context, imageUri);
//        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            return uriToFileApiQ(context,imageUri);
        }
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(imageUri)) {
                return imageUri.getLastPathSegment();
            }
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    /**
     * Android 10 以上适配 另一种写法
     * @param context
     * @param uri
     * @return
     */
//    private static String getFileFromContentUri(Context context, Uri uri) {
//        if (uri == null) {
//            return null;
//        }
//        String filePath;
//        String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
//        ContentResolver contentResolver = context.getContentResolver();
//        Cursor cursor = contentResolver.query(uri, filePathColumn, null,
//                null, null);
//        if (cursor != null) {
//            cursor.moveToFirst();
//            try {
//                filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
//                return filePath;
//            } catch (Exception e) {
//            } finally {
//                cursor.close();
//            }
//        }
//        return "";
//    }

    /**
     * Android 10 以上适配
     * @param context
     * @param uri
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static String uriToFileApiQ(Context context, Uri uri) {
        File file = null;
        //android10以上转换
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            file = new File(uri.getPath());
        } else if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //把文件复制到沙盒目录
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                try {
                    InputStream is = contentResolver.openInputStream(uri);
                    File cache = new File(context.getExternalCacheDir().getAbsolutePath(), Math.round((Math.random() + 1) * 1000) + displayName);
                    FileOutputStream fos = new FileOutputStream(cache);
                    FileUtils.copy(is, fos);
                    file = cache;
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();
    }

    public static Uri stringtoUri(Context context,String filePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getApplicationInfo().packageName + ".fileprovider", new File(filePath));
        }
        return Uri.fromFile(new File(filePath));
    }







    /**
     * 清空图片缓存，包括裁剪、压缩后的图片，避免OOM
     * 注意:必须要在上传完成后调用 必须要获取权限
     */
    private void clearCache() {
        // 清空图片缓存，包括裁剪、压缩后的图片 注意:必须要在上传完成后调用 必须要获取权限
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //PictureCacheManager.deleteCacheDirFile(this, PictureMimeType.ofImage());
            PictureCacheManager.deleteAllCacheDirRefreshFile(this);
        } else {
            PermissionChecker.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
        }
    }
   // private static void path(int position){
        //LocalMedia media = selectList.get(position);
        //Log.i("原图地址::", media.getPath());
    //}
   //OpenCV库加载并初始化成功后的回调函数
   private void iniLoadOpenCV() {
       boolean success = OpenCVLoader.initDebug();
       if(success) {
           Toast.makeText(this. getApplicationContext(), "Loading OpenCV Libraries...", Toast.LENGTH_LONG) . show();
       } else
           Toast.makeText(this. getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG);
   }

}