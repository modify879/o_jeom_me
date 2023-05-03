package com.ojeomme.dto.response.storereviewstatistics;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class TodayStoreRankingResponseDto {

    private final List<StoreResponseDto> stores;

    public TodayStoreRankingResponseDto(List<StoreResponseDto> stores) {
        this.stores = stores;
    }

    @NoArgsConstructor
    @Getter
    public static class StoreResponseDto {

        private Long storeId;
        private String storeName;
        private String regionName;
        private String image;

        @Builder
        public StoreResponseDto(Long storeId, String storeName, String regionName, String image) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.regionName = regionName;
            this.image = image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}
