package jp.vstone.sotasample;

public class RequestParams{
	public int keep=0;
	public int autoend=0;
	public String target="";
	public String layerName="";
	public String content="";
	public double duration=50;
	public double[] points = new double[]{0.0,0.0,0.0}; //参照型であることに注意せよ
	public String actionName="";
	public Double angle=0.0; //参照型であることに注意せよ
	public String uniquename="";
	public boolean fromUser=false;

	public RequestParams(String uniquename){
		this.uniquename=uniquename;
	}

	//値渡しによるコピー
	public void copy(RequestParams requestParams){
		if(requestParams!=null){
		this.keep=requestParams.keep;
		this.autoend=requestParams.autoend;
		this.target=requestParams.target;
		this.layerName=requestParams.layerName;
		this.content=requestParams.content;
		this.duration=requestParams.duration;
		this.points[0]=(double)requestParams.points[0]; //値渡し
		this.points[1]=(double)requestParams.points[1]; //値渡し
		this.points[2]=(double)requestParams.points[2]; //値渡し
		this.actionName=requestParams.actionName;
		this.angle=(double)requestParams.angle; //値渡し
		this.fromUser=requestParams.fromUser;
		}
	}

}
