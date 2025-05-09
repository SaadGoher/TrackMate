package com.example.trackmate.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class that handles image similarity search using TensorFlow Lite.
 */
public class ImageSimilaritySearch {
    private static final String TAG = "ImageSimilaritySearch";
    private static final String MODEL_FILE = "mobilenet_v3.tflite";
    private static final int IMAGE_SIZE = 224; // MobileNet input size
    
    private Interpreter tflite;
    private static ImageSimilaritySearch instance;
    
    /**
     * Get the singleton instance of ImageSimilaritySearch.
     * 
     * @param context The application context
     * @return The ImageSimilaritySearch instance
     */
    public static synchronized ImageSimilaritySearch getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "🔄 Creating new ImageSimilaritySearch instance");
            instance = new ImageSimilaritySearch(context);
        }
        return instance;
    }
    
    private ImageSimilaritySearch(Context context) {
        try {
            Log.d(TAG, "📲 Loading TFLite model: " + MODEL_FILE);
            long startTime = System.currentTimeMillis();
            
            // Load the TFLite model
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, MODEL_FILE);
            
            // Initialize the TFLite interpreter
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // Set number of threads
            tflite = new Interpreter(tfliteModel, options);
            
            long loadTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "✅ TensorFlow Lite model initialized successfully in " + loadTime + "ms");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error initializing TensorFlow Lite model", e);
        }
    }
    
    /**
     * Generate embedding for an image.
     * 
     * @param image The input bitmap image
     * @return A float array representing the image embedding
     */
    public float[] generateEmbedding(Bitmap image) {
        if (tflite == null) {
            Log.e(TAG, "❌ TensorFlow Lite interpreter is not initialized");
            return null;
        }
        
        try {
            Log.d(TAG, "🖼️ Generating embedding for image: " + image.getWidth() + "x" + image.getHeight());
            long startTime = System.currentTimeMillis();
            
            // Prepare the input image
            long prepStartTime = System.currentTimeMillis();
            TensorImage tensorImage = prepareImage(image);
            long prepTime = System.currentTimeMillis() - prepStartTime;
            Log.d(TAG, "⏱️ Image preparation took: " + prepTime + "ms");
            
            // Create an output tensor
            int[] outputShape = tflite.getOutputTensor(0).shape();
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32);
            
            // Run inference
            long inferenceStartTime = System.currentTimeMillis();
            tflite.run(tensorImage.getBuffer(), outputBuffer.getBuffer());
            long inferenceTime = System.currentTimeMillis() - inferenceStartTime;
            Log.d(TAG, "⏱️ TFLite inference took: " + inferenceTime + "ms");
            
            // Get the embedding
            float[] embedding = outputBuffer.getFloatArray();
            long totalTime = System.currentTimeMillis() - startTime;
            
            Log.d(TAG, "✅ Successfully generated embedding with " + embedding.length + 
                  " features in " + totalTime + "ms");
            
            return embedding;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error generating embedding", e);
            return null;
        }
    }
    
    /**
     * Prepare the input image for the model.
     * 
     * @param bitmap The input bitmap
     * @return Processed TensorImage ready for inference
     */
    private TensorImage prepareImage(Bitmap bitmap) {
        Log.d(TAG, "🔄 Processing image for TFLite model input");
        long startTime = System.currentTimeMillis();
        
        // Create an ImageProcessor with image transformations
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f)) // Normalize to [0, 1]
                .build();
        
        // Create a TensorImage object from the bitmap
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        
        // Process the tensor image
        TensorImage processed = imageProcessor.process(tensorImage);
        
        long totalTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "✅ Image processing completed in " + totalTime + "ms, " + 
              "resized to " + IMAGE_SIZE + "x" + IMAGE_SIZE);
        
        return processed;
    }
    
    /**
     * Result class for storing similarity search results
     */
    public static class SearchResult implements Comparable<SearchResult> {
        private String itemId;
        private float similarityScore;
        
        public SearchResult(String itemId, float similarityScore) {
            this.itemId = itemId;
            this.similarityScore = similarityScore;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public float getSimilarityScore() {
            return similarityScore;
        }
        
        @Override
        public int compareTo(SearchResult other) {
            // Sort in descending order of similarity score
            return Float.compare(other.similarityScore, this.similarityScore);
        }
    }
    
    /**
     * Calculate similarity score between two embeddings using cosine similarity.
     * 
     * @param embedding1 First embedding
     * @param embedding2 Second embedding
     * @return Similarity score (0-1, higher means more similar)
     */
    public static float calculateSimilarity(float[] embedding1, float[] embedding2) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "🧮 Calculating similarity between embeddings...");
        
        if (embedding1 == null || embedding2 == null || 
            embedding1.length != embedding2.length) {
            Log.e(TAG, "❌ Cannot calculate similarity - null or mismatched embeddings");
            return 0f;
        }
        
        Log.d(TAG, "📏 Embedding dimensions: " + embedding1.length);
        
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            normA += embedding1[i] * embedding1[i];
            normB += embedding2[i] * embedding2[i];
        }
        
        float similarity = (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
        long duration = System.currentTimeMillis() - startTime;
        
        Log.d(TAG, "✅ Similarity calculation completed in " + duration + "ms, score: " + similarity);
        return similarity;
    }
    
    /**
     * Release resources when done using the interpreter.
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}