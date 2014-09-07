package elegantuniv.atthack;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;

public class AsyncStringDownloader extends BaseAsyncDownloader {

	public interface OnCompletedListener {
		public void onCompleted(String url, String result);
		public void onErrorRaised(String url, Exception e);
		public void onPreExecute(String url);
	}
	
	/////get
	public class AsyncDownloadTask2 extends BaseDownloadTask {
		OnCompletedListener mListener;
		String result;
		StringEntity msEntity;
		
		public AsyncDownloadTask2(String url, String key, OnCompletedListener onCompletedListener){
			super(url, key);
			this.mListener = onCompletedListener;
		}
		
		@Override
		protected Void doInBackground(Void... params){
			DefaultHttpClient client = new DefaultHttpClient();
			
			if(cookieStore == null) {
				cookieStore = client.getCookieStore();
			} else {
				for(Cookie cookie :cookieStore.getCookies()) {
					client.getCookieStore().addCookie(cookie);
				}
			}
			
			HttpGet httpGet = new HttpGet(url);
			
			//if(msEntity != null){
				httpGet.setHeader("Accept", "application/json");
				httpGet.setHeader("Content-type", "application/json");				
			//}
			
			try {
	        	int retryCount = 0;

	        	while(result == null && retryCount < RETRY_LIMIT) {
	        		
	        		if (isCancelled()) {
	        			return null;
	        		}
	        		
	        		HttpResponse response = client.execute(httpGet);
	        		
		            final int statusCode = response.getStatusLine().getStatusCode();

		            if (statusCode != HttpStatus.SC_OK) {
		                return null;
		            }		            		            

		            final HttpEntity entity = response.getEntity();
		            
		            if (entity != null) {		            	
		            	String result = EntityUtils.toString(entity);			            	
		            	this.result = result;
		            }
	        		retryCount++;
	        	}

	        	if(result != null) {
	            	if(url.contains("login/signin")) {
            			client.getCookieStore().clearExpired(new Date(System.currentTimeMillis()));
            			
            			CookieStore cs = client.getCookieStore();
            			if(cs != null) {
            				List<Cookie> cookies = cs.getCookies();
            				if(cookies != null && cookies.size() > 0) {
            					for(int i=0; i<cookies.size(); i++) {
            						cookieStore.addCookie(cookies.get(i));
            					}
            				}
            			}
	            	} else if(url.contains("login/signout")) {
		        		clearCookie();
		        	}
	            }
	        } catch (Exception e) {
	            httpGet.abort();
	            e.printStackTrace();
	        }
	        return null;
		}
						
		@Override
		protected void onPostExecute(Void v) {
			
			downloadComplete(this);
			
			if(mListener != null) {
				if(result == null) {
					mListener.onErrorRaised(url, null);
				} else {
					mListener.onCompleted(url, result);
				}
			}
		}
		
		@Override
        protected void onPreExecute() {
			if(mListener != null) {
				mListener.onPreExecute(url);
			}
        }
		
		protected HttpURLConnection getRequest() throws IOException {
			return (HttpURLConnection) new URL(url).openConnection();
		}
	}
	
	////post
	public class AsyncDownloadTask extends BaseDownloadTask {
		String postValue;
		String result;
		OnCompletedListener mListener;
		StringEntity msEntity;
		
		public AsyncDownloadTask(String url, String key, OnCompletedListener onCompletedListener) {
			super(url, key);
			this.mListener = onCompletedListener;
		}
		
