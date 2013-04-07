package jp.syoboi.android.garaponmate.activity;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SearchView;

import jp.syoboi.android.garaponmate.App;
import jp.syoboi.android.garaponmate.Prefs;
import jp.syoboi.android.garaponmate.R;
import jp.syoboi.android.garaponmate.adapter.MainPagerAdapter;
import jp.syoboi.android.garaponmate.client.GaraponClientUtils;
import jp.syoboi.android.garaponmate.client.SyoboiClientUtils;
import jp.syoboi.android.garaponmate.data.Program;
import jp.syoboi.android.garaponmate.data.SearchParam;
import jp.syoboi.android.garaponmate.fragment.SearchResultFragment;
import jp.syoboi.android.garaponmate.provider.MySearchRecentSuggestionsProvider;
import jp.syoboi.android.garaponmate.service.PlayerService;
import jp.syoboi.android.garaponmate.view.PlayerView;

public class MainActivity extends Activity  {

	static final String TAG = "MainActivity";

	static final long CHANGE_FULLSCREEN_DELAY = 3000;

	static final String SPECIAL_PAGE_PATH = "/garaponMate";

	private static final int PAGE_PAGER = 0;
	private static final int PAGE_SEARCH = 1;

	public static void startActivity(Activity a) {
		Intent i = new Intent(a, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		a.startActivity(i);
	}


	Handler			mHandler = new Handler();
	LinearLayout	mMainContainer;
	PlayerView		mPlayer;
	View			mPlayerKeyGuard;
	View			mContentsContainer;
	View			mPlayerClose;
	View			mSummaryPage;
	View			mSearchPage;
	View			mViewPagerTab;
	ViewPager		mViewPager;
	boolean			mPlayerExpanded;
	boolean			mResumed;
	int				mPage;
	MainPagerAdapter	mPagerAdapter;
	boolean			mShowPlayer;

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (App.forwardLoginActivity(this)) {
			return;
		}
		handleIntent(intent);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (App.forwardLoginActivity(this)) {
			return;
		}

		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			// 起動時はガラポン認証が実行されるようにする
			GaraponClientUtils.setRefreshAuth();
		}

//		mSummaryPage = findViewById(R.id.summaryFragment);
		mSearchPage = findViewById(R.id.searchResultFragment);
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		mViewPager.setOffscreenPageLimit(3);
		mViewPagerTab = mViewPager.findViewById(R.id.viewPagerTab);

		mMainContainer = (LinearLayout) findViewById(R.id.mainContainer);
		mPlayer = (PlayerView) findViewById(R.id.player);
		mPlayerClose = findViewById(R.id.playerClose);
		mContentsContainer = findViewById(R.id.contentsContainer);

		mPagerAdapter = new MainPagerAdapter(
				getFragmentManager(), getApplicationContext());
		mViewPager.setAdapter(mPagerAdapter);

		TabListener tabListener = new TabListener() {

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {

			}

			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				if (tab.getTag() instanceof Integer) {
					int position = (Integer)tab.getTag();
					if (position != mViewPager.getCurrentItem()) {
						mViewPager.setCurrentItem(position);
					}
				}
			}

			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {

			}
		};

		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				Tab tab = getActionBar().getSelectedTab();
				if (tab == null) {
					return;
				}
				Object tag = tab.getTag();
				int currentTab = -1;
				if (tag instanceof Integer) {
					currentTab = (Integer)tag;

				}
				if (currentTab != arg0) {
					getActionBar().selectTab(getActionBar().getTabAt(arg0));
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

		// ActionBar
		ActionBar ab = getActionBar();
//		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);

		for (int j=0; j<mPagerAdapter.getCount(); j++) {
			ActionBar.Tab tab = ab.newTab();
			tab.setTag(j);
			tab.setText(mPagerAdapter.getPageTitle(j));
			tab.setTabListener(tabListener);
			ab.addTab(tab);
		}
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mPlayerClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closePlayer();
			}
		});

		mPlayerKeyGuard = findViewById(R.id.playerKeyGuard);
		mPlayerKeyGuard.setOnTouchListener(new View.OnTouchListener() {
			boolean touching;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!mPlayerExpanded) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						touching = true;
						break;
					case MotionEvent.ACTION_UP:
						if (touching) {
							expandPlayer(true);
						}
						touching = false;
						break;
					case MotionEvent.ACTION_CANCEL:
						touching = false;
						break;
					}
					return true;
				} else {
					return false;
				}
			}
		});

		mMainContainer.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
