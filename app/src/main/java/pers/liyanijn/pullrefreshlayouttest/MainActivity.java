package pers.liyanijn.pullrefreshlayouttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import pers.liyanijn.pullrefreshlayout.PullRefreshLayout;

public class MainActivity extends AppCompatActivity {

    List<String> strings = new ArrayList<>();

    TestAdapter adapter;

    RecyclerView recyclerView;

    PullRefreshLayout pullRefreshLayout;

    TextView refreshTv, loadMoreTv;

    View refreshView, loadMoreView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        for (int i = 0; i < 4; i++) {
            strings.add("测试项" + i);
        }

        pullRefreshLayout = (PullRefreshLayout) findViewById(R.id.prl_test);


        refreshView = getLayoutInflater().inflate(R.layout.layout_refresh, pullRefreshLayout, false);
        refreshTv = (TextView) refreshView.findViewById(R.id.tv_refresh);
        pullRefreshLayout.setRefreshView(refreshView);


        loadMoreView = getLayoutInflater().inflate(R.layout.layout_load_more, pullRefreshLayout, false);
        loadMoreTv = (TextView) loadMoreView.findViewById(R.id.tv_load_more);
        pullRefreshLayout.setLoadMoreView(loadMoreView);

        adapter = new TestAdapter();
        recyclerView = (RecyclerView) findViewById(R.id.rcl_test);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        pullRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                loadMoreTv.setText("上拉加载");
                strings = new ArrayList<String>();
                for (int i = 0; i < 4; i++) {
                    strings.add("测试项" + i);
                }

                Observable
                        .just(1)
                        .delay(1, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(@NonNull Integer integer) throws Exception {
                                adapter.notifyDataSetChanged();
                                pullRefreshLayout.setRefreshing(false);
                            }
                        });
            }
        });

        pullRefreshLayout.setOnLoadMoreListener(new PullRefreshLayout.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

                int size = strings.size();
                if (size < 16) {
                    for (int i = size; i < size + 4; i++) {
                        strings.add("测试项" + i);
                    }
                }

                Observable
                        .just(1)
                        .delay(1, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(@NonNull Integer integer) throws Exception {
                                adapter.notifyDataSetChanged();
                                pullRefreshLayout.setLoadingMore(false);
                                if (strings.size() >= 16) {
                                    loadMoreTv.setText("没有更多数据了");
                                }
                            }
                        });

            }
        });

        pullRefreshLayout.setOnRefreshViewShowListener(new PullRefreshLayout.OnRefreshViewShowListener() {
            @Override
            public void onRefreshViewShow(float percent) {
                refreshTv.setText("下拉刷新，露出的高度百分比:" + percent);
            }
        });

        pullRefreshLayout.setOnLoadMoreViewShowListener(new PullRefreshLayout.OnLoadMoreViewShowListener() {
            @Override
            public void onLoadMoreViewShow(float percent) {
                loadMoreView.setAlpha(percent);
            }
        });
    }

    class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_test, parent, false);
            return new TestViewHolder(v);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            holder.testTv.setText(strings.get(position));
        }

        @Override
        public int getItemCount() {
            return strings.size();
        }
    }

    class TestViewHolder extends RecyclerView.ViewHolder {

        TextView testTv;

        public TestViewHolder(View itemView) {
            super(itemView);
            testTv = (TextView) itemView.findViewById(R.id.tv_test);
        }
    }
}
