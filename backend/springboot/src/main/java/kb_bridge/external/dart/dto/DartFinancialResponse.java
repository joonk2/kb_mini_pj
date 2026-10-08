package kb_bridge.external.dart.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DartFinancialResponse(
        String status,
        String message,
        List<Item> list
) {
    public record Item(
            @JsonProperty("account_nm") String accountName,
            @JsonProperty("thstrm_amount") String currentAmount,
            @JsonProperty("bsns_year") String businessYear
    ) {
    }
}
