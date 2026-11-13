package com.github.s8u.inventory.controller;

import com.github.s8u.inventory.config.JacksonConfig;
import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import com.github.s8u.inventory.usecase.ProductGetUseCase;
import com.github.s8u.inventory.usecase.ProductInboundUseCase;
import com.github.s8u.inventory.usecase.ProductOutboundUseCase;
import com.github.s8u.inventory.usecase.dto.command.ProductInboundCommand;
import com.github.s8u.inventory.usecase.dto.command.ProductOutboundCommand;
import com.github.s8u.inventory.usecase.dto.result.ProductGetResult;
import com.github.s8u.inventory.usecase.dto.result.ProductInboundResult;
import com.github.s8u.inventory.usecase.dto.result.ProductOutboundResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest는 @Configuration을 읽지 않아, 요청 JSON 타입 설정(JacksonConfig)을 직접 넣는다
@Import(JacksonConfig.class)
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductInboundUseCase productInboundUseCase;

    @MockitoBean
    private ProductOutboundUseCase productOutboundUseCase;

    @MockitoBean
    private ProductGetUseCase productGetUseCase;

    @Nested
    class InboundProduct {

        @Test
        void 입고하면_입고_뒤_재고를_응답한다() throws Exception {
            when(productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", "생수 500ml", 10)))
                    .thenReturn(new ProductInboundResult("WTR-500", "생수 500ml", 10));

            inbound("WTR-500", """
                    { "name": "생수 500ml", "quantity": 10 }
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("WTR-500"))
                    .andExpect(jsonPath("$.name").value("생수 500ml"))
                    .andExpect(jsonPath("$.quantity").value(10));
        }

        @Test
        void 수량이_1보다_작으면_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR-500", """
                    { "name": "생수 500ml", "quantity": 0 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.detail").value("수량은 1 이상이어야 합니다"));
            verify(productInboundUseCase, never()).inbound(any());
        }

        @Test
        void 수량이_없으면_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR-500", """
                    { "name": "생수 500ml" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.detail").value("수량은 필수입니다"));
        }

        @Test
        void 상품명이_100자를_넘으면_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR-500", """
                    { "name": "%s", "quantity": 10 }
                    """.formatted("가".repeat(101)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.detail").value("상품명은 100자 이하여야 합니다"));
        }

        @Test
        void SKU에_허용하지_않는_문자가_있으면_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR@500", """
                    { "name": "생수 500ml", "quantity": 10 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.detail").value("SKU는 영문, 숫자, -, _로 된 50자 이하여야 합니다"));
            verify(productInboundUseCase, never()).inbound(any());
        }

        @Test
        void SKU와_요청_본문이_함께_틀리면_오류를_모두_응답한다() throws Exception {
            inbound("WTR@500", """
                    { "name": "생수 500ml", "quantity": 0 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.detail").value("SKU는 영문, 숫자, -, _로 된 50자 이하여야 합니다, 수량은 1 이상이어야 합니다"));
        }

        @Test
        void JSON_형식이_잘못되면_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR-500", "{ \"quantity\": ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.detail").value("요청 본문을 읽을 수 없습니다. JSON 형식과 값의 타입을 확인해 주세요."));
        }

        @Test
        void 수량이_정수가_아니면_바꿔_받지_않고_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR-500", """
                    { "quantity": 1.5 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            inbound("WTR-500", """
                    { "quantity": "10" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            verify(productInboundUseCase, never()).inbound(any());
        }

        @Test
        void 상품명이_문자열이_아니면_바꿔_받지_않고_400_INVALID_REQUEST로_응답한다() throws Exception {
            inbound("WTR-500", """
                    { "name": 123, "quantity": 10 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            inbound("WTR-500", """
                    { "name": true, "quantity": 10 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            verify(productInboundUseCase, never()).inbound(any());
        }

        @Test
        void 요청_본문이_JSON이_아니면_415_INVALID_REQUEST로_응답한다() throws Exception {
            mockMvc.perform(post("/api/products/{sku}/inbound", "WTR-500")
                            .contentType("application/yaml")
                            .content("quantity: 10"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            verify(productInboundUseCase, never()).inbound(any());
        }

        @Test
        void 처음_입고하는_SKU인데_상품명이_없으면_400_PRODUCT_NAME_REQUIRED로_응답한다() throws Exception {
            when(productInboundUseCase.inbound(any())).thenThrow(new BusinessException(ErrorCode.PRODUCT_NAME_REQUIRED, "sku=WTR-500"));

            inbound("WTR-500", """
                    { "quantity": 10 }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PRODUCT_NAME_REQUIRED"));
        }

        @Test
        void 재고가_최대값을_넘으면_409_PRODUCT_STOCK_OVERFLOW로_응답한다() throws Exception {
            when(productInboundUseCase.inbound(any())).thenThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_OVERFLOW, "sku=WTR-500"));

            inbound("WTR-500", """
                    { "quantity": 1 }
                    """)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PRODUCT_STOCK_OVERFLOW"));
        }

        private ResultActions inbound(String sku, String body) throws Exception {
            return mockMvc.perform(post("/api/products/{sku}/inbound", sku)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

    }

    @Nested
    class OutboundProduct {

        @Test
        void 출고하면_출고_뒤_재고를_응답한다() throws Exception {
            when(productOutboundUseCase.outbound(new ProductOutboundCommand("WTR-500", 3)))
                    .thenReturn(new ProductOutboundResult("WTR-500", "생수 500ml", 7));

            outbound("WTR-500", 3)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("WTR-500"))
                    .andExpect(jsonPath("$.name").value("생수 500ml"))
                    .andExpect(jsonPath("$.quantity").value(7));
        }

        @Test
        void 재고가_부족하면_409_PRODUCT_STOCK_INSUFFICIENT로_응답한다() throws Exception {
            when(productOutboundUseCase.outbound(any())).thenThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT, "sku=WTR-500, currentQuantity=3, requestQuantity=5"));

            outbound("WTR-500", 5)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    // RFC 9457은 type이 없으면 about:blank로 본다. Spring 7은 기본으로 type을 넣지 않는다
                    .andExpect(jsonPath("$.type").doesNotExist())
                    .andExpect(jsonPath("$.title").value("Conflict"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("재고가 부족합니다."))
                    .andExpect(jsonPath("$.instance").value("/api/products/WTR-500/outbound"))
                    .andExpect(jsonPath("$.code").value("PRODUCT_STOCK_INSUFFICIENT"));
        }

        @Test
        void 없는_SKU면_404_PRODUCT_NOT_FOUND로_응답한다() throws Exception {
            when(productOutboundUseCase.outbound(any())).thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "sku=WTR-500"));

            outbound("WTR-500", 3)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        }

        private ResultActions outbound(String sku, int quantity) throws Exception {
            return mockMvc.perform(post("/api/products/{sku}/outbound", sku)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"quantity\": %d }".formatted(quantity)));
        }

    }

    @Nested
    class GetProductBySku {

        @Test
        void SKU로_상품과_현재_재고를_응답한다() throws Exception {
            when(productGetUseCase.get("WTR-500")).thenReturn(new ProductGetResult("WTR-500", "생수 500ml", 7));

            mockMvc.perform(get("/api/products/{sku}", "WTR-500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("WTR-500"))
                    .andExpect(jsonPath("$.name").value("생수 500ml"))
                    .andExpect(jsonPath("$.quantity").value(7));
        }

        @Test
        void 없는_SKU면_404_PRODUCT_NOT_FOUND로_응답한다() throws Exception {
            when(productGetUseCase.get("WTR-500")).thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "sku=WTR-500"));

            mockMvc.perform(get("/api/products/{sku}", "WTR-500"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        }

        @Test
        void 처리하지_못한_예외는_500_INTERNAL_ERROR로_응답하고_내부_메시지를_숨긴다() throws Exception {
            when(productGetUseCase.get("WTR-500")).thenThrow(new IllegalStateException("내부 정보가 담긴 메시지"));

            mockMvc.perform(get("/api/products/{sku}", "WTR-500"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.detail").value("서버 오류가 발생했습니다."));
        }

    }

    @Test
    void JSON이_아닌_형식으로_응답을_요청하면_406_INVALID_REQUEST로_응답한다() throws Exception {
        mockMvc.perform(get("/api/products/{sku}", "WTR-500").header(HttpHeaders.ACCEPT, "application/yaml"))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void Accept_헤더_형식이_잘못돼도_에러_본문을_응답한다() throws Exception {
        mockMvc.perform(get("/api/products/{sku}", "WTR-500").header(HttpHeaders.ACCEPT, "bad"))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 없는_경로는_404_INVALID_REQUEST로_응답한다() throws Exception {
        mockMvc.perform(get("/api/products/WTR-500/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

}
