package com.bgsoftware.wildchests.hooks;

import javax.annotation.Nullable;
import java.util.Optional;

public enum PricesProviderType {

    SHOPGUIPLUS,
    CMI,
    WILDCHESTS,
    AUTO;

    public static Optional<PricesProviderType> fromName(@Nullable String name) {
        if (name != null) {
            try {
                return Optional.of(PricesProviderType.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return Optional.empty();
    }

}
