package com.personblog.common.utils;

public class UserContextHolder {
    private static final ThreadLocal<Long> UserContext=new ThreadLocal<>();

    public static void setUserId(Long userId){
        UserContext.set(userId);
    }

    public static Long getUserId(){
        return UserContext.get();
    }

    public static void clearUserId(){
        UserContext.remove();
    }
}