		public AsyncDownloadTask(String url, String key, OnCompletedListener onCompletedListener, String postValue) {
			super(url, key);
			this.mListener = onCompletedListener;
			this.postValue = postValue;
		}

		
		public AsyncDownloadTask(String url, String key, OnCompletedListener onCompletedListener, StringEntity sEntity){
			super(url, key);
			this.mListener = onCompletedListener;
			this.msEntity = sEntity;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			DefaultHttpClient client = new DefaultHttpClient();
			
			HttpPost httpPost = null;
			
			try {
				httpPost = new HttpPost(url);
				if(msEntity != null){
					httpPost.setHeader("Accept", "application/json");
					httpPost.setHeader("Content-type", "application/json");
				}else{
					httpPost.setHeader("Connection", "Keep-Alive");
					httpPost.setHeader("Accept-Charset", "UTF-8");
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
	        
	        //Add postData.
			if(msEntity != null){
				httpPost.setEntity(msEntity);				
			}else {
				if(postValue != null && !postValue.equals("")) {
		        	try {
			        	UrlEncodedFormEntity entity;
			        	ArrayList<BasicNameValuePair> paramsList = new ArrayList<BasicNameValuePair>();
			        	
			        	for(String line : postValue.split("&") ) {
							String[] line_parts = line.split("=");
							paramsList.add(new BasicNameValuePair(line_parts[0], line_parts[1]));
						}
			        	
						entity = new UrlEncodedFormEntity(paramsList, "UTF-8");
						httpPost.setEntity(entity);
		        	} catch(Exception e) {
		        		e.printStackTrace();
		        	}										
		        }
			}			
			
	        try {
	        	int retryCount = 0;

	        	while(result == null && retryCount < RETRY_LIMIT) {
	        		
	        		if (isCancelled()) {
	        			return null;
	        		}
	        			
	        		HttpResponse response = client.execute(httpPost);
	        		
		            final int statusCode = response.getStatusLine().getStatusCode();

		            if (statusCode != HttpStatus.SC_OK) {
		                return null;
		            }		            		            

		            final HttpEntity entity = response.getEntity();
		            
		            if (entity != null) {		            	
		            	String result = EntityUtils.toString(entity);			            	
		            	this.result = result;
		            }
	        		retryCount++;
	        	}

	        	if(result != null) {

	            }
	        } catch (Exception e) {
	            httpPost.abort();
	            e.printStackTrace();
	        }
	        return null;
		}
		
		@Override
		protected void onPostExecute(Void v) {
			
			downloadComplete(this);
			
			if(mListener != null) {
				if(result == null) {
					mListener.onErrorRaised(url, null);
				} else {
					mListener.onCompleted(url, result);
				}
			}
		}
		
		@Override
        protected void onPreExecute() {
			if(mListener != null) {
				mListener.onPreExecute(url);
			}
        }
		
		protected HttpURLConnection getRequest() throws IOException {
			return (HttpURLConnection) new URL(url).openConnection();
		}
	}
	
	protected static AsyncStringDownloader mAsyncStringDownloader;
	protected AsyncStringDownloader() {}
	
	public static AsyncStringDownloader getInstance() {
		if( mAsyncStringDownloader == null ) {
			mAsyncStringDownloader = new AsyncStringDownloader();
		}
		
		return mAsyncStringDownloader;
	}
	
	protected void addTask(String url, String key, OnCompletedListener listener) {
		AsyncDownloadTask task = new AsyncDownloadTask(url, key, listener);
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}
	
	protected void addTask(String url, String key, OnCompletedListener listener, String postValue) {
		AsyncDownloadTask task = new AsyncDownloadTask(url, key, listener, postValue);
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}
	
	protected void addTask(String url, String key, OnCompletedListener listener, StringEntity sEntity){
		AsyncDownloadTask task = new AsyncDownloadTask(url, key, listener, sEntity);
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}
	
	protected void addTask2(String url, String key, OnCompletedListener listener) {
		AsyncDownloadTask2 task = new AsyncDownloadTask2(url, key, listener);
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}
	
	public static void download2(String url, String key, OnCompletedListener listener){
		AsyncStringDownloader.getInstance().addTask2(url, key, listener);
	}
	
	public static void download(String url, String key, OnCompletedListener listener, String postValue) {
		AsyncStringDownloader.getInstance().addTask(url, key, listener, postValue);
	}
	
	public static void download(String url, String key, OnCompletedListener listener) {
		AsyncStringDownloader.getInstance().addTask(url, key, listener);
	}
	
	public static void download(String url, String key, OnCompletedListener listener, StringEntity sEntity){
		AsyncStringDownloader.getInstance().addTask(url, key, listener, sEntity);
	}
	
	public static void removeTasksByKey(String key) {
		getInstance().removeTasks(getInstance().getDownloadQueue(), key);
	}
}