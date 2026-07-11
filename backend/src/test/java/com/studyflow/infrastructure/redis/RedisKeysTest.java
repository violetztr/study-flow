package com.studyflow.infrastructure.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeysTest {
    @Test
    void buildsStableNamespacedKeysForRuruInfrastructure() {
        assertThat(RedisKeys.rate("login", "127.0.0.1")).isEqualTo("ruru:rate:login:127.0.0.1");
        assertThat(RedisKeys.viewDedupe(12L, "user:7")).isEqualTo("ruru:view:dedupe:12:user_7");
        assertThat(RedisKeys.feed("VIDEO", 0)).isEqualTo("ruru:feed:video:0");
        assertThat(RedisKeys.postDetail(9L)).isEqualTo("ruru:post:detail:9");
        assertThat(RedisKeys.postCounter(9L)).isEqualTo("ruru:counter:post:9");
    }

    @Test
    void normalizesUnsafeSegmentsWithoutRemovingMeaningfulText() {
        assertThat(RedisKeys.rate(" Upload Video ", " user: 99 "))
                .isEqualTo("ruru:rate:upload_video:user_99");
        assertThat(RedisKeys.rate("", "   ")).isEqualTo("ruru:rate:unknown:unknown");
    }
}
