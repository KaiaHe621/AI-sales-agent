package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.*;
import com.jichi.ragkb.repository.*;
import com.jichi.ragkb.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final AnswerFeedbackRepository feedbackRepository;
    private final ChatMessageRepository messageRepository;
    private final EvalDatasetRepository datasetRepository;

    /**
     * 提交用户反馈（点赞/点踩）。
     * 对于差评，自动提取问题加入候选评估数据集（人工审核后正式纳入）。
     */
    @Transactional
    public void submitFeedback(Long messageId, int feedback, String comment) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));

        // 同一用户对同一消息只保留一条反馈，重复提交则覆盖
        Long userId = UserContext.getUserId();
        AnswerFeedback fb = feedbackRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseGet(() -> {
                    AnswerFeedback newFb = new AnswerFeedback();
                    newFb.setMessageId(messageId);
                    newFb.setUserId(userId);
                    return newFb;
                });
        fb.setFeedback((short) feedback);
        fb.setComment(comment);
        feedbackRepository.save(fb);

        // 更新消息的 feedback 字段
        message.setFeedback((short) feedback);
        messageRepository.save(message);

        // 差评：把这个问题加入评估候选（人工审核后加入正式评估集）
        if (feedback == -1) {
            log.info("[Feedback] 差评记录，候选加入评估集：messageId={}", messageId);
            // 这里只记录日志，实际需要一个管理后台人工审核后才加入 eval_dataset
        }

        log.info("[Feedback] 反馈已记录：messageId={}，feedback={}，userId={}",
                messageId, feedback, UserContext.getUserId());
    }
}