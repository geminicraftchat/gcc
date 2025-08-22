package cn.ningmo.geminicraftchat.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API密钥加密工具类
 * 提供密钥的加密存储和解密功能
 */
public class KeyEncryption {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final String KEY_FILE = "security.key";
    private static final Logger logger = Logger.getLogger(KeyEncryption.class.getName());
    
    private final Path keyFilePath;
    private SecretKey secretKey;
    
    public KeyEncryption(Path dataFolder) {
        this.keyFilePath = dataFolder.resolve(KEY_FILE);
        initializeKey();
    }
    
    /**
     * 初始化加密密钥
     */
    private void initializeKey() {
        try {
            if (Files.exists(keyFilePath)) {
                // 从文件加载密钥
                byte[] keyBytes = Files.readAllBytes(keyFilePath);
                secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            } else {
                // 生成新密钥
                generateNewKey();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "初始化加密密钥失败", e);
            throw new RuntimeException("无法初始化密钥加密系统", e);
        }
    }
    
    /**
     * 生成新的加密密钥
     */
    private void generateNewKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(256, new SecureRandom());
        secretKey = keyGen.generateKey();
        
        // 保存密钥到文件
        Files.write(keyFilePath, secretKey.getEncoded());
        
        // 设置文件权限（仅所有者可读写）
        try {
            Files.setPosixFilePermissions(keyFilePath, 
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // Windows系统不支持POSIX权限，忽略
            logger.info("当前系统不支持POSIX文件权限设置");
        }
        
        logger.info("已生成新的加密密钥");
    }
    
    /**
     * 加密字符串
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加密失败", e);
            throw new RuntimeException("加密操作失败", e);
        }
    }
    
    /**
     * 解密字符串
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            logger.log(Level.WARNING, "解密失败，可能是明文密钥", e);
            // 如果解密失败，可能是明文密钥，直接返回
            return encryptedText;
        }
    }
    
    /**
     * 检查字符串是否已加密
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            // 尝试Base64解码
            Base64.getDecoder().decode(text);
            // 如果能解码且长度合理，可能是加密的
            return text.length() > 20 && !text.contains(" ");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 安全地清理密钥（在内存中）
     */
    public void clearKey() {
        if (secretKey != null) {
            // Java中无法直接清理SecretKey的内存，但可以置空引用
            secretKey = null;
        }
    }
}