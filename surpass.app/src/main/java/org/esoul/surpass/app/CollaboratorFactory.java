/*
   Copyright 2017-2024 e-soul.org
   All rights reserved.

   Redistribution and use in source and binary forms, with or without modification, are permitted
   provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of conditions
      and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions
      and the following disclaimer in the documentation and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.esoul.surpass.app;

import java.util.stream.Stream;

/**
 * Obtain collaborator services.
 * 
 * @author mgp
 *
 */
public interface CollaboratorFactory {

    /**
     * Obtains an existing instance of a service.
     * 
     * @param <T> The type of the service.
     * @param clazz The {@link Class} instance of the service.
     * 
     * @return The service instance.
     */
    <T> T obtainOne(Class<T> clazz) throws ServiceUnavailableException;

    /**
     * Obtains all existing service instances.
     * 
     * @param <T> The type of the service.
     * @param clazz The {@link Class} instance of the service.
     * @return A {@link Stream} of service instances.
     * @throws ServiceUnavailableException
     */
    <T> Stream<T> obtainAll(Class<T> clazz) throws ServiceUnavailableException;
}
