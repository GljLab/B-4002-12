package com.label4002.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KeywordCloudDTO(
        Long id,
        String name,
        Integer usageCount,
        Double heatScore,
        LocalDateTime lastUsedAt
) {
    public static List<KeywordCloudDTO> computeHeat(List<KeywordCloudDTO> keywords) {
        if (keywords.isEmpty()) return keywords;
        double maxUsageRaw = keywords.stream()
                .mapToDouble(KeywordCloudDTO::usageCount)
                .max()
                .orElse(1);
        final double maxUsage = maxUsageRaw == 0 ? 1 : maxUsageRaw;
        final LocalDateTime now = LocalDateTime.now();
        return keywords.stream().map(k -> {
            long daysSinceLastUse = k.lastUsedAt != null
                    ? java.time.Duration.between(k.lastUsedAt, now).toDays()
                    : 365;
            double timeDecay = Math.exp(-daysSinceLastUse / 90.0);
            double heat = (k.usageCount / maxUsage) * timeDecay;
            return new KeywordCloudDTO(k.id, k.name, k.usageCount, Math.round(heat * 1000.0) / 1000.0, k.lastUsedAt);
        }).toList();
    }
}
