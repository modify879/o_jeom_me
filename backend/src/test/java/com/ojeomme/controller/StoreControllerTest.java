package com.ojeomme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojeomme.common.maps.client.KakaoKeywordClient;
import com.ojeomme.controller.support.AcceptanceTest;
import com.ojeomme.domain.category.Category;
import com.ojeomme.domain.category.repository.CategoryRepository;
import com.ojeomme.domain.regioncode.repository.RegionCodeRepository;
import com.ojeomme.domain.review.Review;
import com.ojeomme.domain.review.repository.ReviewRepository;
import com.ojeomme.domain.store.Store;
import com.ojeomme.domain.store.repository.StoreRepository;
import com.ojeomme.dto.request.store.SearchPlaceListRequestDto;
import com.ojeomme.exception.ApiErrorCode;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StoreControllerTest extends AcceptanceTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private RegionCodeRepository regionCodeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @SpyBean
    private KakaoKeywordClient kakaoKeywordClient;

    private MockWebServer mockWebServer;

    @Nested
    class getStoreReviews {

        @Test
        void 매장과_프리뷰_이미지를_가져온다() {
            // given

            // when
            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .auth().oauth2(accessToken)
                    .when().get("/api/store/{storeId}", store.getId())
                    .then().log().all()
                    .extract();

            JsonPath jsonPath = response.jsonPath();

            // then
            assertThat(jsonPath.getLong("store.storeId")).isEqualTo(store.getId());
            assertThat(jsonPath.getLong("store.placeId")).isEqualTo(store.getKakaoPlaceId());
            assertThat(jsonPath.getString("store.storeName")).isEqualTo(store.getStoreName());
            assertThat(jsonPath.getString("store.categoryName")).isEqualTo(store.getCategory().getCategoryName());
            assertThat(jsonPath.getString("store.addressName")).isEqualTo(store.getAddressName());
            assertThat(jsonPath.getString("store.roadAddressName")).isEqualTo(store.getRoadAddressName());
            assertThat(jsonPath.getString("store.x")).isEqualTo(store.getX());
            assertThat(jsonPath.getString("store.y")).isEqualTo(store.getY());
            assertThat(jsonPath.getInt("store.likeCnt")).isEqualTo(store.getLikeCnt());

            assertThat(jsonPath.getList("previewImages").size()).isEqualTo(store.getReviews().stream().map(Review::getReviewImages).count());
        }

        @Test
        void 매장이_없으면_StoreNotFoundException를_발생한다() {
            // given

            // when
            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .auth().oauth2(accessToken)
                    .when().get("/api/store/{storeId}", -1L)
                    .then().log().all()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(ApiErrorCode.STORE_NOT_FOUND.getHttpStatus().value());
            assertThat(response.asString()).isEqualTo(ApiErrorCode.STORE_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class searchPlaceList {

        private final Map<String, Object> requestDto = new ObjectMapper().convertValue(SearchPlaceListRequestDto.builder()
                .query("스시코우지")
                .x("127.03662909986537")
                .y("37.52186058560857")
                .page(1)
                .build(), Map.class);

        @Test
        void 매장_목록을_가져온다() throws Exception {
            // given
            startMockWebServer();
            setMapsClinet();

            Category category = categoryRepository.save(Category.builder()
                    .categoryName("음식점")
                    .categoryDepth(1)
                    .build());

            Store store = Store.builder()
                    .kakaoPlaceId(23829251L)
                    .category(category)
                    .regionCode(regionCodeRepository.findById("1168010800").orElseThrow())
                    .storeName("스시코우지")
                    .addressName("서울 강남구 논현동 92")
                    .roadAddressName("서울 강남구 도산대로 318")
                    .x("127.03662909986537")
                    .y("37.52186058560857")
                    .likeCnt(5)
                    .build();
            store.writeReview(Review.builder()
                    .user(user)
                    .store(store)
                    .starScore(5)
                    .content("리뷰")
                    .revisitYn(false)
                    .build());
            store = storeRepository.save(store);

            // when
            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .auth().oauth2(accessToken)
                    .params(requestDto)
                    .when().get("/api/store/searchPlaceList")
                    .then().log().all()
                    .extract();

            JsonPath jsonPath = response.jsonPath();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            assertThat(jsonPath.getInt("meta.totalCount")).isEqualTo(1);
            assertThat(jsonPath.getInt("meta.pageableCount")).isEqualTo(1);
            assertThat(jsonPath.getBoolean("meta.isEnd")).isFalse();

            assertThat(jsonPath.getLong("places[0].storeId")).isEqualTo(store.getId());
            assertThat(jsonPath.getLong("places[0].placeId")).isEqualTo(23829251L);
            assertThat(jsonPath.getString("places[0].placeName")).isEqualTo("스시코우지");
            assertThat(jsonPath.getString("places[0].categoryName")).isEqualTo("초밥,롤");
            assertThat(jsonPath.getString("places[0].phone")).isEqualTo("02-541-6200");
            assertThat(jsonPath.getString("places[0].addressName")).isEqualTo("서울 강남구 논현동 92");
            assertThat(jsonPath.getString("places[0].roadAddressName")).isEqualTo("서울 강남구 도산대로 318");
            assertThat(jsonPath.getString("places[0].x")).isEqualTo("127.03662909986537");
            assertThat(jsonPath.getString("places[0].y")).isEqualTo("37.52186058560857");
            assertThat(jsonPath.getInt("places[0].likeCnt")).isEqualTo(5);
            assertThat(jsonPath.getInt("places[0].reviewCnt")).isEqualTo(1);

            closeMockWebServer();
        }

        @Test
        void 매장_목록을_가져온다_없는_매장() throws Exception {
            // given
            startMockWebServer();

            setMapsClinet();

            // when
            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .auth().oauth2(accessToken)
                    .params(requestDto)
                    .when().get("/api/store/searchPlaceList")
                    .then().log().all()
                    .extract();

            JsonPath jsonPath = response.jsonPath();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            assertThat(jsonPath.getInt("meta.totalCount")).isEqualTo(1);
            assertThat(jsonPath.getInt("meta.pageableCount")).isEqualTo(1);
            assertThat(jsonPath.getBoolean("meta.isEnd")).isFalse();

            assertThat(jsonPath.getString("places[0].storeId")).isNull();
            assertThat(jsonPath.getLong("places[0].placeId")).isEqualTo(23829251L);
            assertThat(jsonPath.getString("places[0].placeName")).isEqualTo("스시코우지");
            assertThat(jsonPath.getString("places[0].categoryName")).isEqualTo("초밥,롤");
            assertThat(jsonPath.getString("places[0].phone")).isEqualTo("02-541-6200");
            assertThat(jsonPath.getString("places[0].addressName")).isEqualTo("서울 강남구 논현동 92");
            assertThat(jsonPath.getString("places[0].roadAddressName")).isEqualTo("서울 강남구 도산대로 318");
            assertThat(jsonPath.getString("places[0].x")).isEqualTo("127.03662909986537");
            assertThat(jsonPath.getString("places[0].y")).isEqualTo("37.52186058560857");
            assertThat(jsonPath.getInt("places[0].likeCnt")).isEqualTo(0);
            assertThat(jsonPath.getInt("places[0].reviewCnt")).isEqualTo(0);

            closeMockWebServer();
        }
    }

    private void startMockWebServer() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    private void closeMockWebServer() throws Exception {
        mockWebServer.close();
    }

    private void setMapsClinet() {
        String uri = String.format("http://%s:%s", mockWebServer.getHostName(), mockWebServer.getPort());
        ReflectionTestUtils.setField(kakaoKeywordClient, "mapsClient", WebClient.create().mutate().baseUrl(uri).defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build());

        String response = "{\n" +
                "  \"documents\": [\n" +
                "    {\n" +
                "      \"address_name\": \"서울 강남구 논현동 92\",\n" +
                "      \"category_group_code\": \"FD6\",\n" +
                "      \"category_group_name\": \"음식점\",\n" +
                "      \"category_name\": \"음식점 > 일식 > 초밥,롤\",\n" +
                "      \"distance\": \"\",\n" +
                "      \"id\": \"23829251\",\n" +
                "      \"phone\": \"02-541-6200\",\n" +
                "      \"place_name\": \"스시코우지\",\n" +
                "      \"place_url\": \"http://place.map.kakao.com/23829251\",\n" +
                "      \"road_address_name\": \"서울 강남구 도산대로 318\",\n" +
                "      \"x\": \"127.03662909986537\",\n" +
                "      \"y\": \"37.52186058560857\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"meta\": {\n" +
                "    \"is_end\": true,\n" +
                "    \"pageable_count\": 1,\n" +
                "    \"same_name\": {\n" +
                "      \"keyword\": \"스시코우지\",\n" +
                "      \"region\": [],\n" +
                "      \"selected_region\": \"\"\n" +
                "    },\n" +
                "    \"total_count\": 1\n" +
                "  }\n" +
                "}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(response)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    }
}