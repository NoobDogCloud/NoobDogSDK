package common.java.ServiceTemplate;

@FunctionalInterface
public interface ApiTokenSender {
    /**
     * @param serviceName 服务名称
     * @param className   类名称
     * @param actionName  方法名称
     * @param code        验证码
     */
    void run(String serviceName, String className, String actionName, String code);
}
