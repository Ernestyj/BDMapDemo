package com.bdmapdemo;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.SupportMapFragment;

/**百度地图MapFragment实现方式
 * 注意：Fragment方式并不稳定，不推荐使用；supportMapFragment.getMapView();常引起获取MapView失败
 * @author Eugene
 * @data 2014-12-25
 */
public class MapFragment extends FragmentActivity {
//	private static final String TAG = "MapFragment";
	
	//SupportMapFragment管理地图生命周期
	SupportMapFragment supportMapFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aty_fragment);
		
		/**地图Fragment初始化 */
		//MapStatus定义地图状态，MapStatus.Builder为地图状态构造器
		MapStatus mapStatus = new MapStatus.Builder().overlook(-20).zoom(15).build();//overlook地图俯仰角度，zoom地图缩放级别 3~19
		//BaiduMapOptions为MapView初始化选项
		BaiduMapOptions baiduMapOptions = new BaiduMapOptions().mapStatus(mapStatus)
				.compassEnabled(false).zoomControlsEnabled(false);//compassEnabled设置是否允许指南针，默认允许；zoomControlsEnabled设置是否显示缩放控件
		
		/**Fragment初始化*/
		//newInstance创建一个MapFragment实例
		supportMapFragment = SupportMapFragment.newInstance(baiduMapOptions);
		//要管理fragment，需使用FragmentManager
		FragmentManager manager = getSupportFragmentManager();
		//一个事务是在同一时刻执行的一组动作（很像数据库中的事务），可以用add(),remove(),replace()等方法构成事务，最后使用commit()方法提交事务
		//add (int containerViewId, Fragment fragment, String tag)Add a fragment to the activity state.
		//tag: Optional tag name for the fragment, to later retrieve the fragment with FragmentManager.findFragmentByTag(String).
		manager.beginTransaction().add(R.id.map, supportMapFragment, "map_fragment").commit();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
