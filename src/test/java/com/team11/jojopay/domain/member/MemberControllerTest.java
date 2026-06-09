package com.team11.jojopay.domain.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.member.controller.MemberController;
import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class MemberControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MemberController memberController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return 1L; // long memberId 파라미터에 1L 다이렉트 바인딩 강제화
                    }
                })
                .build();
    }

    @Test
    @DisplayName("[GET] 내 기본 정보 조회 API 성공")
    void getMyInfo_Api_Success() throws Exception {
        // given: 리플렉션 기술로 private 생성자 우회 조립
        java.lang.reflect.Constructor<MemberResponse> constructor = MemberResponse.class.getDeclaredConstructor(
                String.class, String.class, String.class,
                Class.forName("com.team11.jojopay.domain.member.enums.MembershipGrade"),
                Long.class, Long.class, java.time.LocalDateTime.class
        );
        constructor.setAccessible(true);
        MemberResponse mockResponse = constructor.newInstance("강감찬", "kang@test.com", "010-1111-2222", null, 0L, 0L, null);

        given(memberService.getMyInfo(1L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/members/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 정보 조회 성공"))
                .andExpect(jsonPath("$.data.name").value("강감찬"));

        verify(memberService, times(1)).getMyInfo(1L);
    }

    @Test
    @DisplayName("[GET] 내 멤버십 정보 조회 API 성공")
    void getMyMembership_Api_Success() throws Exception {
        // given
        java.lang.reflect.Constructor<MembershipResponse> constructor = MembershipResponse.class.getDeclaredConstructor(
                Class.forName("com.team11.jojopay.domain.member.enums.MembershipGrade"), Long.class, int.class, Long.class
        );
        constructor.setAccessible(true);
        MembershipResponse mockResponse = constructor.newInstance(null, 50000L, 5, 50000L);

        given(memberService.getMyMembership(1L)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/membership"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("멤버십 정보 조회 성공"))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(50000));

        verify(memberService, times(1)).getMyMembership(1L);
    }
}
