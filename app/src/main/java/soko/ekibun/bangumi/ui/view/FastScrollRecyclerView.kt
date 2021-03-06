package soko.ekibun.bangumi.ui.view

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import com.simplecityapps.recyclerview_fastscroll.R
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import com.simplecityapps.recyclerview_fastscroll.utils.Utils

class FastScrollRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr), RecyclerView.OnItemTouchListener {

    private val mScrollbar: FastScroller

    private var mFastScrollEnabled = true

    private var mDownX: Int = 0
    private var mDownY: Int = 0
    private var mLastY: Int = 0

    private var mStateChangeListener: OnFastScrollStateChangeListener? = null

    val scrollBarWidth: Int
        get() = mScrollbar.width

    val scrollBarThumbHeight: Int
        get() = mScrollbar.mThumbHeight

    /**
     * Returns the available scroll bar height:
     * AvailableScrollBarHeight = Total height of the visible view - thumb height
     */
    private fun getAvailableScrollBarHeight(): Int{
        val visibleHeight = height
        return visibleHeight - mScrollbar.mThumbHeight
    }

    init {

        val typedArray = context.theme.obtainStyledAttributes(
                attrs, R.styleable.FastScrollRecyclerView, 0, 0)
        try {
            mFastScrollEnabled = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollThumbEnabled, true)
        } finally {
            typedArray.recycle()
        }

        mScrollbar = FastScroller(context, this, attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        addOnItemTouchListener(this)
    }

    /**
     * We intercept the touch handling only to support fast scrolling when initiated from the
     * scroll bar.  Otherwise, we fall back to the default RecyclerView touch handling.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, ev: MotionEvent): Boolean {
        return handleTouchEvent(ev)
    }

    override fun onTouchEvent(rv: RecyclerView, ev: MotionEvent) {
        handleTouchEvent(ev)
    }

    /**
     * Handles the touch event and determines whether to show the fast scroller (or updates it if
     * it is already showing).
     */
    private fun handleTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Keep track of the down positions
                mDownX = x
                mLastY = y
                mDownY = mLastY
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener)
            }
            MotionEvent.ACTION_MOVE -> {
                mLastY = y
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener)
        }
        return mScrollbar.isDragging
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }

    override fun draw(c: Canvas) {
        super.draw(c)
        if (mFastScrollEnabled) {
            onUpdateScrollbar()
            mScrollbar.draw(c)
        }
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    fun scrollToPositionAtProgress(touchFraction: Float): String {

        val layoutManager = (layoutManager as? LinearLayoutManager)?:return ""
        val firstIndex= layoutManager.findFirstVisibleItemPosition()
        val lastIndex = layoutManager.findLastVisibleItemPosition()
        //update itemHeight
        if(itemHeightCache.size != layoutManager.itemCount)
            itemHeightCache = IntArray(layoutManager.itemCount){200}
        for(i in firstIndex..lastIndex)
            itemHeightCache[i] = layoutManager.findViewByPosition(i)?.height?:continue
        val totalHeight = itemHeightCache.sum()
        mScrollbar.mThumbHeight = Math.max(layoutManager.height * layoutManager.height / totalHeight, mScrollbar.minThumbHeight)

        val availableScrollHeight = totalHeight - layoutManager.height

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            return ""
        }

        val scrollY = (touchFraction * availableScrollHeight).toInt()
        var totalOffset = 0
        itemHeightCache.forEachIndexed { index, height ->
            if(scrollY >= totalOffset && scrollY<=totalOffset + height){
                layoutManager.scrollToPositionWithOffset(index, totalOffset - scrollY)
                val sectionedAdapter = (adapter as? SectionedAdapter)?:return ""
                return sectionedAdapter.getSectionName(index)
            }
            totalOffset += height
        }
        return ""
    }

    private var itemHeightCache = IntArray(0)
    /**
     * Updates the bounds for the scrollbar.
     */
    private fun onUpdateScrollbar() {
        val layoutManager = (layoutManager as? LinearLayoutManager)?:return
        val firstIndex= layoutManager.findFirstVisibleItemPosition()
        val lastIndex = layoutManager.findLastVisibleItemPosition()
        //update itemHeight
        if(itemHeightCache.size != layoutManager.itemCount)
            itemHeightCache = IntArray(layoutManager.itemCount){200}
        for(i in firstIndex..lastIndex)
            itemHeightCache[i] = layoutManager.findViewByPosition(i)?.height?:continue
        val firstChild = layoutManager.findViewByPosition(firstIndex)?:return
        val topOffset = layoutManager.getDecoratedTop(firstChild)
        val totalHeight = itemHeightCache.sum()
        mScrollbar.mThumbHeight = Math.max(layoutManager.height * layoutManager.height / totalHeight, mScrollbar.minThumbHeight)

        val availableScrollHeight = totalHeight - layoutManager.height
        val scrolledPastHeight: Int = itemHeightCache.sliceArray(0 until firstIndex).sum()

        val availableScrollBarHeight = getAvailableScrollBarHeight()

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbPosition(-1, -1)
            return
        }
        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        val scrollY = scrolledPastHeight - topOffset
        val scrollBarY = (scrollY.toFloat() / availableScrollHeight * availableScrollBarHeight).toInt()

        // Calculate the position and size of the scroll bar
        val scrollBarX: Int = if (Utils.isRtl(resources)) {
            0
        } else {
            width - mScrollbar.width
        }
        mScrollbar.setThumbPosition(scrollBarX, scrollBarY)
    }

    interface SectionedAdapter {
        fun getSectionName(position: Int): String
    }
}
