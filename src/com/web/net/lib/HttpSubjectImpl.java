package com.web.net.lib;

import java.util.Map;
import java.util.Set;

import org.apache.http.Header;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/***
 * 真实的http请求
 * @author 罗政 上午11:34:06
 */
public class HttpSubjectImpl implements HttpSubject{
	final String TAG = HttpSubjectImpl.class.getName() ;
	AsyncHttpClient client = new AsyncHttpClient();  

	@Override
	public void request(Request request) {
		if(request == null){
			return ;
		}
		if(!HttpEngine.getEngine().isCallInit()){
			throw new RuntimeException("please call HttpEngine init method") ;
		}
		if(Type.POST == request.getCurrentRequestType()){
			post(request);
		}else if(Type.GET == request.getCurrentRequestType()){
			get(request);
		}
	}
	private void get(final Request request) {
		/**利用 asynchttp请求数据*/
		if(null == client){
			client = new AsyncHttpClient();
		}
		client.setTimeout(HttpEngine.getEngine().getTimeout());
		client.get(request.getUrl(), new AsyncHttpResponseHandler() {
		    @Override
		    public void onStart() {
		    	Log.d(TAG, "http get is start") ;
		    }
		    @Override
		    public void onSuccess(int statusCode, Header[] headers, byte[] r) {
		    	callonSuccess(statusCode, headers, r, request);
		    }
		    @Override
		    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
		    	callonFailure(statusCode, headers, errorResponse, e, request);
		    }
		});
	}
	private void post(final Request request) {
		if(null == client){
			client = new AsyncHttpClient();
		}
		client.setTimeout(HttpEngine.getEngine().getTimeout());
		Map<String ,String> ps = request.getRequestParms() ;
		if(ps == null){
			throw new NullPointerException("post params is null") ;
		}
		RequestParams params = new RequestParams() ;
		Set<String> keys = ps.keySet() ;
		for(String key : keys){
			params.put(key, ps.get(key)) ;
		}
		AsyncHttpClient client = new AsyncHttpClient();
		client.post(HttpEngine.getEngine().getmContext(), request.getUrl(), params, new AsyncHttpResponseHandler() {
			@Override
		    public void onStart() {
				Log.d(TAG, "http post is start") ;
		    }
		    @Override
		    public void onSuccess(int statusCode, Header[] headers, byte[] r) {
		    	callonSuccess(statusCode, headers, r, request);
		    }
		    @Override
		    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
		    	callonFailure(statusCode, headers, errorResponse, e, request);
		    }
		});
	}
	private void callonFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e,Request request){
		 HttpResponseListener listener = request.getHttpResponseListener() ;
    	 if(listener != null){
    		listener.onFailure(statusCode, errorResponse == null?e.getMessage():new String(errorResponse), e);
    	 }else{
    		Log.d(TAG, "please add http response listener") ;
    	 }
	 }
	private void callonSuccess(int statusCode, Header[] headers, byte[] r,Request request){
		HttpResponseListener listener = request.getHttpResponseListener() ;
    	if(listener != null){
    		Response<Object> response = new Response<Object>() ;
    		/**判断回送数据是否正确*/
    		if(null == r){
    			response.setMode(null);
    			response.setCode(-100);
    			response.setErrorMsg("server response data is null");
    			response.setE(new NullPointerException("server response data is null"));
    		}else{
    			/**调用客户端 自定定义的json解析*/
    			Object t =listener.onJson(new String(r));
    			response.setMode(t);
    			response.setCode(200);
    		}
    		listener.onSuccess(response); /*回调客户端注册的成功*/
    	}else{
    		Log.d(TAG, "please add http response listener") ;
    	}
	}
}
