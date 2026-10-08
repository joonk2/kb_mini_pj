package kb_bridge.external.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DartCompanyResponse(

        String status,

        String message,

        @JsonProperty("corp_code")
        String corpCode,

        @JsonProperty("corp_name")
        String corpName,

        @JsonProperty("corp_name_eng")
        String corpNameEng,

        @JsonProperty("stock_name")
        String stockName,

        @JsonProperty("stock_code")
        String stockCode,

        @JsonProperty("ceo_nm")
        String ceoName,

        @JsonProperty("corp_cls")
        String corpClass,

        @JsonProperty("jurir_no")
        String corporationRegistrationNumber,

        @JsonProperty("bizr_no")
        String businessRegistrationNumber,

        String adres,

        @JsonProperty("hm_url")
        String homepageUrl,

        @JsonProperty("ir_url")
        String irUrl,

        @JsonProperty("phn_no")
        String phoneNumber,

        @JsonProperty("fax_no")
        String faxNumber,

        @JsonProperty("induty_code")
        String industryCode,

        @JsonProperty("est_dt")
        String establishedDate,

        @JsonProperty("acc_mt")
        String fiscalMonth

) {}