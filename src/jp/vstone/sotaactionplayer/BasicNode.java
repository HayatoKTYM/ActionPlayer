package jp.vstone.sotaactionplayer;

public interface BasicNode {
 public boolean isWither(RequestParams requestParams);
 public boolean isFinished(RequestParams requestParams);
 public String node_graph(RequestParams requestParams);
 public void cancel(RequestParams requestParams);
}
