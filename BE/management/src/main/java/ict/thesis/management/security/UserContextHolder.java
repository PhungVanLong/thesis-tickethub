package ict.thesis.management.security;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> userContextThreadLocal = new ThreadLocal<>();

    public static UserContext getContext() {
        UserContext context = userContextThreadLocal.get();
        if (context == null) {
            context = new UserContext();
            userContextThreadLocal.set(context);
        }
        return context;
    }

    public static void setContext(UserContext context) {
        userContextThreadLocal.set(context);
    }

    public static void clearContext() {
        userContextThreadLocal.remove();
    }
}
