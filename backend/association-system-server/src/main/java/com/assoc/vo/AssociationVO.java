package com.assoc.vo;

import java.time.LocalDateTime;

/** 社团出参 */
public record AssociationVO(
        Long id,
        String name,
        String code,
        String category,
        Long leaderUserId,
        String leaderName,
        Integer status,
        String description,
        LocalDateTime createdAt) {
}
