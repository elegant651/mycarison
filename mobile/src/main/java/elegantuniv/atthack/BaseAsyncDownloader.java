package elegantuniv.atthack;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.os.AsyncTask;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;


public abstract class BaseAsyncDownloader {
	
	protected static CookieStore cookieStore;
	protected final int RETRY_LIMIT = 3;
	protected ArrayList<BaseDownloadTask> queue = new ArrayList<BaseDownloadTask>();
	protected BaseDownloadTask currentTask;
	
	public static void setCookieStore(String cookieString) {

		if(cookieString == null || cookieString.equals("")) {
			return;
		}
		
		cookieStore = new BasicCookieStore();
		
		cookieString = cookieString.replace("; ", "&");
		cookieString = cookieString.replace(";", "&");

		for(String line : cookieString.split("&")) {
			String[] line_parts = line.split("=");
			BasicClientCookie cookie = new BasicClientCookie(line_parts[0], line_parts[1]);
			cookie.setDomain("knowre.com");
			cookie.setExpiryDate(new Date(200, 0, 1));
			cookieStore.addCookie(cookie);
		}
	}
	
	public static String getCookieStringFromCookieStore() {
		
		if(cookieStore == null || cookieStore.getCookies() == null || cookieStore.getCookies().size() == 0) {
			return null;
		}
		
		List<Cookie> arCookie = cookieStore.getCookies();
		String cookieString = "";
		
		for(int i=0; i<arCookie.size(); i++) {
			
			if(i != 0) {
				cookieString += "; ";
			}
			
			Cookie ck = arCookie.get(i);
			cookieString += ck.getName() + "=" + ck.getValue();
		}
		
		return cookieString;
	}
	
	public static void setCookieToCookieManager() {
		
		if(cookieStore == null || cookieStore.getCookies() == null || cookieStore.getCookies().size() == 0) {
			return;
		}
		
		CookieManager.getInstance().setAcceptCookie(true);

		List<Cookie> arCookie = cookieStore.getCookies();
		for(Cookie cookie : arCookie) {
			String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=knowre.com";
			CookieManager.getInstance().setCookie("knowre.com", cookieString);
		}
		
		CookieSyncManager.getInstance().sync();
	}
	
	public static void clearCookie() {
		cookieStore = null;
	}
	
	protected void checkIsDownloading() {
		
		if(queue.size() == 1) {
			currentTask = queue.get(0); 
			currentTask.execute();
		}
	}
	
	protected void addTaskToQueue(BaseDownloadTask task) {
		
		try {
			/* AsyncStringDownloader에서 추가한 경우,
			 * 기존에 있던것들 중 같은 url이 있는지 확인해보고,
			 * 있다면 제거, 만약 실행중이라면 취소하고 다음 task 실행.
			 */
			String currentUrl = task.getUrl();
			String url = null;
			int size = queue.size();
			
			if(size != 0) {
				for(int i=0; i<size; i++) {
					url = queue.get(i).getUrl();

					if(task instanceof AsyncStringDownloader.AsyncDownloadTask && url.equals(currentUrl)) {
						queue.remove(i);
						
						if(i==0) {
							//취소하고 다음 task 실행.
							currentTask.cancel(true);
							currentTask = queue.get(0); 
							currentTask.execute();
						}
						break;
					}
				}
			}

			queue.add(task);
			checkIsDownloading();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeTasks(ArrayList<BaseDownloadTask> arList, String key) {

		try {
			if(arList == null || arList.size() == 0) {
				return;
			}
			
			int size = arList.size();
			
			for(int i=size - 1; i>= 0; i--) {
				if(arList.get(i).getKey() != null && key.equals(arList.get(i).getKey())) {
					BaseDownloadTask bdt = arList.get(i);
					arList.remove(i);
					bdt.cancel(true);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void downloadComplete(AsyncTask<Void, Void, Void> task) {
		
		if(queue != null && queue.size() > 0 && queue.get(0) == task) {
			queue.remove(0);
			
			if(queue.size() > 0) {
				(queue.get(0)).execute();
			}
		}
	}

	public ArrayList<BaseDownloadTask> getDownloadQueue() {
		return queue;
	}
	
////////// classes.
	
	protected class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
	
	protected abstract class BaseDownloadTask extends AsyncTask<Void, Void, Void> {
		
		protected String url;
		protected String key;
		
		public BaseDownloadTask(String url, String key) {
			if(!url.contains("http")) {
				this.url = "http://" + url;
			} else {
				this.url = url;
			}
			this.key = key;
		}
		
		public String getUrl() {
			return url;
		}
		
		public String getKey() {
			return key;
		}
	}
}
