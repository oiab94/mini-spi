package com.oiab.minispi.transfer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountInfo(
        @JsonProperty("number") String number,
        @JsonProperty("name") @JsonAlias({"ownerName", "accountName"}) String name
) {
}
