package com.bdmapdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.DistanceUtil;

/**百度地图
 * @author Eugene
 * @data 2014-12-25
 */
public class BDMap extends Activity implements OnGetGeoCoderResultListener, OnCheckedChangeListener{
//	private static final String TAG = "BDMap";
	
	BDApplication mApplication;//全局应用
	
	MapView mMapView;//百度地图视图控件
	BaiduMap mMapController;//百度地图控制器
	
	LocationClient mLocationClient;//定位服务的客户端
	BDLocation mCurLocation;//当前位置
	
	GeoCoder mGeoCoder = null;//地理编码查询类
	
	EditText etLat, etLng, etCity, etAddr, etRadius;
	RadioGroup radioGroup;
	LinearLayout llGeoCode, llFollowSetting;
	Vibrator mVibrator;//震动组件
	
	LatLng followPoint = null;//待跟踪的点坐标
	int radius = 2000;//检测半径，默认2000米
	boolean isOutBound = false;//是否离开限定区域
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/**全局Application初始化，尽量在setContentView之前初始化 */
		mApplication = BDApplication.GetInstance();
		//设置LoactionHandler
		mApplication.setLoactionHandler(new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == BDApplication.NEW_LOC_MSG){ 
					mCurLocation = (BDLocation) msg.obj;
					locatingOnReceiveLocation();
					drawCircleOnReceiveLocation(2000);//默认2000米
					followPoint = new LatLng(mCurLocation.getLatitude() + 0.005, 
							mCurLocation.getLongitude() + 0.005);//默认模拟点位置
					drawDotOnReceiveLocation(followPoint.latitude, followPoint.longitude);
				}
			}
		});
		
		/**UI初始化*/
		setContentView(R.layout.aty_bdmap);
		llFollowSetting = (LinearLayout) findViewById(R.id.ll_followsetting);
		llGeoCode = (LinearLayout) findViewById(R.id.ll_geocode);
		etLat = (EditText) findViewById(R.id.et_lat);
		etLng = (EditText) findViewById(R.id.et_lng);
		etCity = (EditText) findViewById(R.id.et_city);
		etAddr = (EditText) findViewById(R.id.et_addr);
		etRadius = (EditText) findViewById(R.id.et_radius);
		radioGroup = (RadioGroup) this.findViewById(R.id.radioGroup);
		radioGroup.setOnCheckedChangeListener(this);
		//获取震动服务
		mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
		
        /**地图视图、控制器初始化 */
		mMapView = (MapView) findViewById(R.id.mapView);//百度地图视图控件
		mMapController = mMapView.getMap();//百度地图控制器
		initMapStatus();
		//开启定位图层
		mMapController.setMyLocationEnabled(true);//setMyLocationEnabled设置是否允许定位图层
		
		/**定位初始化 */
		//定位服务的客户端，从BDApplication获取全局LocationClient
		mLocationClient = ((BDApplication)getApplication()).mLocationClient;
		//定义定位参数
		LocationClientOption option = new LocationClientOption();
		initLocationOptions(option);//设置定位参数
		mLocationClient.setLocOption(option);
		//启动定位sdk
		mLocationClient.start();
		
		/**初始化地理编码查询 */
		mGeoCoder = GeoCoder.newInstance();
		mGeoCoder.setOnGetGeoCodeResultListener(this);
	}
	
	//OnGetGeoCoderResultListener地理编码查询结果回调函数
	@Override
	public void onGetGeoCodeResult(GeoCodeResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(getApplicationContext(), "抱歉，未能找到地理编码结果", Toast.LENGTH_LONG).show();
			return;
		}
		
		mMapController.clear();
		mMapController.addOverlay(
			new MarkerOptions()//MarkerOptions标记覆盖物
				.position(result.getLocation())//设置 marker 覆盖物的位置坐标
				//设置 Marker覆盖物的图标，相同图案的 icon的 marker最好使用同一个 BitmapDescriptor对象以节省内存空间。
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding)));
		mMapController.setMapStatus(MapStatusUpdateFactory.newLatLng(result.getLocation()));
		
		Toast.makeText(getApplicationContext(), 
				"纬度：" + result.getLocation().latitude + "；经度：" + result.getLocation().longitude, 
				Toast.LENGTH_LONG).show();
	}
	
	//OnGetGeoCoderResultListener反地理编码查询结果回调函数
	@Override
	public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(getApplicationContext(), "抱歉，未能找到反地理编码结果", Toast.LENGTH_LONG).show();
			return;
		}
		
		mMapController.clear();
		mMapController.addOverlay(
			new MarkerOptions()
				.position(result.getLocation())//设置 marker 覆盖物的位置坐标
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding)));//设置 Marker覆盖物的图标
		mMapController.setMapStatus(MapStatusUpdateFactory.newLatLng(result.getLocation()));
		
		Toast.makeText(getApplicationContext(), result.getAddress(), Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (checkedId == R.id.rbtn_follow) {
			llFollowSetting.setVisibility(View.VISIBLE);
			llGeoCode.setVisibility(View.GONE);
		}
		if (checkedId == R.id.rbtn_geocode) {
			llGeoCode.setVisibility(View.VISIBLE);
			llFollowSetting.setVisibility(View.GONE);
		}
	}
	
	/**按钮单击事件处理
	 * @param v
	 */
	public void onClickProcess(View v) {
		if (v.getId() == R.id.btn_inversegeocode) {
			LatLng ptCenter = new LatLng((Float.valueOf(etLat.getText().toString())), 
					(Float.valueOf(etLng.getText().toString())));
			//ReverseGeoCodeOption反地理编码请求参数
			mGeoCoder.reverseGeoCode(new ReverseGeoCodeOption()
					.location(ptCenter));//设置反地理编码位置坐标
		} else if (v.getId() == R.id.btn_geocode) {
			//GeoCodeOption地理编码请求参数
			mGeoCoder.geocode(new GeoCodeOption()
					.city(etCity.getText().toString())//设置地址
					.address(etAddr.getText().toString()));//设置地址
		} else if (v.getId() == R.id.btn_locate) {
			//请求定位，异步返回，结果在locationListener中获取
			mLocationClient.requestLocation();
		} else if (v.getId() == R.id.btn_changeloc) {
			mMapController.clear();
			//变更模拟点位置，并重新绘制点
			followPoint = new LatLng(followPoint.latitude + 0.005, 
					followPoint.longitude + 0.005);
			drawDotOnReceiveLocation(followPoint.latitude, followPoint.longitude);
			drawCircleOnReceiveLocation(Integer.valueOf(etRadius.getText().toString()));//重新绘制圆
			//距离计算（单位：米），错误时返回-1；DistanceUtil为测距工具
			LatLng curPoint = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());
			double distance = DistanceUtil.getDistance(followPoint, curPoint);//传入参数为百度经纬度坐标
			
			isOutBound = distance > (double)radius;
			if (isOutBound) {//超出限定区域，震动
				mVibrator.vibrate(1000);//1s
			}
			Toast.makeText(getApplicationContext(), "测距结果：" + (int)distance 
					+ "米 \n 是否超出范围：" + isOutBound, 
					Toast.LENGTH_LONG).show();
		} else if (v.getId() == R.id.btn_changeradius) {
			mMapController.clear();
			drawDotOnReceiveLocation(followPoint.latitude, followPoint.longitude);//重新绘制点
			radius = Integer.valueOf(etRadius.getText().toString());
			if(radius > 0){
				//根据新的半径重新绘制圆
				drawCircleOnReceiveLocation(radius);
				//距离计算（单位：米），错误时返回-1；DistanceUtil为测距工具
				LatLng curPoint = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());
				double distance = DistanceUtil.getDistance(followPoint, curPoint);//传入参数为百度经纬度坐标
				
				isOutBound = distance > (double)radius;
				if (isOutBound) {//超出限定区域，震动
					mVibrator.vibrate(1000);//1s
				}
				Toast.makeText(getApplicationContext(), "测距结果：" + (int)distance 
						+ "米 \n 是否超出范围：" + isOutBound, 
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**设置地图状态
	 */
	private void initMapStatus() {
		mMapView.showZoomControls(false);//取消缩放控件
		
		//MapStatusUpdateFactory生成地图状态将要发生的变化
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(14.0f);//设置缩放级别
		//setMapStatus更新地图状态
		mMapController.setMapStatus(msu);
		
		//MapStatus定义地图状态，MapStatus.Builder为地图状态构造器
		MapStatus mapStatus = new MapStatus.Builder(mMapController.getMapStatus()).rotate(0).build();
		MapStatusUpdate u = MapStatusUpdateFactory.newMapStatus(mapStatus);//newMapStatus设置地图新状态
		//animateMapStatus以动画方式更新地图状态，动画耗时 300 ms
		mMapController.animateMapStatus(u);
	}
	
	/**当获取新的位置信息时，进行相关处理
	 */
	private void locatingOnReceiveLocation() {
		mMapController.clear();
		//MyLocationData定位数据，MyLocationData.Builder定位数据建造器
		MyLocationData locData = new MyLocationData.Builder()
			.accuracy(mCurLocation.getRadius())
//			.direction(100)//设置定位数据的方向信息，顺时针0-360
			.latitude(mCurLocation.getLatitude())
			.longitude(mCurLocation.getLongitude()).build();
		mMapController.setMyLocationData(locData);
		//MyLocationConfiguration配置定位图层显示方式
		mMapController.setMyLocationConfigeration(new MyLocationConfiguration(
				MyLocationConfiguration.LocationMode.NORMAL, //定位图层显示方式
				false, //是否允许显示方向信息
				null));//用户自定义定位图标
		//以动画方式更新地图状态到当前位置
		LatLng ll = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());
		MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);//newLatLng设置地图新中心点
		mMapController.animateMapStatus(u);//animateMapStatus以动画方式更新地图状态，动画耗时 300 ms
	}
	
	/**绘制以当前位置为圆心的圆
	 */
	private void drawCircleOnReceiveLocation(int radius) {
		LatLng llCircle = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());
		//CircleOptions创建圆的选项
		OverlayOptions circleOverlayOptions = new CircleOptions()
				.fillColor(0x000000FF)//设置圆填充颜色
				.center(llCircle)//设置圆心坐标
					.stroke(new Stroke(2, 0xFF0000FF))//设置圆边框信息，边框的宽度默认为 5（像素）
				.radius(radius);//设置圆半径（米）
		mMapController.addOverlay(circleOverlayOptions);//向地图添加一个 Overlay
	}
	
	/**绘制模拟的点
	 */
	private void drawDotOnReceiveLocation(double lat, double lng) {
		LatLng llDot = new LatLng(lat, lng);
		OverlayOptions ooDot = new DotOptions()
			.center(llDot)//设置圆点的圆心坐标
			.radius(15)//设置圆点的半径（像素）, 默认为 5px
			.color(0xFF0000FF);//设置圆点的颜色
		mMapController.addOverlay(ooDot);
	}
	
	/**设置定位参数
	 * @param option
	 */
	private void initLocationOptions(LocationClientOption option) {
		option.setOpenGps(true);//打开gps
		option.setCoorType("bd09ll");//设置坐标类型Baidu encoded latitude & longtitude
		option.setScanSpan(1000 * 60 * 5);//扫描间隔5min
		option.setLocationMode(LocationMode.Hight_Accuracy);//GPS + Network locating
		option.setAddrType("all");//locating results include all address infos
		option.setIsNeedAddress(true);//include address infos
	}
	
	@Override  
    protected void onDestroy() {  
		// 退出时停止定位sdk
		mLocationClient.stop();
		// 关闭定位图层
		mMapController.setMyLocationEnabled(false);
		
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();  
    }
	
    @Override  
    protected void onResume() {  
        super.onResume();  
        //在activity执行onResume时执行mMapView.onResume()，实现地图生命周期管理  
        mMapView.onResume();  
    }
    
    @Override  
    protected void onPause() {  
        super.onPause();  
        //在activity执行onPause时执行mMapView.onPause()，实现地图生命周期管理  
        mMapView.onPause();  
    }

}
