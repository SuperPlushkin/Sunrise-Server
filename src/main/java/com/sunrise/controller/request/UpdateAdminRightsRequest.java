package com.sunrise.controller.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminRightsRequest {

    @NotNull(message = "isAdmin is required")
    private Boolean isAdmin;
}
