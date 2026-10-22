package org.openathar.api.port.in;

import java.util.List;

import org.openathar.api.domain.CalculationMethod;

/** Use case: describe the calculation methods the API supports. */
public interface MethodsUseCase {

    List<CalculationMethod> list();
}