//				Log.v("", "actionBarHeight:" +  getActionBar().getHeight());
				ActionBar ab = getActionBar();
				View v = mMainContainer;
				int abHeight = ab.isShowing() ? ab.getHeight() : 0;

				if (v.getPaddingTop() != abHeight) {
					mMainContainer.setPadding(v.getPaddingLeft(), abHeight,
							v.getPaddingRight(), v.getPaddingBottom());
				}
			}
		});

		int page = PAGE_PAGER;

		if (savedInstanceState != null) {
			page = savedInstanceState.getInt("page");
		}
		switchPage(page);

		if (savedInstanceState != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt("tab"));
		} else {
			int viewPagerPage = Prefs.getStartPage(0);
			if (0 <= viewPagerPage && viewPagerPage < mPagerAdapter.getCount()) {
				mViewPager.setCurrentItem(viewPagerPage);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
			LayoutTransition lt = new LayoutTransition();
			lt.setDuration(250);
			mMainContainer.setLayoutTransition(lt);
		}

		updateMainContainer();
		handleIntent(getIntent());
	}

	void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			doSearch(intent.getStringExtra(SearchManager.QUERY));
		}
	}

	@Override
	protected void onDestroy() {

		if (mPlayer != null) {
			mPlayer.destroy();
			mPlayer = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPlayer.onPause();

		Prefs.setStartPage(mViewPager.getCurrentItem());
		mResumed = false;
	}

	@Override
	protected void onResume() {
		mResumed = true;
		mPlayer.onResume();
		super.onResume();

		SyoboiClientUtils.syncHistories(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("page", mPage);
		outState.putInt("tab", mViewPager.getCurrentItem());
	}

	/**
	 * 画面の向きが変わったらレイアウトを再設定
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		updateMainContainer();
		updateNavigationMode();
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);

		// SearchView の設定
		SearchView sv = (SearchView) menu.findItem(R.id.search).getActionView();

		// タブレットのときは検索窓がデフォルトで広がった状態にする
		if (getResources().getBoolean(R.bool.tablet)) {
			sv.setIconifiedByDefault(false);
		}

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo si = searchManager.getSearchableInfo(getComponentName());
		sv.setSearchableInfo(si);

		sv.setQueryRefinementEnabled(true);

//		sv.setOnQueryTextListener(new OnQueryTextListener() {
//			@Override
//			public boolean onQueryTextSubmit(String query) {
//				doSearch(query);
//
//				return false;
//			}
//			@Override
//			public boolean onQueryTextChange(String newText) {
//				return false;
//			}
//		});

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.logout);
		if (mi != null) {
			mi.setVisible(Prefs.getLoginHistory().size() > 1);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startSettingsActivity();
			return true;
		case android.R.id.home:
			switchPage(PAGE_PAGER);
			break;
		case R.id.search:
			break;
		case R.id.help:
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(getString(R.string.helpUrl)));
				startActivity(intent);
			}
			break;
		case R.id.icon:
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://crowdworks.jp/public/jobs/7212"));
				startActivity(intent);
			}
			break;
		case R.id.logout:
			App.from(this).logout();
			MainActivity.startActivity(this);
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		// フルスクリーンだったら解除
		if (mPlayer.isFullScreen()) {
			mPlayer.setFullScreen(false);
		}

		// プレイヤーが拡大されていたら縮小
		if (mPlayerExpanded) {
			expandPlayer(false);
			return;
		}

		if (mPage == PAGE_SEARCH) {
			FragmentManager fm = getFragmentManager();
			if (fm.getBackStackEntryCount() > 0) {
				if (fm.getBackStackEntryCount() == 1) {
					switchPage(PAGE_PAGER);
				} else {
				}
				getFragmentManager().popBackStack();
				return;
			}
		}

		if (mPage != PAGE_PAGER) {
			switchPage(PAGE_PAGER);
			return;
		}

		// プレイヤーが表示されていたら閉じる
		if (mShowPlayer) {
			closePlayer();
			return;
		}


//		//ページが戻れる状態だったら戻る
//		if (mWebView.canGoBack()) {
//			mWebView.goBack();
//			return;
//		}
		super.onBackPressed();
	}

	/**
	 * 設定を開く
	 */
	void startSettingsActivity() {
		Intent i = new Intent(MainActivity.this, SettingActivity.class);
		startActivity(i);
	}

	/**
	 * Playerで再生
	 * @param id
	 */
	public void playVideo(Program p, int playerId) {
		switch (playerId) {
		case App.PLAYER_POPUP:
			{
				Intent i = new Intent(this, PlayerService.class);
				i.setAction(PlayerService.ACTION_SET_VIDEO);
				i.putExtra(App.EXTRA_PROGRAM, p);
				startService(i);
			}
			return;
		case App.PLAYER_EXTERNAL:
			playVideoExternal(p);
			return;
		}

		mPlayer.setVideo(p, playerId);

		if (mShowPlayer) {
			expandPlayer(false);
		} else {
			mShowPlayer = true;
			expandPlayer(true);
		}
	}

	public void playVideo(Program p) {
		playVideo(p, Prefs.getPlayerId());
	}

	public void closePlayer() {
		mPlayer.destroy();
		mShowPlayer = false;
		expandPlayer(false);
		updatePlayerContainerSize();
	}


	public void playVideoExternal(Program p) {
		Uri uri = Uri.parse(GaraponClientUtils.getM3uUrl(p.gtvid));
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(uri, "application/vnd.apple.mpegurl");
		startActivity(i);
	}

	/**
	 * Player部分のサイズを変更
	 * @param expand
	 */
	public void expandPlayer(boolean expand) {

		mPlayerExpanded = expand;

		if (expand) {
			getActionBar().hide();
		} else {
			getActionBar().show();
		}

		mPlayer.setAutoFullScreen(expand);

		mPlayerClose.setVisibility(expand ? View.GONE : View.VISIBLE);

		mPlayer.showToolbar(expand);

		updatePlayerContainerSize();
	}

	/**
	 * 画面の向きに合わせてレイアウトを変更する
	 */
	void updateMainContainer() {

		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			mMainContainer.setOrientation(LinearLayout.HORIZONTAL);
			break;
		default:
			mMainContainer.setOrientation(LinearLayout.VERTICAL);
			break;
		}
		updatePlayerContainerSize();
	}

	/**
	 * 分割表示痔のPlayerの幅と高さを設定
	 */
	void updatePlayerContainerSize() {

		boolean landscape = Configuration.ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation;
		LinearLayout.LayoutParams contentslp = (LinearLayout.LayoutParams)mContentsContainer.getLayoutParams();
		LinearLayout.LayoutParams playerLp = (LinearLayout.LayoutParams)mPlayer.getLayoutParams();

		contentslp.width = playerLp.width = landscape ? 0 : LayoutParams.MATCH_PARENT;
		contentslp.height = playerLp.height = landscape ? LayoutParams.MATCH_PARENT : 0;

		mPlayer.setVisibility(mShowPlayer ? View.VISIBLE : View.GONE);
		mContentsContainer.setVisibility(mPlayerExpanded ? View.GONE : View.VISIBLE);
	}




	void switchPage(int page) {
		mPage = page;

		setPageVisibility(mViewPager, page == PAGE_PAGER, 0.5f);
		if (mPage != PAGE_SEARCH) {
			// 検索結果からもとにもどるときは、検索のFragmentをすべて消す
			// UPボタンを無効化
			FragmentManager fm = getFragmentManager();
			while (fm.getBackStackEntryCount() > 0) {
				fm.popBackStackImmediate();
			}
			getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_HOME_AS_UP);
			updateNavigationMode();
		} else {
			// 検索結果が表示されるときはUPボタンを有効にする
			getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
			updateNavigationMode();
		}
	}

	void updateNavigationMode() {
		boolean showActionBarTab = false;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (mPage != PAGE_SEARCH) {
				showActionBarTab = true;
			}
		}
		getActionBar().setNavigationMode(
				showActionBarTab
				? ActionBar.NAVIGATION_MODE_TABS : ActionBar.NAVIGATION_MODE_STANDARD);
		mViewPagerTab.setVisibility(showActionBarTab ? View.GONE : View.VISIBLE);
	}

	static AlphaAnimation sFadeInAnim = new AlphaAnimation(0, 1);
	static AlphaAnimation sFadeOutAnim = new AlphaAnimation(1, 0);

	void setPageVisibility(final View target, boolean show, float zoomFrom) {
		boolean showOld = target.getVisibility() == View.VISIBLE;
		if (showOld != show) {
			AnimationSet set = new AnimationSet(true);
			set.setDuration(300);
			if (show) {
				set.addAnimation(new AlphaAnimation(0, 1));
				set.addAnimation(new ScaleAnimation(
						zoomFrom, 1, zoomFrom, 1,
						Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f));
				target.setVisibility(View.VISIBLE);
				target.startAnimation(set);
			} else {
				set.addAnimation(new AlphaAnimation(1, 0));
				set.addAnimation(new ScaleAnimation(
						1, zoomFrom, 1, zoomFrom,
						Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f));
				target.startAnimation(set);
				target.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * キーワードを指定して検索を実行
	 * @param query
	 */
	private void doSearch(String query) {
		if (TextUtils.isEmpty(query)) {
			return;
		}

		SearchParam sp = new SearchParam();
		if (query.startsWith(App.SEARCH_QUERY_PREFIX_CAPTION)) {
			sp.searchType = SearchParam.STYPE_CAPTION;
			sp.keyword = query.substring(App.SEARCH_QUERY_PREFIX_CAPTION.length()).trim();
		} else {
			sp.keyword = query;
		}
		search(sp);

		SearchRecentSuggestions srs = new SearchRecentSuggestions(
				MainActivity.this,
				MySearchRecentSuggestionsProvider.AUTHORITY,
				MySearchRecentSuggestionsProvider.MODE);
		srs.saveRecentQuery(sp.keyword, null);
	}

	/**
	 * 検索パラメータを指定して検索を実行
	 * @param searchParam
	 */
	public void search(SearchParam searchParam) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

		switchPage(PAGE_SEARCH);

		SearchResultFragment f = SearchResultFragment.newInstance(
				searchParam);

		getFragmentManager().beginTransaction()
		.replace(R.id.searchResultFragment, f)
		.addToBackStack("search")
		.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
		.commit();
	}

	@Override
	public View onCreateView(View parent, String name, Context context,
			AttributeSet attrs) {
//		if ("ListView".equals(name)) {
//			return new MyListView(context, attrs);
//		}
		return super.onCreateView(parent, name, context, attrs);
	}
}
