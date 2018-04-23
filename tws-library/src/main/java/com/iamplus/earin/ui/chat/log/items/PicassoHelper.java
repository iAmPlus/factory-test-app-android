package com.iamplus.earin.ui.chat.log.items;


import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.iamplus.earin.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import java.io.File;

/**
 * Helper class that uses Picasso to load images into an {@link ImageView}.
 */
class PicassoHelper {

    private final static int DEFAULT_IMAGE = R.drawable.ic_image_black_48dp; //TODO: change

    /**
     * Load an image into the provided {@link ImageView}
     *
     * <p>
     *     This method is optimized for loading images into child views of a {@link RecyclerView}.
     *     <br>
     *     To reduce the memory footprint images get resized to the actual size of the provided {@link ImageView}.
     *     The side effect for that is a slightly delayed loading of the image, because we have to wait until
     *     the view is inflated to its actual size.
     *     <br>
     *     After resizing the image, it gets cropped to a square and a radius applied to the corners.
     *     <br>
     *     The consumer of this method will be notified through the passed in callback, as soon as the images
     *     is successfully loaded.
     * </p>
     *
     * @param imageView the {@link ImageView}
     * @param file a {@link File} that represents an image
     * @param callback result {@link Callback.EmptyCallback}
     */
    static void loadImage(@NonNull final ImageView imageView, @NonNull final File file,
                          @NonNull final Callback.EmptyCallback callback) {
        loadImage(imageView, Picasso.with(imageView.getContext()).load(file), callback);
    }

    /**
     * Load an image into the provided {@link ImageView}
     *
     * <p>
     *     This method is optimized for loading images into child views of a {@link RecyclerView}.
     *     <br>
     *     To reduce the memory footprint images get resized to the actual size of the provided {@link ImageView}.
     *     The side effect for that is a slightly delayed loading of the image, because we have to wait until
     *     the view is inflated to its actual size.
     *     <br>
     *     After resizing the image, it gets cropped to a square and a radius applied to the corners.
     *     <br>
     *     The consumer of this method will be notified through the passed in callback, as soon as the images
     *     is successfully loaded.
     * </p>
     *
     * @param imageView the {@link ImageView}
     * @param uri an {@link Uri} to an image
     * @param callback result {@link Callback.EmptyCallback}
     */
    static void loadImage(@NonNull final ImageView imageView, @NonNull final Uri uri,
                          final @NonNull Callback.EmptyCallback callback) {
        loadImage(imageView, Picasso.with(imageView.getContext()).load(uri), callback);
    }


    private static void loadImage(@NonNull final ImageView imageView, @NonNull final RequestCreator requestCreator,
                                  @NonNull final Callback.EmptyCallback callback) {
        imageView.post(new Runnable() {
            @Override
            public void run() {
                final int imageWidth = imageView.getWidth();

                RequestCreator creator = requestCreator
                        .placeholder(DEFAULT_IMAGE)
                        .error(DEFAULT_IMAGE)
                        .transform(new ResizeTransformation(imageWidth));

                if(imageWidth > 0) {
                    creator = creator.resize(imageWidth, 0);
                }

                final int radius = imageView.getContext()
                        .getResources()
                        .getDimensionPixelSize(R.dimen.chat_text_max_width);
                creator = creator.transform(new CropSquareTransform(radius));

                creator.into(imageView, callback);
            }
        });
    }

    public static class ResizeTransformation implements Transformation {

        private final int width;

        ResizeTransformation(int width) {
            this.width = width;
        }

        @Override
        public Bitmap transform(final Bitmap source) {
            if(width <= 0) return source;

            double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
            int targetHeight = (int) (width * aspectRatio);
            Bitmap result = Bitmap.createScaledBitmap(source, width, targetHeight, false);
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override
        public String key() {
            return "resize_transformation_" + width;
        }
    }

    public static class CropSquareTransform implements Transformation {

        private int radius = -1;

        /**
         * Constructs transformation with a clip radius for rounding corners.
         *
         * @param radius to clip corners
         */
        CropSquareTransform(int radius) {
            this.radius = radius;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            Bitmap cropped = Bitmap.createBitmap(source, x, y, size, size);
            // release source bitmap
            if (cropped != source) {
                source.recycle();
            }
            if (radius <= 0) {
                // no rounding needed
                return cropped;
            }

            /**
             * Round the corners of the image using the radius specified in {@link CropSquareTransform}
             */
            // create paint out of the image
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(cropped, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            // create canvas to draw on
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            canvas.drawRoundRect(new RectF(0, 0, size, size), radius, radius, paint);
            // release cropped bitmap
            if (output != cropped) {
                cropped.recycle();
            }

            return output;
        }

        @Override
        public String key() {
            return "crop-square-radius(" + radius + ")";
        }
    }

}
