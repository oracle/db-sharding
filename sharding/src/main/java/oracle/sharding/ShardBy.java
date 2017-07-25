package oracle.sharding;

/**
 * Created by somestuff on 4/1/17.
 */
public enum ShardBy {
    HASH(1), RANGE(2), LIST(4), NONE(0);

    int id;
    ShardBy(int aid) { id = aid; }
}
