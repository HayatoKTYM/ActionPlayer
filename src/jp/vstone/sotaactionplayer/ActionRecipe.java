package jp.vstone.sotaactionplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.vstone.RobotLib.CRobotUtil;

public class ActionRecipe {

	HashMap<String, Recipe> recipe_map = new HashMap<String,Recipe>();
	private static ActionRecipe instance ;
	static String TAG="ActionRecipe";

	public ActionRecipe() {

		setRecipe("le", new String[][]{{"head_y","head_p"}});
		setRecipe("ln", new String[][]{{"le","body_y"}});
		setRecipe("lt", new String[][]{{"body_y"}});
		setRecipe("speakto", new String[][]{{"le","speak"}});
		setRecipe("nod", new String[][]{{"head_p"},{"head_p"}});
		setRecipe("cock", new String[][]{{"head_r"},{"head_r"}});
		setRecipe("blink", new String[][]{{"led"},{"wait"},{"led"}});
		setRecipe("shake_head", new String[][]{{"head_y"},{"head_y"},{"head_y"}});
		setRecipe("hi", new String[][]{{"r_sho","r_elb"},{"wait"},{"r_sho","r_elb"}});
		setRecipe("shake_right_hand", new String[][]{{"r_sho","r_elb"},{"wait"},{"r_sho","r_elb"}});
		setRecipe("shake_left_hand", new String[][]{{"l_sho","l_elb"},{"wait"},{"l_sho","l_elb"}});
		setRecipe("banzai", new String[][]{{"shake_left_hand","shake_right_hand"}});
		setRecipe("home", new String[][]{{"head_p","head_y","head_r","body_y","r_sho","r_elb","l_sho","l_elb"}});

	}

	//シングルトン化
	public static ActionRecipe getInstance() {
		if (instance != null)
			return instance;
		instance = new ActionRecipe();
		return instance;
	}

	//入れ子構造を適当な変数名で書き換える
	public static Nest opennest(String nest){
		HashMap<String,String> variable =new  HashMap<String,String>(); //and,thenの解析にひっからない適当な文字に置換する
		int Rcount=0;
		boolean Flag=false;
		int[] sf={0,0};
		int varnumm=0;

		for(int i=0;i<nest.length();i++){
			char mozi = nest.charAt(i);
			if(mozi==(char)'{'){
				Rcount++;
			}else if(mozi==(char)'}'){
				Rcount--;
			}
			if(Rcount>0 && !Flag){
				Flag=true;
				sf[0]=i;
			}else if(Rcount==0 && Flag){
				sf[1]=i;
				Flag=false;
				variable.put("var"+Integer.toString(varnumm), nest.substring(sf[0]+1, sf[1]));
				nest=nest.substring(0, sf[0])+"var"+Integer.toString(varnumm)+nest.substring(sf[1]+1,nest.length());
				varnumm++;
				i=-1; //nestを更新したのでやり直し
			}
		}
		Nest result=new Nest();

		result.nest=nest;
		result.variable=variable;
		return result;
	}

	Pattern kakko_p = Pattern.compile("(.*)\\[([^\\[]*)\\]$");

	public Recipe makeRecipe(String name){
		 	Recipe recipe=new Recipe();
		 	Nest nest=opennest(name);

		 	String action_name=nest.nest;

			ArrayList<String[]> actions = new ArrayList<String[]>();
			ArrayList<String[]> args = new ArrayList<String[]>();

			String[] action_sequence = action_name.split("_then_");

			for(int i=0;i<action_sequence.length;i++){
				String[] action_tree = action_sequence[i].split("_and_");
				String[] arg_tree=action_tree.clone();
				//各要素の引数を格納し、また既存レシピか複合レシピから合成されることを確認
				for(int j=0;j<action_tree.length;j++){
					arg_tree[j]="";
					Matcher m = kakko_p.matcher(action_tree[j]); //引数分離
					if(m.find()){
						action_tree[j] = m.group(1);
						arg_tree[j] = m.group(2);

					}
					if(! recipe_map.containsKey(action_tree[j]) && !SotaActionPlayer.Basic_motion_map.containsKey(action_tree[j]) && !nest.variable.containsKey(action_tree[j])){
						CRobotUtil.Log(TAG,"レシピ合成失敗:"+action_name+" 素材不足:"+action_tree[j]);
						return null;
					}
					if(nest.variable.containsKey(action_tree[j])){
						action_tree[j]=nest.variable.get(action_tree[j]);
					}
				}
				actions.add(i, action_tree);
				args.add(i,arg_tree);
			}
		 recipe.actions=actions;
		 recipe.args=args;
		 return recipe;
	}

	public Recipe getRecipe(String name){
		 Recipe recipe=new Recipe();
		if(recipe_map.containsKey(name)){
			recipe = recipe_map.get(name);
		}else{ //有機命令合成
			recipe = makeRecipe(name);
		}
		return recipe;
	}


	public void setRecipe(String name,String[][] action_strings){
		ArrayList<String[]> actions = new ArrayList<String[]>();
		ArrayList<String[]> args = new ArrayList<String[]>();
		for(int i=0;i<action_strings.length;i++){
			actions.add(i, action_strings[i]);
			String[] arg = action_strings[i].clone();
			for(int j=0;j<arg.length;j++){
				arg[j]=""; //初期化
			}
			args.add(i, arg);
		}
		Recipe recipe=new Recipe();
		recipe.actions=actions;
		recipe.args=args;
		recipe_map.put(name, recipe);
	}

}

class Recipe{
	ArrayList<String[]> actions = new ArrayList<String[]>();
	ArrayList<String[]> args = new ArrayList<String[]>();
	public int getNodenum(int sequens){
		return actions.get(sequens).length;
	}
}

class Nest{
	String nest;
	HashMap<String,String> variable;
}