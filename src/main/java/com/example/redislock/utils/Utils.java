package com.example.redislock.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility class containing methods for common operations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    /**
     * Generates a UUID and converts it to Base64 format.
     *
     * @return the generated id in Base64 format
     */
    public static String uuidBase64() {
        UUID uuid = UUID.randomUUID();

        byte[] src = ByteBuffer.wrap(new byte[16])
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();

        return Base64.getEncoder().encodeToString(src).substring(0, 22);
    }
}
