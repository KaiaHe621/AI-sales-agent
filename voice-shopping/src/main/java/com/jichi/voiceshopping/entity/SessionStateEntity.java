package com.jichi.voiceshopping.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "session_state")
public class SessionStateEntity {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /** INTENT / CLARIFY / RECOMMEND / ORDER_CONFIRM  / ENDED */
    @Column(nullable = false, length = 32)
    private String phase;

    @Column(name = "current_intent", length = 64)
    private String currentIntent;

    /** 槽位：品类、预算、风格、场景…… Agent 边聊边填 */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> slots;

    /** 当前还在等用户回答的那个澄清问题，没有则为 null */
    @Column(name = "pending_ask", columnDefinition = "text")
    private String pendingAsk;

    /** 上一轮推给用户的商品 ID 列表，用于"再看看第二个"这类指代 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "last_recommendations", columnDefinition = "bigint[]")
    private List<Long> lastRecommendations;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}