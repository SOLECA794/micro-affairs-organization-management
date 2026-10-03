import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * 演示账号 BCrypt 密文生成与回验工具（用于生成 db/data.sql 中的 password 字段）。
 *
 * 运行方式（本地 JDK 17+，需 spring-security-crypto 6.x jar 在 classpath）：
 *   mvn -s <settings> dependency:get -Dartifact=org.springframework.security:spring-security-crypto:6.3.5
 *   javac -cp <jar> -d tools/out tools/BcryptHashGenerator.java
 *   java -cp "tools/out;<jar路径>" BcryptHashGenerator
 *
 * 对每个账号：BCrypt.gensalt(10) 生成盐 → hashpw 生成密文 → checkpw 回验，
 * 回验不通过则非零退出，确保写入 data.sql 的密文真实可用。
 */
public class BcryptHashGenerator {

    private static final String[][] ACCOUNTS = {
            {"admin",  "admin123"},
            {"mgr001", "mgr123456"},
            {"mgr002", "mgr123456"},
            {"stu001", "stu123456"},
            {"stu002", "stu123456"},
            {"stu003", "stu123456"},
            {"stu004", "stu123456"},
            {"stu005", "stu123456"},
    };

    public static void main(String[] args) {
        for (String[] account : ACCOUNTS) {
            String username = account[0];
            String rawPassword = account[1];
            String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
            boolean ok = BCrypt.checkpw(rawPassword, hash);
            if (!ok) {
                System.err.println("VERIFY FAILED: " + username);
                System.exit(1);
            }
            System.out.println(username + "|" + rawPassword + "|" + hash + "|verify=PASS");
        }
        System.out.println("ALL_ACCOUNTS_VERIFIED");
    }
}
