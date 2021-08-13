package zwitter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import zwitter.serivice.UserService;

@RestController
public class ZwitterUserController {

    @Autowired
    private UserService userService;

    @GetMapping(value="/userRegister")
    public String createUser(String loginName,String userName){
        boolean flag = userService.createUser(loginName,userName);
        return "bingo";
    }



}
