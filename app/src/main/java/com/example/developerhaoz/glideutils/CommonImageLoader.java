package com.example.developerhaoz.glideutils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by developerHaoz on 2017/6/23.
 */

public class CommonImageLoader {

    /**
     * 单例
     */
    private static volatile CommonImageLoader mInstance;
    private static LinkedList<Keeper> mKeepers;


    private CommonImageLoader() {
    }

    public static CommonImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (CommonImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new CommonImageLoader();
                    mKeepers = new LinkedList<>();
                    return mInstance;
                }
            }
        }
        return mInstance;
    }


    /**
     * 创建新的keeper
     *
     * @param fragment
     */
    public void addGlideRequests(@NonNull Fragment fragment) {
        //  避免重复创建
        for (Keeper keeper : mKeepers) {
            if (keeper.key == fragment.hashCode()) {
                return;
            }
        }

        Keeper keeper = new Keeper(fragment);
        mKeepers.add(keeper);
    }

    /**
     * 创建新的Keeper
     *
     * @param activity
     */
    public void addGlideRequests(@NonNull Activity activity) {
        //  避免重复创建
        for (Keeper keeper : mKeepers) {
            if (keeper.key == activity.hashCode()) {
                return;
            }
        }

        Keeper keeper = new Keeper(activity);
        mKeepers.add(keeper);
    }

    /**
     * hashCode 为 iHashCode 的对象需要使用图像加载功能
     *
     * @param fragment  对象所绑定的fragment
     * @param iHashCode 对象的hashCode
     */
    public void iNeedLoadImageFunction(@NonNull Fragment fragment, int iHashCode) {
        //  查找到相应的Keeper，存储对象的hashCode
        for (Keeper keeper : mKeepers) {
            if (keeper.key == fragment.hashCode()) {
                keeper.values.add(iHashCode);
            }
        }

        //  错误抛出，说明fragment没有创建对应Keeper
        throw new IllegalArgumentException();
    }

    /**
     * hashCode 为 iHashCode 的对象需要使用图像加载功能
     *
     * @param activity  对象所绑定的activity
     * @param iHashCode 对象的hashCode
     */
    public void iNeedLoadImageFunction(@NonNull Activity activity, int iHashCode) {
        for (Keeper keeper : mKeepers) {
            if (keeper.key == activity.hashCode()) {
                keeper.values.add(iHashCode);
            }
        }

        //  错误抛出，说明activity没有创建对应Keeper
        throw new IllegalArgumentException();
    }

    /**
     * hashCode为iHashCode的对象需要使用图像加载功能
     * <p>
     * 一般用来嵌套使用
     *
     * @param hashCode  hashCode必须是已经申请过图片加载功能的对象的hashCode
     * @param iHashCode 对象的hashCode
     */
    public void iNeedLoadImageFunction(int hashCode, int iHashCode) {
        for (Keeper keeper : mKeepers) {
            for (Integer i : keeper.values) {
                if (i == hashCode) {
                    keeper.values.add(iHashCode);
                }
            }
        }

        //  错误抛出，说明hashCode对象没有申请图片加载功能
        throw new IllegalArgumentException();
    }

    private GlideRequests getGlideRequests(int hashCode) {
        for (Keeper keeper : mKeepers) {
            if (keeper.values.contains(hashCode)) {
                return keeper.glideRequests;
            }
        }
        return GlideApp.with(MyApplication.getInstance());
    }

    /**
     * 移除 GlideRequests，在 onDestroy() 方法中调用，避免内存泄漏
     *
     * @param fragment
     */
    public void removeGlideRequests(Fragment fragment) {
        for (Keeper keeper : mKeepers) {
            if (keeper.key == fragment.hashCode()) {
                mKeepers.remove(keeper);
            }
        }
    }

    /**
     * 移除 GlideRequests，在 onDestroy() 方法中调用，避免内存泄漏
     *
     * @param activity
     */
    public void removeGlideRequests(Activity activity) {
        for (Keeper keeper : mKeepers) {
            if (keeper.key == activity.hashCode()) {
                mKeepers.remove(keeper);
            }
        }
    }

    /**
     * 根据 hashCode 移除 GlideRequests
     *
     * @param hashCode
     */
    public void removeGlideRequests(int hashCode) {
        for (Keeper keeper : mKeepers) {
            if (keeper.key == hashCode) {
                mKeepers.remove(keeper);
            }
        }
    }

    /**
     * 该方法为普通的图片加载，hashCode 的添加和移除已经在 BaseFragment 以及 BaseActivity 中集成
     * 加载图片直接调用该方法就行
     *
     * @param hashCode
     * @param uri
     * @param imageView
     */
    public void displayImage(int hashCode, String uri, ImageView imageView) {
        getGlideRequests(hashCode)
                .load(uri)
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    /**
     * 在一个imageView里面异步展示一个图片
     *
     * @param uri
     * @param imageView
     */
    public void displayImage(String uri, ImageView imageView) {
        GlideApp.with(imageView.getContext())
                .load(uri)
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // 设置缓存的策略
                .into(imageView);
    }

    public void loadImage(int hashCode, String uri, final getImageListener listener) {
        getGlideRequests(hashCode)
                .asBitmap()
                .load(uri)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        listener.onSuccess(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        listener.onError(errorDrawable);
                    }
                });
    }

    /**
     * 异步加载一个图片，监听加载过程，指定大小，在回调中取得位图
     * 可以用来加载大图。
     *
     * @param context
     * @param uri
     * @param listener
     */
    public void loadImage(Context context, String uri, final getImageListener listener) {

        GlideApp.with(context)
                .asBitmap()
                .load(uri)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        listener.onSuccess(bitmap);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        listener.onError(errorDrawable);
                    }
                });

    }

    /**
     * 获取磁盘缓存
     *
     * @return
     */
    public DiskCache getDiskCache() {
        return CommonImageLoader.getInstance().getDiskCache();
    }

    /**
     * 获取内存缓存
     *
     * @return
     */
    public MemoryCache getMemoryCache() {
        return CommonImageLoader.getInstance().getMemoryCache();
    }

    /**
     * 注意！！！该方法必须在子线程中执行
     * 清除硬盘缓存
     */
    public void cleanDiskCache(final Context context) {
        Glide.get(context).clearDiskCache();
    }

    /**
     * 清除内存缓存
     */
    public void cleanMemoryCache(Context context) {
        Glide.get(context).clearMemory();
    }

    /**
     * 内存和硬盘双清
     */
    public void cleanDoubleCache(Context context) {
        cleanDiskCache(context);
        cleanMemoryCache(context);
    }

    /**
     * 恢复请求，一般在停止滚动的时候调用
     *
     * @param context
     */
    public void resumeRequests(Context context) {
        Glide.with(context).resumeRequests();
    }

    /**
     * 暂停请求，一般在滚动的时候调用
     *
     * @param context
     */
    public void pauseRequests(Context context) {
        Glide.with(context).pauseRequests();
    }

    /**
     * 根据图片的网络地址，拿到使用 Glide 进行缓存后的图片缓存地址
     * 注意！！！ 该方法要在子线程中调用，否则会出错
     *
     * @param imageUrl 图片的网络地址
     * @return 图片的缓存地址
     */
    public static String getImagePathFromCache(String imageUrl, Context context) {

        FutureTarget<File> futureTarget = Glide.with(context)
                .load(imageUrl)
                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
        File cacheFile;
        try {
            cacheFile = futureTarget.get();
            return cacheFile.getAbsolutePath();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 用于监听异步加载图片的过程
     */
    public interface ImageLoadingListener {
        void onSuccess();
        void onError();
    }

    /**
     * 用于以及加载图片获取 Bitmap
     */
    public interface getImageListener {
        void onSuccess(Bitmap bitmap);
        void onError(Drawable drawable);
    }

    private static class Keeper {

        /**
         * {@link Activity}或{@link Fragment}的HashCode
         */
        int key;

        GlideRequests glideRequests;
        /**
         * 需要使用{@link Keeper#glideRequests}加载图片的对象的HashCode
         * 一般是Activity、Fragment、Adapter、PopupWindow这些需要在内部加载图像的对象
         * 对于这些对象唯一的要求是，生命周期与key所对应Activity或Fragment的生命周期“直接绑定”或“间接绑定”
         */
        Set<Integer> values;

        public Keeper(@NonNull Activity activity) {
            key = activity.hashCode();
            values = new HashSet<>();
            values.add(activity.hashCode());
            glideRequests = GlideApp.with(activity);
        }

        public Keeper(@NonNull Fragment fragment) {
            key = fragment.hashCode();
            values = new HashSet<>();
            values.add(fragment.hashCode());
            glideRequests = GlideApp.with(fragment);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Keeper) {
                return key == ((Keeper) obj).key;
            }
            return false;
        }
    }
}
