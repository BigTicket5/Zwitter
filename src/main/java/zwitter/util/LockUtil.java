package zwitter.util;

import com.sun.istack.internal.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LockUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean getLock(@NotNull final String key){
        boolean result = false;
        try{
           result = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key,"EXIST", 2,TimeUnit.MINUTES));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

}
