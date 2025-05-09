# TrackMate Image and Text Similarity Feature

This feature uses TensorFlow Lite and text-based matching to match lost and found items.

## Setup Instructions

1. Download the MobileNet v3 model:
   - Download the `mobilenet_v3.tflite` model file
   - Place it in the app's `assets` directory at:
     `app/src/main/assets/mobilenet_v3.tflite`

2. The app is configured to use TensorFlow Lite for generating image embeddings and comparing them using cosine similarity.

3. When a user reports a lost or found item, the system will:
   - Generate an embedding for the image if available
   - Compare it against items of the opposite type (lost vs. found)
   - Also match items based on text similarity (name, description, location)
   - Show potential matches to the user with combined similarity scores

## Firebase Optimization

For optimal performance, configure Firebase Database rules with proper indexing. A sample indexing configuration is provided in `app/src/main/assets/firebase_indexing.json`. Apply these rules in your Firebase console to significantly improve query performance.

Key indexed fields:
- `typeString` (for querying lost/found items)
- `userId` (for filtering by user)
- `timestamp` (for sorting and recent items)

## Performance Monitoring

The app includes comprehensive performance logging to identify bottlenecks:
- Image processing timing (preprocessing, inference)
- Firebase operation timing (queries, writes)
- Similarity calculation timing (text, image)

Check the logs with tags: `ItemMatcher`, `ImageSimilaritySearch`, `FirebaseService`, and `PerformanceMonitor`.

## Technical Implementation

The implementation consists of several key components:

1. `ImageSimilaritySearch.java` - Handles TensorFlow Lite model loading and image embedding generation
2. `ImageEmbedding.java` - Model class for storing image embeddings in Firebase
3. `ItemMatcher.java` - Utility for finding matches between lost and found items using both image and text similarity
4. `FirebaseService.java` - Extended with methods to store and retrieve embeddings
5. `ItemDetailActivity.java` - Enhanced to display item similarity information:
   - Owner profile with contact details
   - Similarity score between matched items
   - List of similar items with match percentages
6. `SimilarItemAdapter.java` - Custom adapter for displaying similar items with similarity scores
7. `ReportFragment.java` - Updated with improved image handling and loading indicators

## Enhanced Matching Algorithm

The app now uses a hybrid matching approach:
1. **Image Similarity**: Uses TensorFlow Lite to generate embeddings and cosine similarity for visual matching
2. **Text Similarity**: Matches items based on name, description, and location using fuzzy text matching
3. **Combined Scoring**: Weighted combination of image and text matching for better accuracy
4. **Fallback Mechanism**: If image processing fails, the system can still match based on text features

## User Experience

1. When viewing an item's details, users will see:
   - Basic item information (name, description, location, date)
   - The profile of the user who posted the item
   - A color-coded similarity score (Excellent/Good/Possible)
   - A scrollable list of similar items with match percentages

2. When reporting new items:
   - Improved image preview with proper scaling
   - Progress indicator during processing
   - Automatic matching with existing items

## Troubleshooting

- If you encounter issues with the TensorFlow Lite model, make sure the model file is properly placed in the assets directory.
- Check that the dependencies in the build.gradle file are correctly configured.
- For best results, ensure images are clear and the object is prominently featured in the frame.
- If similarity scores seem incorrect, adjust the threshold values in `ItemMatcher.java`
- For Firebase serialization issues, ensure all enum fields have string backing fields for proper serialization.
