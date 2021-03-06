package com.codingbingoZTT.BanmaReader.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.avos.avoscloud.LogUtil;
import com.codingbingoZTT.BanmaReader.Constants;
import com.codingbingoZTT.BanmaReader.R;
import com.codingbingoZTT.BanmaReader.base.BaseActivity;
import com.codingbingoZTT.BanmaReader.base.BaseFragment;
import com.codingbingoZTT.BanmaReader.dao.Book;
import com.codingbingoZTT.BanmaReader.dao.BookDao;
import com.codingbingoZTT.BanmaReader.dao.Chapter;
import com.codingbingoZTT.BanmaReader.dao.ChapterDao;
import com.codingbingoZTT.BanmaReader.model.eventbus.BookStatusChangeEvent;
import com.codingbingoZTT.BanmaReader.ui.activity.ReadingActivity;
import com.codingbingoZTT.BanmaReader.ui.listener.OnReadChapterProgressListener;
import com.codingbingoZTT.BanmaReader.view.loadingview.CatLoadingView;
import com.codingbingoZTT.BanmaReader.view.readview.PageWidget;
import com.codingbingoZTT.BanmaReader.view.readview.ReadController;
import com.codingbingoZTT.BanmaReader.view.readview.interfaces.OnControllerStatusChangeListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 *
 *
 * By 2017/3/30.
 */

public class ReadingFragment extends BaseFragment implements OnControllerStatusChangeListener, OnReadChapterProgressListener {
    private ReadController readController;
    private CatLoadingView readLoadingView;
    private PageWidget readPageWidget;



    public PageWidget getReadPageWidget() {
        return readPageWidget;
    }

    public void setReadPageWidget(PageWidget readPageWidget) {
        this.readPageWidget = readPageWidget;
    }


    public CatLoadingView getReadLoadingView() {
        return readLoadingView;
    }

    public void setReadLoadingView(CatLoadingView readLoadingView) {
        this.readLoadingView = readLoadingView;
    }



    private View.OnClickListener onClickListener;

    private long bookId;
    private String bookPath;

    public Book getmBook() {
        return mBook;
    }

    public void setmBook(Book mBook) {
        this.mBook = mBook;
    }

    private Book mBook;
    private List<Chapter> mChapterList;

    @Override
    public String getFragmentName() {
        return ReadingFragment.class.getSimpleName();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reading, container, false);

        initView(view);
        return view;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public void setBookPath(String bookPath) {
        this.bookPath = bookPath;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    private void initView(View view) {
        readController = (ReadController) view.findViewById(R.id.readController);
        readController.setOnControllerStatusChangeListener(this);
        if (onClickListener != null) {
            readController.setOnViewClickListener(onClickListener);
        }

        readPageWidget = (PageWidget) view.findViewById(R.id.readPageWidget);
        //bookId判断书籍状态种类
        if (BaseActivity.NO_BOOK_ID != bookId) {
            readPageWidget.setBookId(bookId, false);
            mBook = getDaoSession().getBookDao().load(bookId);
        } else {
            List<Book> bookList = getDaoSession()
                    .getBookDao()
                    .queryBuilder()
                    .where(BookDao.Properties.BookPath.eq(bookPath))
                    .list();
            if (bookList.size() == 0) {
                readPageWidget.setBookPath(bookPath);
            } else {
                mBook = bookList.get(0);
                bookId = mBook.getId();
                readPageWidget.setBookId(mBook.getId(), false);
            }
        }

        readLoadingView = (CatLoadingView) view.findViewById(R.id.loading);
        if (mBook == null || mBook.getProcessStatus() != Constants.BOOK_PROCESSED) {
            readLoadingView.setVisibility(View.VISIBLE);
            //说明书籍不存在或者还没有处理好
        } else {
            readLoadingView.setVisibility(View.GONE);

            notifyController();
        }
    }

    /**
     * 设置控制页面的状态
     */
    private void notifyController() {
        if (mBook == null) {
            mBook = getDaoSession().getBookDao().load(bookId);
        }

        mChapterList = getDaoSession()
                .getChapterDao()
                .queryBuilder()
                .where(ChapterDao.Properties.BookId.eq(bookId)).list();
        readController.setTotalChaptersNum(mBook.getCurrentChapter(), mChapterList.size() - 1);
        readController.setOnReadChapterProgressListener(this);
    }

    public void nextPage() {
        readPageWidget.nextPage();
    }

    public void prePage() {
        readPageWidget.prePage();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (EventBus.getDefault().isRegistered(this) == false) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (EventBus.getDefault().isRegistered(this) == true) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BookStatusChangeEvent bookStatusChangeEvent) {
        if (bookId != BaseActivity.NO_BOOK_ID && bookId != bookStatusChangeEvent.getBookId()) {
            //不处理
            LogUtil.log.i("bookId不同，不是同一书籍，不更新。");
            return;
        }
        if (bookId == BaseActivity.NO_BOOK_ID){
            Book tempBook = getDaoSession().getBookDao().load(bookStatusChangeEvent.getBookId());
            if (tempBook == null || tempBook.getBookPath().equals(bookPath) == false) {
                LogUtil.log.i("bookPath不同，不是同一书籍，不更新。");
                return;
            }
        }


        switch (bookStatusChangeEvent.getStatus()) {
            case Constants.BOOK_PROCESSED:
                LogUtil.log.e("*****************************");
                LogUtil.log.e("Current bookId is: " + bookId);
                LogUtil.log.e("Current bookPath is: " + bookPath);
                LogUtil.log.e("Pass bookId is: " + bookStatusChangeEvent.getBookId());
                LogUtil.log.e("*****************************");

                readLoadingView.setVisibility(View.GONE);
                bookId = bookStatusChangeEvent.getBookId();
                readPageWidget.setBookId(bookId, true);
                readPageWidget.postInvalidate();
                notifyController();
                break;
            default:
                readLoadingView.setVisibility(View.VISIBLE);

                if (bookId == ReadingActivity.NO_BOOK_ID) {
                    List<Book> bookList = getDaoSession()
                            .getBookDao()
                            .queryBuilder()
                            .where(BookDao.Properties.BookPath.eq(bookPath))
                            .list();
                    if (bookList.size() > 0) {
                        mBook = bookList.get(0);
                        bookId = mBook.getId();
                    }
                }

                if (bookId == bookStatusChangeEvent.getBookId()) {
                    readLoadingView.setLoadingProgress(bookStatusChangeEvent.getProgress());
                }
                break;
        }
    }

    @Override
    public void onControllerStatusChange(boolean isShowing) {
        switchFullScreen(!isShowing);
    }

    @Override
    public void onReadProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mBook.setCurrentChapter(progress);
        Chapter currentChapter = mChapterList.get(progress);
        if (currentChapter != null) {
            mBook.setCurrentPosition(currentChapter.getPosition());
            getDaoSession().getBookDao().update(mBook);

            readPageWidget.setBookId(mBook.getId(), false);
            readPageWidget.postInvalidate();
        } else {
            LogUtil.avlog.e("onReadProgressChanged currentChapter is null, " + "progress -- " + progress);
        }
    }
}
