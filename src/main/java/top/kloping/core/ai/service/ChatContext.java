package top.kloping.core.ai.service;

import lombok.Data;
import top.kloping.core.ai.dto.Message;
import top.kloping.core.ai.dto.SystemMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 聊天上下文类
 * 管理对话历史和系统消息
 *
 * @author github kloping
 * @since 2025/9/19
 */
@Data
public class ChatContext {
    private String model;
    private Integer max = 50; // 默认最大消息数
    private SystemMessage systemMessage;
    private final List<Message<?>> messages = Collections.synchronizedList(new ArrayList<>());
    
    // 读写锁用于更细粒度的并发控制
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 添加消息到上下文
     * 如果超过最大数量，则移除最早的消息
     */
    public void addMessage(Message<?> message) {
        if (message == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            if (max != null && messages.size() >= max) {
                messages.remove(0);
            }
            messages.add(message);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取消息列表的只读视图
     */
    public List<Message<?>> getMessagesReadOnly() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(messages);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空所有消息
     */
    public void clearMessages() {
        lock.writeLock().lock();
        try {
            messages.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        lock.readLock().lock();
        try {
            return messages.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return messages.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
}
