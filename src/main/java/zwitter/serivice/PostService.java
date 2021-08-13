package zwitter.serivice;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;


import java.util.Set;

@Service
public class PostService {

    private final static String STATUSID_KEY = "status_id:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    public String createPost(String userId, String message){
        String userStatusId = userService.createUserStatus(userId,message);
        if(StringUtils.isEmpty(userStatusId)){
            return "";
        }
        String statusKey = STATUSID_KEY+userStatusId;
        String postedTime =(String)redisTemplate.opsForHash().get(statusKey,"posted");
        if(StringUtils.isEmpty(postedTime)){
            return "";
        }
        redisTemplate.opsForZSet().add("profile:"+userId,userStatusId,Double.parseDouble(postedTime));
        syndicateStatus(userId,userStatusId,postedTime);
        return userStatusId;
    }

    private void syndicateStatus(String userId, String userStatusId, String postedTimeStr){
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        String followerKey = "follower:"+userId;
        Set<ZSetOperations.TypedTuple<String>> followers = redisTemplate.opsForZSet().rangeByScoreWithScores(followerKey,0,-1);

        redisTemplate.executePipelined((RedisCallback) con-> {
            followers.forEach(f->{
                String followerId = f.getValue();
                con.zAdd(serializer.serialize("home:"+followerId),Double.parseDouble(postedTimeStr),serializer.serialize(userStatusId) );

            });
            return null;
        });


    }

}
