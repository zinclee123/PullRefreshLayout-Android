# PullRefreshLayout
类似SwipeRefreshLayout,不过还支持上拉加载功能

## 示例(Demo)
<p><img src="https://github.com/zinclee123/PullRefreshLayout/blob/master/img/Demo.gif?raw=true" width="320" alt="Screenshot"/></p>

## 用法(Usage)
### Step 1.
在项目的根build.gradle文件中加入<br/>
```
allprojects {
   repositories {
      ...
      maven { url 'https://jitpack.io' }
   }
}
   ```
### Step 2.
在你需要使用该控件的module的build.gradle中加入依赖<br/>
```
dependencies {
  compile 'com.github.zinclee123:PullRefreshLayout:v0.2'
}
```
### Step 3.
在布局文件中使用</br>
```xml
<pers.zinclee123.pullrefreshlayout.PullRefreshLayout
     android:id="@+id/prl_test"
     android:layout_width="match_parent"
     android:layout_height="match_parent"
     android:background="@color/colorPrimary">

     <android.support.v7.widget.RecyclerView
         android:id="@+id/rcl_test"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:background="@android:color/white"/>

 </pers.zinclee123.pullrefreshlayout.PullRefreshLayout>
```
PullRefreshLayout有两个自定义属性prl_refreshLayout，prl_loadMoreLayout，可指定下拉刷新的视图和上拉加载的视图，在布局文件中使用方式如下</br>
```xml
<pers.zinclee123.pullrefreshlayout.PullRefreshLayout
     android:id="@+id/prl_test"
     android:layout_width="match_parent"
     android:layout_height="match_parent"
     android:background="@color/colorPrimary">

     <android.support.v7.widget.RecyclerView
         android:id="@+id/rcl_test"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         app:prl_refreshLayout="@layout/layout_refresh"
         app:prl_refreshLayout="@layout/layout_load_more"
         android:background="@android:color/white"/>

 </pers.zinclee123.pullrefreshlayout.PullRefreshLayout>
```
也可以通过代码设置如下</br>
```java
pullRefreshLayout.setRefreshView(refreshView);
pullRefreshLayout.setLoadMoreView(loadMoreView);
```
PullRefreshLayout一共可设置四个监听器</br>
#### 1.OnRefreshListener
刷新监听器，通过pullRefreshLayout.setOnRefreshListener方法设置。在准备刷新的时候，调用监听器的onRefresh方法
#### 2.OnLoadMoreListener
刷新监听器，通过pullRefreshLayout.setOnLoadMoreListener方法设置。在准备加载的时候，调用监听器的onLoadMore方法
#### 3.OnRefreshViewShowListener
刷新监听器，通过pullRefreshLayout.setOnRefreshViewShowListener方法设置。在刷新视图位置发生变动时调用，回调监听器的onRefreshViewShow(float percent)这个方法，percent为0.0的一个数，表示 （刷新视图底部到整个视图顶部的距离）/(刷新视图的高度) 的值，可能大于1.0f,这个主要是在下拉过程中让刷新视图做一些动画用的，Demo中也有演示
#### 4.OnLoadMoreViewShowListener
刷新监听器，通过pullRefreshLayout.setOnLoadMoreViewShowListener方法设置。在加载视图位置发生变动时调用，回调监听器的onLoadMoreViewShow(float percent)这个方法，percent为0.0的一个数，表示 （加载视图顶部到整个视图底部的距离）/(加载视图的高度) 的值，可能大于1.0f,这个主要是在上拉过程中让加载视图做一些动画用的

### 注意(Notice)
如果只设置了刷新视图但未设置刷新监听器，下拉的时候不会显示刷新视图。上拉同理



