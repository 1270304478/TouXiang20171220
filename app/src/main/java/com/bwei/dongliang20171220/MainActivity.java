package com.bwei.dongliang20171220;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.HttpRequest;
import okhttp3.internal.http2.Header;

public class MainActivity extends Activity {

    @BindView(R.id.image_view)
    ImageView imageView;
    private static int CAMERA_REQUEST_CODE = 1;//摄像头返回data
    private static int GALLERY_REQUEST_CODE = 2;//图库返回data
    private static int CROP_REQUEST_CODE = 3;//裁剪返回data
    private String url_getHeadImage = "http://www.jcpeixun.com/app_client_api/userinfo.aspx?uid=450894";//获得头像
    private String url_postHeadImage = "http://www.jcpeixun.com/app_client_api/upload_uimg.aspx";//上传头像
    private File tmpDir;//图片文件夹
    private File picture;//图片文件
    private Uri uri_picture;//统一资源标识符
    @BindView(R.id.but_paizhao)
    Button butPaizhao;
    @BindView(R.id.but_xuanqu)
    Button butXuanqu;
    @BindView(R.id.but_quxiao)
    Button butQuxiao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


    }

    @OnClick({R.id.but_paizhao, R.id.but_xuanqu, R.id.but_quxiao})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.but_paizhao:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);//启动摄像头
                break;
            case R.id.but_xuanqu:
                Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);
                intent2.setType("image/*");
                startActivityForResult(intent2, GALLERY_REQUEST_CODE);//启动系统图库
                break;
            case R.id.but_quxiao:
                break;
        }
    }
    //将头像保存到sd卡（返回值是一个file类型的uri）
    private Uri saveBitmap(Bitmap bm) {
        tmpDir = new File(Environment.getExternalStorageDirectory() + "/com.jikexueyuan.avater");
        if (!tmpDir.exists()) {
            tmpDir.mkdir();
        }
        picture = new File(tmpDir.getAbsolutePath() + "avater.png");
        try {
            FileOutputStream fos = new FileOutputStream(picture);
            bm.compress(Bitmap.CompressFormat.PNG, 85, fos);
            fos.flush();
            fos.close();
            return Uri.fromFile(picture);//返回uri
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    //将头像保存到sd卡并将content类型的uri装换成file类型的uri( uri(content) - bitmap - uri(file) )
    private Uri convertUri(Uri uri) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            return saveBitmap(bitmap);//将头像保存到sd卡
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //图片裁剪
    private void startImageZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");//启动裁剪界面
        intent.setDataAndType(uri, "image/*");//传入uri资源，类型为image
        intent.putExtra("crop", "true");//设置为可裁剪
        intent.putExtra("aspectX", 1);//aspect要裁剪的宽高比例（这里为1:1）
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 150);//output最终输出图片的宽和高
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);//设置裁剪之后的数据通过intent返回回来
        startActivityForResult(intent, CROP_REQUEST_CODE);
    }

    //数据返回
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE) {//摄像头
            if (data == null) {
                return;//用户点击取消则直接返回
            } else {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap bm = extras.getParcelable("data");
                    uri_picture = saveBitmap(bm);//将文件保存到sd卡（直接是file类型的bitmap）
                    startImageZoom(uri_picture);
                }
            }
        } else if (requestCode == GALLERY_REQUEST_CODE) {//图库
            if (data == null) {
                return;
            }
            uri_picture = data.getData();
            Uri fileUri = convertUri(uri_picture);//将content类型的uri转换成file类型的uri，，必须是file类型的uri(一般uri分file和content两种类型)
            startImageZoom(fileUri);
        } else if (requestCode == CROP_REQUEST_CODE) {//得到图片裁剪后的数据
            if (data == null) {//用户点击取消则直接返回
                return;
            }
            Bundle extras = data.getExtras();
            if (extras == null) {
                return;
            }
            Bitmap bm = extras.getParcelable("data");

            imageView.setImageBitmap(bm);//将图片显示在界面
            sendImage(bm);//将数据发送到服务器
        }
    }

    //将Bitmap转换成字符串发送到服务器
    private void sendImage(Bitmap bm) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 60, stream);
        byte[] bytes = stream.toByteArray();
        String img = new String(Base64.encodeToString(bytes, Base64.DEFAULT));//接口为base64字符串时调用

        File img2 = new File(getRealPathFromURI(uri_picture.fromFile(picture)));//接口为file时调用

        getVolley(img2);//xUtils方式
        getAsync(img2);//async方式
    }

    private void getAsync(File img2) {

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("uid", "450894");
        try {
            //params.put("uimg", img);//接口为字符串时
            params.put("uimg", img2);//接口为file时
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        client.post(url_postHeadImage, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
            }
            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }
    private void getVolley(File img2) {
        RequestParams params = new RequestParams();
        // 将图片设置到参数中
     /* //  com.lidroid.xutils.http.RequestParams params = new com.lidroid.xutils.http.RequestParams();
        params.addBodyParameter("uid", "450894");
        params.addBodyParameter("uimg", img2);
        // 2、上传文件
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.send(HttpRequest.HttpMethod.POST,
                url_postHeadImage, params,
                new RequestCallBack<String>() {
                    @Override
                    public void onStart() {
                        System.out.println("开始请求");
                    }
                    @Override
                    public void onLoading(long total, long current,
                                          boolean isUploading) {
                        System.out.println("正在加载：共" + total + "个字节，当前："
                                + current);
                    }
                    @Override
                    public void onSuccess(
                            ResponseInfo<String> objectResponseInfo) {
                        System.out.println("上传成功");
                        System.out.println(objectResponseInfo.result);
                        Toast.makeText(MainActivity.this, "上传成功" + "\r\n" + "objectResponseInfo.result", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(com.lidroid.xutils.exception.HttpException e, String s) {
                        System.out.println("上传失败");
                        Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
                    }
                });*/
    }
    //接口为file时调用该方法
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
