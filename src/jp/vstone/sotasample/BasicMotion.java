package jp.vstone.sotasample;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;

import jp.vstone.RobotLib.CRobotUtil;

public class BasicMotion extends Thread implements BasicNode,Request_Watcher{

	static RequestParams excuting_item;
	static RequestParams temp_item;
	public static RequestParams excuted_item=new RequestParams("");
	static Queue<RequestParams> queue = new ArrayDeque<RequestParams>();

	public String status="ready";

	boolean priority_updataFlag=false;

	 HashMap<String,Integer> priority_map = new  HashMap<String,Integer>();

	 HashMap<String,RequestParams> keep_action =new  HashMap<String,RequestParams>();

	 HashMap<String,Boolean> autoend_action =new  HashMap<String,Boolean>();

	   String current_action_layerName="";

	   String TAG ="BasicMotion";

	   public int getpriority(String layerName) {
		   int priority=0;
			if(priority_map.containsKey(layerName)){
				priority=priority_map.get(layerName);
			}
			return priority;
	}

	   public void updataStatus(){

	   }

		public void action(RequestParams requestParams){
				if(requestParams==null){
					return;
				}

				//現在のlayer達の活性度を問い合わせる
				int current_priority=getpriority(current_action_layerName);
				int priority=getpriority(requestParams.layerName);

				if(priority<0){
					return;
				}

				if(requestParams.fromUser){
					if(requestParams.keep == 1 ){
						add_keep_map(requestParams);
					}else if(requestParams.keep <0 ){
						remove_keep_map(requestParams.layerName);
					}
				}

				if(priority<getpriority(max_layer_in_keep())){
					return;
				}

				if(status.equals("play")){
					if(priority<current_priority){
						//無視される
						//CRobotUtil.Log(TAG,"ignore:"+layerName+ " priority:"+Integer.toString(priority));
						return;
					}else{
						actionStop(requestParams);
					}
				}



				if(play(requestParams)){
					status="play";
					current_action_layerName=requestParams.layerName;
					excuting_item=requestParams;
					if(requestParams.fromUser){
						CRobotUtil.Log(TAG,requestParams.layerName+" x:"+Integer.toString((int)requestParams.points[0])+" y:"+Integer.toString((int)requestParams.points[1])+" duration:"+Integer.toString((int)requestParams.duration)+" priority:"+Integer.toString(priority));
						//CRobotUtil.Log(TAG,current_action_layerName+" priority:"+Integer.toString(current_priority)+status);
					}
				}else{
					remove_keep_map(requestParams.layerName); //実行不可能な命令なのでkeepからも削除
				}
		}

		public void actionStop(RequestParams requestParams){

		}

		public boolean play(RequestParams requestParams){
			return false;
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

				}else if(status.equals("ready") && ! keep_action.isEmpty()){
					if(excuted_item==null){
						action(keep_action.get(max_layer_in_keep()));
					}else if(! Objects.equals(excuted_item,keep_action.get(max_layer_in_keep()))){ //同一行動の実施防止
						action(keep_action.get(max_layer_in_keep()));
						CRobotUtil.Log(TAG,"Restart:"+max_layer_in_keep());
					}
				}else if(priority_updataFlag && ! keep_action.isEmpty()){ //play中にpriorityに変更があった場合
					priority_updataFlag=false;
					if(excuted_item==null){
						if(! Objects.equals(excuting_item,keep_action.get(max_layer_in_keep()))){
							action(keep_action.get(max_layer_in_keep()));
						}
					}else if(! Objects.equals(excuted_item,keep_action.get(max_layer_in_keep()))){ //同一行動の実施防止
						if(! Objects.equals(excuting_item,keep_action.get(max_layer_in_keep()))){
							action(keep_action.get(max_layer_in_keep()));
							CRobotUtil.Log(TAG,"Intterrupt by priority updata:"+max_layer_in_keep());
						}
					}
				}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				}
			}

	   public String max_layer_in_keep(){
		   int max_priority=-1;
		   String max_layerName="";
		   String[] layers =keep_action.keySet().toArray(new String[0]);
		   for(int i=0;i<layers.length;i++){
			   if(priority_map.containsKey(layers[i])){
				   if(max_priority<priority_map.get(layers[i])){
					   max_priority=priority_map.get(layers[i]);
					   max_layerName=layers[i];
				   }else if(priority_map.get(layers[i])<0){ //非活性化
					   keep_action.remove(layers[i]);
				   }
			   }
		   }
			if(max_priority<0){
				return "";
			}
		   return max_layerName;
	   }

		public synchronized void remove_keep_map(String layerName){
			if(keep_action.containsKey(layerName)){
				keep_action.remove(layerName);
				CRobotUtil.Log(TAG,layerName+" keep remove");
			}

		}

		public synchronized void add_keep_map(RequestParams tmp){
				tmp.fromUser=false;
				keep_action.put(tmp.layerName, tmp);
				CRobotUtil.Log(TAG,tmp.layerName+" action keep");
		}

		public void setQueue(RequestParams item){
			queue.add(item);
			CRobotUtil.Log(TAG,"add queue");
		}

		//Queueにもkeepにも実行中にもいなかったら枯れたと判断
		public boolean isWither(RequestParams requestParams){
			if(!keep_action.containsValue(requestParams) && ! Objects.equals(excuting_item,requestParams) && !queue.contains(requestParams) && ! Objects.equals(temp_item,requestParams) ){
				return true;
			}
			return false;
		}

		public boolean isFinished(RequestParams requestParams){
			return Objects.equals(excuted_item,requestParams);
		}

		public String node_graph(RequestParams requestParams){
			if(Objects.equals(excuting_item,requestParams)){
				return "E"; //実行中
			}else if(keep_action.containsValue(requestParams) && requestParams.layerName.equals(max_layer_in_keep())){
				return "K"; //保留中
			}
			return "N"; //停止中
		}

		public void cancel(RequestParams requestParams){
			remove_keep_map(requestParams.layerName);
			if(Objects.equals(excuting_item,requestParams)){
				actionStop(requestParams);
			}
		}


		@Override
		   public void update_priority_callback( HashMap<String,Integer> priority_map){
			   this.priority_map=priority_map;
			   priority_updataFlag=true;
		   }

}


