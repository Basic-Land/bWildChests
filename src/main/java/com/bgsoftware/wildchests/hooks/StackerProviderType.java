package com.bgsoftware.wildchests.hooks;

import javax.annotation.Nullable;
import java.util.Optional;

public enum StackerProviderType {

    WILDSTACKER,
    AUTO;

    public static Optional<StackerProviderType> fromName(@Nullable String name) {
        if (name != null) {
            try {
                return Optional.of(StackerProviderType.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return Optional.empty();
    }

}
