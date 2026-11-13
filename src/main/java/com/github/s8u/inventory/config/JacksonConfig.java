package com.github.s8u.inventory.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * 요청 JSON의 타입을 엄격하게 받는다
 *
 * Jackson은 기본으로 1.5를 정수 1로, "10"을 정수 10으로, 123을 문자열 "123"으로 바꿔 받는다.
 * 재고 수량이나 상품명이 요청과 다르게 저장되지 않도록, 타입이 다르면 바꾸지 않고 400으로 거절한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictTypeJsonMapperBuilderCustomizer() {
        return builder -> builder
                // 정수 필드에 1.5, "10"이 오면 거절
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // 문자열 필드에 123, 1.5, true가 오면 거절 (위 설정은 문자열 필드에는 적용되지 않는다)
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }

}
