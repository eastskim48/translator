package kr.ac.korea.translator.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Block;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Page;
import com.google.api.services.vision.v1.model.Paragraph;
import com.google.api.services.vision.v1.model.Symbol;
import com.google.api.services.vision.v1.model.TextAnnotation;
import com.google.api.services.vision.v1.model.Word;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kr.ac.korea.translator.model.Detection;
import kr.ac.korea.translator.model.TextBlock;
import kr.ac.korea.translator.utils.PackageManagerUtils;
import kr.ac.korea.translator.view.main.CoverActivity;

public class OCRApi {
    private static final String TAG = CoverActivity.class.getName();
    private static final String API_KEY = "AIzaSyAookS18qQ5Nz6aG3UR_XZ6YVb6T96RaL0";
    private static final int MAX_LABEL_RESULTS = 50;
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static Context mContext;
    public static List<TextBlock> rstList;

    public static List<TextBlock> callOcr(final Bitmap bitmap, Context context, CoverActivity.TranslateCallback callback) {
        mContext=context;
        rstList = new ArrayList<>();
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LabelDetectionTask(prepareAnnotationRequest(bitmap),callback);
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
        }
        return rstList;
    }

    public static Vision.Images.Annotate prepareAnnotationRequest(final Bitmap bitmap) throws IOException {
        Log.e("S","hi");
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(API_KEY) {
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);
                        String packageName = mContext.getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);
                        String sig = PackageManagerUtils.getSignature(mContext.getPackageManager(), packageName);
                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);
        Vision vision = builder.build();
        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
            // Add the image
            com.google.api.services.vision.v1.model.Image base64EncodedImage = new com.google.api.services.vision.v1.model.Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);
            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature textDetection = new Feature();
                textDetection.setType("DOCUMENT_TEXT_DETECTION");
                textDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(textDetection);
            }});
            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});
        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");
        return annotateRequest;
    }

    public static class LabelDetectionTask extends AsyncTask<Object, Void, String> {
        private Vision.Images.Annotate mRequest;
        private CoverActivity.TranslateCallback mCallback;
        LabelDetectionTask(Vision.Images.Annotate annotate, CoverActivity.TranslateCallback callback) {
            mRequest = annotate;
            mCallback = callback;
        }
        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }
        protected void onPostExecute(String result) {
            Log.e("s",result);
            mCallback.resultToScreen(rstList);
        }
    }

    public static String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("I found these things:\n\n");
        List<AnnotateImageResponse> responses = response.getResponses();
        String blockText;
        String pageText;
        if (responses != null) {
            for (AnnotateImageResponse res : responses) {
                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page : annotation.getPages()) {
                    pageText = "";
                    for (Block block : page.getBlocks()) {
                        blockText = "";
                        for (Paragraph para : block.getParagraphs()) {
                            String paraText = "";
                            for (Word word : para.getWords()) {
                                String wordText = "";
                                for (Symbol symbol : word.getSymbols()) {
                                    wordText = wordText + symbol.getText();
                                }
                                paraText = String.format("%s %s", paraText, wordText);
                            }
                            blockText = blockText + paraText;
                        }
                        String s = translate(blockText);
                        if(s!=null)
                            rstList.add(new TextBlock(block.getBoundingBox(),s));
                        pageText = pageText + blockText;
                    }
                }
            }
        }
        return message.toString();
    }


    public static String translate(String text){
        try{
            String s = TranslateApi.Translate(text);
            Gson gson = new Gson();
            List<Detection> result = gson.fromJson(s, new TypeToken<List<Detection>>() {}.getType());
            if(!result.get(0).getDetectedLanguage().getLanguage().equals("ko"))
                return result.get(0).getTranslations().get(0).getText();
        }
        catch(Exception e){
            Log.e("Exception",e.toString());
        }
        return null;
    }
}
