package zwitter.serivice;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import zwitter.util.LockUtil;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final static String USERID_KEY = "user_id:";
    private final static String USER_KEY = "user";
    private final static String STATUSID_KEY = "status_id:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LockUtil lockUtil;

    public boolean createUser(String loginName, String userName){
        if(StringUtils.isEmpty(loginName)||StringUtils.isEmpty(userName)){
            return false;
        }
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        String lnameStr = loginName.toLowerCase();
        if(Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("nicknameset",loginName))){
            log.info("nick name {} exists",loginName);
            return false;
        }
        if(lockUtil.getLock(lnameStr)){
            String userId = String.valueOf(redisTemplate.opsForValue().increment(USERID_KEY));
            redisTemplate.executePipelined(new RedisCallback<String>() {
                @Override
                public String doInRedis(RedisConnection connection){
                    connection.sAdd(serializer.serialize("nicknameset"),serializer.serialize(lnameStr));
                    connection.hSet(serializer.serialize(USER_KEY),serializer.serialize(lnameStr),serializer.serialize(userId));
                    String key = USER_KEY+":"+userId;
                    connection.hMSet(serializer.serialize(key),userProp(lnameStr,userName,userId,serializer));
                    return null;
                }
            });
            return true;
        }
        return false;
    }

    private Map<byte[],byte[]> userProp(String loginName,String userName, String userId,RedisSerializer<String> serializer){
        Map<byte[],byte[]> newUser  = new HashMap<>();
        newUser.put(serializer.serialize("loginName"),serializer.serialize(loginName));
        newUser.put(serializer.serialize("id"),serializer.serialize(userId));
        newUser.put(serializer.serialize("userName"),serializer.serialize(userName));
        newUser.put(serializer.serialize("followers"),serializer.serialize("0"));
        newUser.put(serializer.serialize("following"),serializer.serialize("0"));
        newUser.put(serializer.serialize("posts"),serializer.serialize("0"));
        newUser.put(serializer.serialize("signup"),serializer.serialize(String.valueOf(System.currentTimeMillis())));
        return newUser;
    }

    public String createUserStatus(String userId, String message){
        String userKey = USER_KEY+":"+userId;
        Object loginName = redisTemplate.opsForHash().get(userKey,"loginName");
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        if(loginName==null){
            return "";
        }
        String statusId = String.valueOf(redisTemplate.opsForValue().increment(STATUSID_KEY));
        redisTemplate.executePipelined((RedisCallback) connection-> {
            String statusKey = STATUSID_KEY+statusId;
            String timeStamp = String.valueOf(System.currentTimeMillis());
            connection.hMSet(serializer.serialize(statusKey),userStatus(userId,message,statusId,loginName.toString(),timeStamp,serializer));
            connection.hIncrBy(serializer.serialize(userKey),serializer.serialize("posts"),1);
            return null;
        });
        return statusId;
    }

    private Map<byte[],byte[]> userStatus(String userId,String message, String statusId,String loginName,String timeStamp, RedisSerializer<String> serializer){
        Map<byte[],byte[]> newStatus= new HashMap<>();
        newStatus.put(serializer.serialize("message"),serializer.serialize(message));
        newStatus.put(serializer.serialize("posted"),serializer.serialize(timeStamp));
        newStatus.put(serializer.serialize("statusId"),serializer.serialize(statusId));
        newStatus.put(serializer.serialize("userId"),serializer.serialize(userId));
        newStatus.put(serializer.serialize("loginName"),serializer.serialize(loginName));
        return newStatus;
    }

}
