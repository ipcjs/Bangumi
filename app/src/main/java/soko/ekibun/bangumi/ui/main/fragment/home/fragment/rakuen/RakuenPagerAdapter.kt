package soko.ekibun.bangumi.ui.main.fragment.home.fragment.rakuen

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
import soko.ekibun.bangumi.api.bangumi.bean.Rakuen
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.topic.TopicActivity

class RakuenPagerAdapter(context: Context, val fragment: RakuenFragment, private val pager: ViewPager, private val scrollTrigger: (Boolean)->Unit) : PagerAdapter(){
    private val tabList = context.resources.getStringArray(R.array.topic_list)

    init{
        pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                loadTopicList(position)
                scrollTrigger((items[position]?.second?.tag as? RecyclerView)?.canScrollVertically(-1) == true)
            } })
    }

    private val items = HashMap<Int, Pair<RakuenAdapter, SwipeRefreshLayout>>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position){
            val swipeRefreshLayout = SwipeRefreshLayout(container.context)
            val recyclerView = RecyclerView(container.context)
            recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    scrollTrigger((items[pager.currentItem]?.second?.tag as? RecyclerView)?.canScrollVertically(-1) == true)
                }
            })

            val adapter = RakuenAdapter()
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setOnItemClickListener { _, v, position ->
                TopicActivity.startActivity(v.context, adapter.data[position].url)
                //WebActivity.launchUrl(v.context, adapter.data[position].url)
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener { loadTopicList(position) }
            Pair(adapter,swipeRefreshLayout)
        }
        container.addView(item.second)
        if((item.second.tag as? RecyclerView)?.tag == null)
            loadTopicList(position)
        return item.second
    }

    private var topicCall = HashMap<Int, Call<List<Rakuen>>>()
    fun loadTopicList(position: Int = pager.currentItem){
        val item = items[position]?:return
        item.first.isUseEmpty(false)
        topicCall[position]?.cancel()
        topicCall[position] = Bangumi.getRakuen(listOf("", "group", "subject", "ep", "mono")[position], (fragment.activity as? MainActivity)?.ua?:"")
        item.second.isRefreshing = true
        topicCall[position]?.enqueue(ApiHelper.buildCallback(item.second.context, {
            item.first.isUseEmpty(true)
            item.first.setNewData(it)
            (item.second.tag as? RecyclerView)?.tag = true
        },{
            item.second.isRefreshing = false
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