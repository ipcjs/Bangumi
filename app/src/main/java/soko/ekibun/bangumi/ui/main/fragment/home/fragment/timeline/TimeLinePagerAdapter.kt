package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.ui.main.MainActivity

class TimeLinePagerAdapter(context: Context, val fragment: TimeLineFragment, private val pager: ViewPager, private val scrollTrigger: (Boolean)->Unit) : PagerAdapter(){
    private val tabList = context.resources.getStringArray(R.array.timeline_list)
    private var topicCall = HashMap<Int, Call<List<TimeLine>>>()
    val pageIndex = HashMap<Int, Int>()

    var selectedType = R.id.timeline_type_friend

    init{
        pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if((items[position]?.second?.tag as? RecyclerView)?.tag == null) {
                    pageIndex[position] = 0
                    loadTopicList(position)
                }
                scrollTrigger((items[position]?.second?.tag as? RecyclerView)?.canScrollVertically(-1) == true)
            } })
    }

    fun reset() {
        items.forEach {  (it.value.second.tag as? RecyclerView)?.tag = null }
        pageIndex.clear()
        loadTopicList()
    }

    private val items = HashMap<Int, Pair<TimeLineAdapter, SwipeRefreshLayout>>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position){
            val swipeRefreshLayout = SwipeRefreshLayout(container.context)
            val recyclerView = RecyclerView(container.context)
            recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    scrollTrigger((items[pager.currentItem]?.second?.tag as? RecyclerView)?.canScrollVertically(-1) == true)
                }
            })

            val adapter = TimeLineAdapter((fragment.activity as? MainActivity)?.ua?:"")
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setEnableLoadMore(true)
            adapter.setOnLoadMoreListener({
                loadTopicList(position)
            }, recyclerView)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener {
                pageIndex[position] = 0
                loadTopicList(position) }
            Pair(adapter,swipeRefreshLayout)
        }
        container.addView(item.second)
        if(pageIndex[position] == 0)
            loadTopicList(position)
        return item.second
    }

    fun loadTopicList(position: Int = pager.currentItem){
        val item = items[position]?:return
        item.first.isUseEmpty(false)
        val page = pageIndex.getOrPut(position) {0}
        if(page == 0) {
            item.second.isRefreshing = true
            item.first.setNewData(null)
        }
        topicCall[position]?.cancel()
        topicCall[position] = Bangumi.getTimeLine(listOf("all", "say", "subject", "progress", "blog", "mono", "relation", "group", "wiki", "index", "doujin")[position], page + 1,
                if(selectedType != R.id.timeline_type_all) item.first.ua else "",  if(selectedType == R.id.timeline_type_self) (fragment.activity as? MainActivity)?.user else null)
        topicCall[position]?.enqueue(ApiHelper.buildCallback(item.second.context, {
            item.first.isUseEmpty(true)
            if(it.isEmpty()) item.first.loadMoreEnd()
            else item.first.loadMoreComplete()
            val list = it.toMutableList()
            if(it.isNotEmpty() && item.first.data.lastOrNull { it.isHeader }?.header == it.getOrNull(0)?.header)
                list.removeAt(0)
            item.first.addData(list)
            (item.second.tag as? RecyclerView)?.tag = true
            pageIndex[position] = (pageIndex[position]?:0) + 1
        },{
            item.second.isRefreshing = false
            item.first.loadMoreFail()
        }))
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return tabList[pos]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return tabList.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}