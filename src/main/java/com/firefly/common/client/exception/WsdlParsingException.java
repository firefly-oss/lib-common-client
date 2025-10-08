/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.client.exception;

/**
 * Exception thrown when WSDL parsing or service initialization fails.
 * 
 * <p>This exception indicates problems with WSDL retrieval, parsing,
 * or service port initialization. It typically occurs during client
 * construction rather than during request execution.
 *
 * @author Firefly Software Solutions Inc
 * @since 2.0.0
 */
public class WsdlParsingException extends ServiceClientException {

    /**
     * Creates a new WSDL parsing exception.
     *
     * @param message the error message
     */
    public WsdlParsingException(String message) {
        super(message);
    }

    /**
     * Creates a new WSDL parsing exception with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public WsdlParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new WSDL parsing exception from a cause.
     *
     * @param cause the underlying cause
     */
    public WsdlParsingException(Throwable cause) {
        super("Failed to parse WSDL: " + cause.getMessage(), cause);
    }
}

