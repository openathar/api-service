package org.openathar.api.port.in;

import java.util.Optional;

import org.openathar.api.domain.ApiKey;
import org.openathar.api.domain.ApiKeyUsage;

/** Use case: self-serve API key issuance and usage lookup (no email, no account). */
public interface ApiKeyUseCase {

    ApiKey issue(String label);

    Optional<ApiKeyUsage> usage(String value);
}
