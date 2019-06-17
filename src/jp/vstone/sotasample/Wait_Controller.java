package jp.vstone.sotasample;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;

import jp.vstone.RobotLib.CRobotUtil;

public class Wait_Controller extends BasicMotion {
	   private static Wait_Controller instance ;


		public Wait_Controller() {
				TAG="wait controller";
				this.setDaemon(true);
				this.start();
		}

		//シングルトン化
		public static Wait_Controller getInstance() {
			if (instance != null)
				return instance;
			instance = new Wait_Controller();
			return instance;
		}

		 HashMap<String,Timer> timer_map = new  HashMap<String,Timer>();
		 Queue<String> keep_action_uniquename =new  ArrayDeque<String>();
		 Queue<String> excuted_item = new  ArrayDeque<String>();

		   //実行完了したものからexcutedに移動
		   @Override
		   public void updataStatus(){
			 Iterator<String> ite = timer_map.keySet().iterator();
			 while(ite.hasNext()){
				 String key = ite.next();
				 if(! timer_map.get(key).isAlive()){
						 excuted_item.add(key);
						 CRobotUtil.Log(TAG,"実行完了 "+key);
				 };
			 }
			  Iterator<String> ite2 = excuted_item.iterator();
			 while(ite2.hasNext()){
				 String key = ite2.next();
				 if(timer_map.containsKey(key)){
						timer_map.remove(key);
				 };
			 }
		   }


		   @Override
			public void action(RequestParams requestParams){
					if(requestParams==null){
						return;
					}

					if(requestParams.fromUser){
						if(requestParams.keep == 1 ){
							add_keep_map(requestParams);
						}else if(requestParams.keep <0 ){
							remove_keep_map(requestParams);
						}
					}

					play(requestParams);
					CRobotUtil.Log(TAG,requestParams.layerName+" x:"+Integer.toString((int)requestParams.points[0])+" uniquename:"+requestParams.uniquename);

			}

			@Override
			public synchronized void add_keep_map(RequestParams tmp){
				keep_action_uniquename.add(tmp.uniquename);
				CRobotUtil.Log(TAG,tmp.layerName+" action keep "+tmp.uniquename);
		}


			public synchronized void remove_keep_map(RequestParams requestParams){
				if(keep_action_uniquename.contains(requestParams.uniquename)){
					keep_action_uniquename.remove(requestParams.uniquename);
					CRobotUtil.Log(TAG,requestParams.layerName+" remove keep "+requestParams.uniquename);
				}

			}

			@SuppressWarnings("deprecation")
			@Override
			public void actionStop(RequestParams requestParams){
				if(timer_map.containsKey(requestParams.uniquename)){
					timer_map.get(requestParams.uniquename).stop();
				}

			}

			@Override
			public boolean play(RequestParams requestParams){
				Timer timer = new Timer((int)Math.max(requestParams.points[0], requestParams.duration));
				timer.setDaemon(true);
				timer.start();
				timer_map.put(requestParams.uniquename, timer);
				return true;
			}


			@Override
			public void run() {
				CRobotUtil.Log(TAG,"start thread");

				while(true){
					updataStatus();
					if(!queue.isEmpty()){
						temp_item=queue.poll();
						action(temp_item);
						temp_item=null;
					}


						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					}
				}

			//Queueにもkeepにも実行中にもいなかったら枯れたと判断
			@Override
			public boolean isWither(RequestParams requestParams){
				if(!keep_action_uniquename.contains(requestParams.uniquename) && !queue.contains(requestParams) && ! Objects.equals(temp_item,requestParams) ){
					if(excuted_item.contains(requestParams.uniquename)){
						excuted_item.remove(requestParams.uniquename);
					}

					return true;
				}

				return false;
			}

			@Override
			public boolean isFinished(RequestParams requestParams){
				return excuted_item.contains(requestParams.uniquename);
			}

			@Override
			public void cancel(RequestParams requestParams){
				remove_keep_map(requestParams);
				actionStop(requestParams);
			}

			@Override
			public String node_graph(RequestParams requestParams){
				if(timer_map.containsKey(requestParams.uniquename)){
					return "E"; //実行中
				}else if(keep_action_uniquename.contains(requestParams.uniquename) && requestParams.layerName.equals(max_layer_in_keep())){
					return "K"; //保留中
				}
				return "N"; //停止中
			}

			public class Timer extends Thread{
				int ms=0;

				public Timer(int ms) {
					this.ms=ms;
				}

				@Override
				public void run() {
					try {
						Thread.sleep(ms);
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				}
			}

}


