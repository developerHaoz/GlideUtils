### 前言
> 最近项目在做性能优化，之前项目的图片加载用的是 [Android-Universal-Image-Loader](https://github.com/nostra13/Android-Universal-Image-Loader)，相信大家对于这个老牌的图片加载框架应该都很熟悉。但由于该库的作者已经很久没维护了， 而且 Google 又力推自己员工出品的 **[glide](https://github.com/bumptech/glide)**，在比较了各大图片加载框架的性能之后，决定用 **[glide](https://github.com/bumptech/glide)** 作为新的图片加载框架。

### 本文内容
- 为什么我要进行封装
- 结合 glide 特性的一些思考
- 封装通用的 CommonImageLoader


#### 一、为什么我要进行封装
对于开源项目，有些库的 API 确实设计的相当棒，拿 glide 举个例子：
```
        GlideApp.with(context).load(imageUrl).into(imageView);

```
很多时候只要简单的调用一行代码实现图片的高性能加载（对于 glide 这个框架不熟悉的同学，可以看一下我这篇文章 [glide 一个强大的图片加载框架](http://www.jianshu.com/p/fae51d781987) ），使用起来是不是特别的简单？可能你会有疑问，都已经封装得这么好了，有必要再封装一层么？那你错了，哪怕他已经很完美了，我都会进行相应的封装。

现在技术发展的这么快，如果不进行封装，随着业务的需求，如果发现 glide 这个库已经满足不了我们的需求，而需要换成另外的图片加载库的话，那估计得跪。要把所有调用 glide 的地方全部都修改一遍，累都累死你，但是如果我们封装了一层的话，三天两头改一次都没多大关系。

#### 二、结合 glide 特性的一些思考

既然要对 glide 进行相应的封装，那我们首先就必须要对于这个图片加载库有着足够充分的了解，学习一个热门的框架，我觉得最好的方式就是直接阅读官网的文档或者
 Github 上面的 wiki，写得再好的博客，难免会有所疏漏。而且，随着时间的流逝，这些框架大都会进行一些新特性的添加，以及性能的优化。只有文档才能体现该框架最新的特性，如果想看 glide 的文档的话，可以点击 [这里](https://github.com/bumptech/glide/wiki)

为了更好的封装 glide 这个图片加载库，我也花了相当多的心思，除了把 glide 官网上的 wiki 看完之外，还看了很多有关 glide 写得很不错的博客，以及封装第三方库有关的一些文章，最后才算对 glide 这个库有了更好的把握。

我们要使用一个框架，必然是因为它有着一些非常好的特性，所以我们在封装的时候就必须尽量的保留它的这些特性，不然我们的封装就没有意义了，glide 比较好的特性主要有这几点
- 有着非常简洁的 API
- 处理图片时能保持一个低的内存消耗
- 能够根据 Activity 或 Fragment 的生命周期，对图片就行相应的处理和回收

我们这次的封装的难点就在于第三点，如果只是在 Activity 或 Fragment 中加载的话，那封装很简单啊，直接
```
    public static void displayImage(Context context, String imageUrl, ImageView imageView){
        GlideApp.with(context)
                .load(imageUrl)
                .into(imageView);
    }
```
但是如果你在 Adapter 或者 PopupWindow 这些无法直接获取到 Activity 或 Fragment 的类中想进行图片加载的话，那就不行咯。可能你会说我直接将 Activity 或 Fragment 作为参数直接传进 Adapter 或 PopupWindow 中不就行了。但这样也未免有点太不优雅了，作为一个有追求的程序员，怎么能这么懒呢。

#### 三、封装通用的 CommonImageLoader
上一节中我们谈到了，封装 glide 最大的难点，那我们现在就试着解决这个问题。既然 Adapter 和 PopupWindow 无法直接拿到 Activity 或 Fragment，那我们能不能换种方式来实现呢？当然是可以的。

我们先来看一下封装后的 CommonImageLoader 中的架构
```
public class CommonImageLoader {

    private static LinkedList<Keeper> mKeepers;

    // 创建新的keeper
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

    // 创建新的Keeper
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

    //hashCode 为 iHashCode 的对象需要使用图像加载功能
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

    // hashCode 为 iHashCode 的对象需要使用图像加载功能
    public void iNeedLoadImageFunction(@NonNull Activity activity, int iHashCode) {
        for (Keeper keeper : mKeepers) {
            if (keeper.key == activity.hashCode()) {
                keeper.values.add(iHashCode);
            }
        }

        //  错误抛出，说明activity没有创建对应Keeper
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


    public void displayImage(int hashCode, String uri, ImageView imageView) {
        getGlideRequests(hashCode)
                .load(uri)
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    private static class Keeper {
        int key;
        GlideRequests glideRequests;
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
}
```


可以看到我们首先创建了一个 Keeper 的静态内部类，这个类的作用就是为了解决在 Adapter 或 PupupWindow 这些无法直接获取到 Activity 或 Fragment 的问题。

Keeper 中有两个参数分别 Activity 和 Fragment 的构造函数，将其构建成一个 GlideRequests，GlideRequest 其实就是通过 GlideApp.with() 得到的，所以我们只要得到 GlideRequests，再调用 .load(imageUrl).into(imageView) 就能进行图片的加载了，同时在 Keeper 中用一个 HashSet 保存了 Activity 或 Fragment 的 hashCode。

在 CommonImageLoader 中有一个  **private static LinkedList<Keeper> mKeepers;** 用于保存 Keeper。

可以看到 CommonImageLoader 中有一个 public void addGlideRequests() 的方法，这个方法有两种重载，分别为 public void addGlideRequests(Activity activity) 和 public void addGlideRequests(Fragment fragment) 是用来将 Activity 或 Fragment 的 hashCode 以及对应的 GlideRequests 保存在 Keeper 中。

为了统一方法调用，我们直接将 GlideApp.with(context) 全部改成 GlideRequests，这样的话，我们要进行图片加载时只要在 Activity 或 Fragmet 的 onCreate() 中调用 addGlideRequest，然后在 CommonImageLoader 中我们便可以根据 getGlideRequests() 获取到相应的 GlideRequests，以便于后续的处理。

至于如何处理在 Adapter 或 PopupWindow 中进行图片加载，可以看到在 CommonImageLoader 中有 iNeedLoadImageFuction(@NonNull Fragment fragment, int iHashCode) 以及 iNeedLoadImageFuction(@NonNull Activity activity, int iHashCode) 的重载方法，我们只要在 Activity 或 Fragment 中，将 Fragment 以及 Adapter 的 hashCode 传进去
```
Adapter adapter = new Adapter();
CommonImageLoader.getInstance().iNeedLoadImageFuction(fragment, adapter.hashCode())
```
然后在 Adapter 中就能根据 Adapter 的 hashCode 获取到 GlideRequests，然后进行图片的加载了，当然这些操作已经封装在 CommonImageLoader 里面了， 我们直接调用就好了。


以上便是本文的全部内容，全部的代码我已经放上 Github 了，有兴趣的点击[这里](https://github.com/developerHaoz/GlideUtils)，如果对你有帮助的话，就赏个 star 吧。

### 猜你喜欢
- [Android 一款十分简洁、优雅的日记 APP](http://www.jianshu.com/p/b4fde6b835a3)
- [Android 能让你少走弯路的干货整理](http://www.jianshu.com/p/514656c383a2)
- [Android 撸起袖子，自己封装 DialogFragment](http://www.jianshu.com/p/c9f20ec7277a)
- [手把手教你从零开始做一个好看的 APP](http://www.jianshu.com/p/8d2d74d6046f)
