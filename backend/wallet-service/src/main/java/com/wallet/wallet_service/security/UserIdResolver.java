package com.wallet.wallet_service.security;

import com.wallet.wallet_service.exception.UnauthorizedAccessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class UserIdResolver {

    /**
     * Returns the userId that was stamped onto the request by the JWT filter.
     * Throws 403 if the header the client sent doesn't match the token's subject.
     */
    public String resolve(HttpServletRequest request, String claimedUserId) {
        String authenticatedUserId =
                (String) request.getAttribute("authenticatedUserId");

        if (authenticatedUserId == null) {
            throw new UnauthorizedAccessException("No authenticated user on request");
        }

        if (!authenticatedUserId.equals(claimedUserId)) {
            throw new UnauthorizedAccessException(
                    "X-User-Id header does not match authenticated token");
        }

        return authenticatedUserId;
    }
}